# 엔티티 모델 검토 — 이슈 정리

> 대상 파일: `entity-model_ko.md`  
> 작성 기준: 추가기능(능력치 계산기) MVP 범위 확정 내용 반영

---

## 확정된 전제 사항

| 항목 | 확정 내용 |
|------|-----------|
| 추가기능 MVP statType 범위 | `ELEMENT_VALUE`, `ELEMENT_PIERCE`, `RESIST_PIERCE` 3종만 |
| 공격력 / 치명타 / 스텟 | MVP 범위 밖 — 추후 확장 |
| 장비당 보석 슬롯 | 최대 1개 |
| `MaterialPriceHistory` 용도 | 가성비 계산기와 무관 (크롤링 기반 재료 시세 전용) |
| `Gem` 주술 가능 단계 | **`강화됨` 단계에서만** 가능. `빛나는` 등급에는 주술 불가 (확정) |

---

## 이슈 목록

### 🔴 이슈 1 — `Gem.gem_grade` 구조 재검토 필요

**현재 설계:**
```
gem_grade: 기본 | 세공됨 | 강화됨 | 빛나는 | 주술됨
```

**문제점:**  
`gem_grade="주술됨"`은 독립 등급처럼 보이지만, 실제로는 `강화됨` 등급에 주술이 부가된 상태다.  
`빛나는` 등급에는 주술이 붙지 않으므로, 주술 여부는 별도 필드로 분리하는 게 맞다.

**확정된 전제:**  
- 주술 부착 가능 단계: **`강화됨` 단계에서만** 가능  
- `빛나는` 등급에는 주술 불가 (확정)  
- 표기 예: `<주술명> 강화됨`

**권장 수정 방향:**  
`gem_grade`에서 `주술됨`을 제거하고, 주술 여부는 `ritual_id`의 존재 여부로 표현.  
`강화됨` 등급일 때만 `ritual_id`가 non-null일 수 있도록 DB 체크 제약 추가.

```
gem_grade: 기본 | 세공됨 | 강화됨 | 빛나는
ritual_id (FK → Ritual, nullable): null=주술 없음, non-null=주술 있음 (강화됨만 허용)
```

DB 체크 제약:
```sql
CHECK (ritual_id IS NULL OR gem_grade = '강화됨')
```

`UNIQUE(name, gem_grade, ritual_id)` 제약 유지.

---

### 🔴 이슈 2 — `statKey` 문자열 방식 위험성

**현재 설계 (13.5절):**
```
ITEM:42
ITEM:42:RITUAL:XX
SET:3:RITUAL_COUNT:5:MARK:XX
```

**문제점:**
1. DB 인덱스 효율 저하 — 문자열 전체 매칭 또는 LIKE 검색 필요
2. 파싱 로직이 애플리케이션 코드에 분산됨
3. 형식이 조금만 달라져도 (공백, 대소문자) 집계 불일치 발생
4. 2단계, 3단계 확장 시 기존 데이터 마이그레이션 필요

**권장 수정 방향:**  
MVP(1단계)는 `statKey = ITEM:{itemId}` 단순 문자열로 유지하되,  
2단계 이상 확장 시 복합 컬럼 방식으로 전환 검토.

```
stat_item_id (FK → Item)
stat_ritual_mark (nullable)
stat_set_id (FK → EquipmentSet, nullable)
stat_ritual_count (nullable)
```

**MVP에서는 현행 유지 가능. 단, 2단계 전환 시점에 반드시 재설계 필요하다는 주석을 코드/문서에 명시할 것.**

---

### 🔴 이슈 3 — `BundleEquipmentGem` 슬롯 제한 미정의

**현재 설계:**  
`bundle_line_id (FK → BundleLine)` + `gem_id (FK → Gem)` — 슬롯 수 제한 없음

**확정 내용:**  
장비 1개당 보석 최대 1개

**문제점:**  
현재 구조는 동일 `bundle_line_id`에 여러 `BundleEquipmentGem` 행 삽입이 가능.  
DB 레벨 제한이 없으면 애플리케이션 로직에만 의존하게 됨.

**권장 수정 (2가지 중 택1):**

옵션 A — UNIQUE 제약 추가:
```sql
UNIQUE(bundle_line_id)
```

옵션 B — `BundleEquipmentDetail`에 `gem_id` 컬럼 합치기:
```
BundleEquipmentDetail.gem_id (FK → Gem, nullable)
```
별도 테이블 불필요, 1개 제한이 구조적으로 보장됨. **옵션 B 권장.**

---

### 🟡 이슈 4 — `MaterialPriceHistory`와 `TradeStatMonthly` 역할 혼동 가능성

**확정 내용:**  
`MaterialPriceHistory`는 가성비 계산기와 무관하며, **크롤링 기반 재료 시세 전용 테이블**이다.

**문제점:**  
문서 12.2절에서 `TradeStatMonthly`를 가성비 비교의 가격 기준으로 설명하면서,  
14.3절에 `MaterialPriceHistory`가 "가성비 계산기 기본값으로 사용된다"고 기술되어 있어 혼란 발생.

**권장 수정:**  
14.3절 설명에서 "가성비 계산기 기본값" 문구를 **"재료 시세 조회 전용"** 으로 수정.

| 테이블 | 데이터 출처 | 용도 |
|--------|------------|------|
| `TradeStatMonthly` | 자체 거래 확정 데이터 | 가성비 계산기 가격 기준 |
| `MaterialPriceHistory` | 외부 크롤링 (geota.co.kr) | 재료 시세 참고 조회만 |

---

### 🟡 이슈 5 — `ItemStat.statType` MVP 범위 미명시

**확정 내용:**  
MVP statType = `ELEMENT_VALUE`, `ELEMENT_PIERCE`, `RESIST_PIERCE` 3종.  
공격력, 치명타율, 주 스텟(힘/민첩/생명/지능)은 MVP 범위 밖.

**문제점:**  
현재 문서에 MVP 이후 확장 항목임이 명시되어 있지 않음.  
이후 개발자나 Claude Code가 잘못된 범위로 구현할 가능성 있음.

**권장 수정:**  
`ItemStat` 엔티티 설명에 아래 주석 추가:

```
// MVP statType: ELEMENT_VALUE | ELEMENT_PIERCE | RESIST_PIERCE
// 확장 예정 (MVP 이후): ATTACK_POWER | CRIT_RATE | MAIN_STAT_STR | MAIN_STAT_DEX 등
```

---

### 🟢 이슈 6 — `TradeConfirmed.cancelled` boolean 한계

**현재 설계:**
```
cancelled (boolean, 기본값 false)
cancelledAt (nullable)
```

**문제점:**  
"누가 취소했는지" (구매자 / 판매자 / 관리자) 구분 불가.  
분쟁 처리나 패널티 정책 도입 시 추적 불가.

**권장 수정 방향:**  
MVP에서는 boolean 유지 가능. 단, 향후 전환 예정임을 주석으로 명시.

```
// TODO: cancelled(boolean) → cancelledBy(BUYER | SELLER | ADMIN) Enum으로 확장 예정
```

---

## 수정 우선순위 요약

| 우선순위 | 이슈 | 조치 유형 |
|----------|------|-----------|
| 🔴 즉시 | `Gem.gem_grade`에서 `주술됨` 제거 + CHECK 제약 추가 | 구조 수정 |
| 🔴 즉시 | `BundleEquipmentGem` → `BundleEquipmentDetail.gem_id` 통합 (옵션 B) | 구조 수정 |
| 🔴 확인 | `statKey` 문자열 방식 — 2단계 전환 전 재설계 주석 추가 | 문서 주석 |
| 🟡 단기 | `MaterialPriceHistory` 설명 문구 수정 | 문서 수정 |
| 🟡 단기 | `ItemStat.statType` MVP 범위 주석 추가 | 문서 수정 |
| 🟢 장기 | `TradeConfirmed.cancelled` → `cancelledBy` Enum 전환 | 향후 확장 |
