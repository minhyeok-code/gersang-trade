# 크롤러 설계 문서

> 거상 거래 플랫폼 — 거상짱 크롤링 설계 확정본  
> 작성일: 2026-04-28

---

## 목차
1. [크롤링 대상 및 실행 순서](#1-크롤링-대상-및-실행-순서)
2. [크롤러별 상세 설계](#2-크롤러별-상세-설계)
3. [파싱 전략](#3-파싱-전략)
4. [멱등성(Upsert) 전략](#4-멱등성upsert-전략)
5. [오류 처리 전략](#5-오류-처리-전략)
6. [이미지 처리](#6-이미지-처리)
7. [오픈 이슈](#7-오픈-이슈)

---

## 1. 크롤링 대상 및 실행 순서

### 1.1 데이터 소스

```
gerniverse.app  → Cloudflare 차단 ❌
geota.co.kr     → x-signature HMAC 차단 ❌
거상짱          → 전통 SSR, 시맨틱 HTML ✅
```

### 1.2 크롤러 분류 및 실행 순서

크롤러는 4개로 분류되며, 의존성에 따라 순서대로 실행해야 한다.  
각 크롤러는 별도 명령어로 독립 실행된다.

```
1단계: 아이템 크롤러   ← 반드시 먼저 실행
2단계: 세트 크롤러    ← 아이템 크롤러 완료 후 실행
3단계: 주술 크롤러    ← 아이템 + 세트 크롤러 완료 후 실행
4단계: 용병 크롤러    ← 독립 실행 가능 (아이템 의존 없음)
```

**실행 순서가 중요한 이유:**
- 세트 크롤러는 `EquipmentItem`이 DB에 있어야 `EquipmentSetPiece` 연결 가능
- 주술 크롤러는 `EquipmentSet`이 DB에 있어야 `RitualApplicability` 연결 가능
- 순서를 어기면 FK 조회 실패 → 스킵 로그 발생

### 1.3 크롤러별 출력 엔티티

| 크롤러 | 출력 엔티티 |
|---|---|
| 아이템 크롤러 | `EquipmentItem`, `ItemStat` |
| 재료 크롤러 | `MaterialItem` |
| 세트 크롤러 | `EquipmentSet`, `EquipmentSetPiece`, `EquipmentSetEffect` |
| 주술 크롤러 | `Ritual`, `RitualApplicability`, `RitualSetEffect` |
| 용병 크롤러 | `Mercenary`, `MercenaryStat`, `MercenarySkill` 등 |

> **아이템 크롤러와 재료 크롤러는 분리 운영한다.**  
> 현재 아이템 크롤러가 모든 아이템을 MATERIAL로 저장하는 문제가 있어 수정 중.  
> 재료 크롤러는 별도로 구현 예정.

---

## 2. 크롤러별 상세 설계

### 2.1 아이템 크롤러

```
입력: 장비 아이템 페이지 (갑옷/투구/장갑/요대/신발 등 슬롯별 분류 페이지)
출력:
  EquipmentItem  (아이템명, 슬롯, 이미지URL, 레벨 등)
  ItemStat       (스탯 종류, 수치, stat_unit)
```

**주의사항:**
- 슬롯별로 페이지가 분리되어 있음 (갑옷 페이지, 투구 페이지 등)
- 이미지 URL 저장 (S3 업로드는 후순위 — 6번 참고)
- stat_unit 파싱 규칙 적용 필수 (3번 참고)

---

### 2.2 세트 크롤러

```
입력: 세트 목록 페이지 (카테고리 1페이지에 전체 세트 포함)
출력:
  EquipmentSet       (세트명, totalPieces, isTradeable=true)
  EquipmentSetPiece  (슬롯별 피스 — DB에서 EquipmentItem 조회 후 연결)
  EquipmentSetEffect (2종/3종/6종 세트 효과)
```

**피스 연결 방법:**
```
세트 페이지의 피스명 = 슬롯명 (예: "투구", "갑옷")
DB 조회: setName="지국천왕" AND slot=HELMET
→ EquipmentItem 찾아서 EquipmentSetPiece 연결
→ 못 찾으면 스킵 + 로그 (5번 참고)
```

**각성/비각성 세트:**
- 지국천왕 세트와 각성지국천왕 세트는 **완전히 별개의 EquipmentSet**으로 저장
- 연관관계 없음, parent_set_id 불필요

---

### 2.3 주술 크롤러

```
입력: 주술 목록 페이지 (카테고리 1페이지에 전체 주술 포함)
출력:
  Ritual               (주술명, 레벨, 기본 능력치)
  RitualApplicability  (적용 가능 세트 — DB에서 세트명 조회 후 연결)
  RitualSetEffect      (3세트/5세트 효과 + outcome)
```

**RitualApplicability 파싱 규칙:**
```
"보석추가 아이템: 파멸자, 황제, 지국천왕, 항삼세명왕 장갑, 요대, 신발"

토큰을 하나씩 읽으면서:
  DB 세트명과 매칭 → 해당 세트 전슬롯 RitualApplicability 저장
  슬롯명(장갑/요대/신발 등) → 무시

이유: 거상짱 데이터가 실제 게임과 다름.
  사이트 표기는 특정 슬롯만 가능한 것처럼 되어 있지만
  실제 게임에서는 세트 전슬롯에 주술 가능.
```

**북두칠성(대성공) 처리:**
```
북두칠성 행은 별도 Ritual이 아님.
바로 위 주술(천추)의 outcome=GREAT_SUCCESS로 저장.

천추 행     → Ritual 저장 + RitualSetEffect(outcome=SUCCESS)
북두칠성 행 → 별도 Ritual 없음, 천추 ritual_id + outcome=GREAT_SUCCESS로 저장
```

---

## 3. 파싱 전략

### 3.1 스탯 파싱 규칙

크롤링 시 능력치 문자열에서 StatType과 StatUnit을 결정한다.

```
"힘300"      → STRENGTH,          300, FLAT, element=NONE
"민첩300"    → DEXTERITY,         300, FLAT, element=NONE
"생명300"    → VITALITY,          300, FLAT, element=NONE
"지력300"    → INTELLECT,         300, FLAT, element=NONE
"방어300"    → DEFENSE,           300, FLAT, element=NONE
"타저30%"    → HITTING_RESISTANCE, 30, PERCENT, element=NONE
"마저30%"    → MAGIC_RESISTANCE,   30, PERCENT, element=NONE
"속성값+5"   → ELEMENT_VALUE,       5, FLAT, element=ADAPTIVE  ← 용병 속성 따라감
"화속성+2"   → ELEMENT_VALUE,       2, FLAT, element=FIRE
"수속성+2"   → ELEMENT_VALUE,       2, FLAT, element=WATER
"풍속성+2"   → ELEMENT_VALUE,       2, FLAT, element=WIND
"뇌속성+3"   → ELEMENT_VALUE,       3, FLAT, element=THUNDER
"땅속성+3"   → ELEMENT_VALUE,       3, FLAT, element=EARTH     ← 땅속성 고정
"데미지+2%"  → DAMAGE_PERCENT,      2, PERCENT, element=NONE
"이속+1"     → FIELD_MOVE_SPEED,    1, FLAT, element=NONE
```

### 3.2 세트 효과 멀티라인 파싱

```
"6종\n힘400\n속성값5\n(땅속성3)"

파싱 결과 3행:
  requiredPieces=6, STRENGTH,     400, FLAT, element=NONE
  requiredPieces=6, ELEMENT_VALUE,  5, FLAT, element=ADAPTIVE ← 용병 속성 따라감
  requiredPieces=6, ELEMENT_VALUE,  3, FLAT, element=EARTH    ← 땅속성 고정

파싱 규칙:
  "속성값+n"  → element=ADAPTIVE (용병 속성에 따라 해당 속성값 증가)
  "땅속성+n"  → element=EARTH    (땅속성 고정 증가)
  "화속성+n"  → element=FIRE
  "뇌속성+n"  → element=THUNDER
  "풍속성+n"  → element=WIND
  "수속성+n"  → element=WATER
  element=null은 ELEMENT_VALUE에서 사용하지 않음
```

### 3.3 RitualSetEffect outcome 구분

```
일반 주술 행 (천추, 천선 등)
  → outcome = SUCCESS

북두칠성 행 (대성공)
  → 바로 위 주술의 ritual_id 참조
  → outcome = GREAT_SUCCESS

UI 표시:
  outcome=SUCCESS       → "천추"
  outcome=GREAT_SUCCESS → "천추<북두칠성>"
```

---

## 4. 멱등성(Upsert) 전략

크롤러를 여러 번 실행해도 중복 저장되지 않도록 upsert 전략을 적용한다.

### 4.1 엔티티별 기준 키

| 엔티티 | Upsert 기준 키 | 비고 |
|---|---|---|
| `EquipmentItem` | `name` + `slot` | 세트명+슬롯 조합 |
| `ItemStat` | `item_id` + `stat_type` + `element` | |
| `EquipmentSet` | `name` | 세트명 유일 |
| `EquipmentSetPiece` | `set_id` + `slot` | UNIQUE 제약 기존 존재 |
| `EquipmentSetEffect` | `set_id` + `required_pieces` + `stat_type` + `element` | UNIQUE 제약 기존 존재 |
| `Ritual` | `name` | 주술명 유일 |
| `RitualApplicability` | `ritual_id` + `equipment_item_id` | UNIQUE 제약 기존 존재 |
| `RitualSetEffect` | `ritual_id` + `outcome` + `set_id` + `required_ritual_pieces` + `stat_type` | outcome 포함 필수 |

### 4.2 Upsert 처리 방식

```java
// 예시: EquipmentSet upsert
equipmentSetRepository.findByName(name)
    .ifPresentOrElse(
        existing -> {
            // 있으면 업데이트 (totalPieces 등 변경 가능 필드만)
            existing.update(totalPieces);
        },
        () -> {
            // 없으면 새로 저장
            equipmentSetRepository.save(new EquipmentSet(name, totalPieces));
        }
    );
```

### 4.3 주의사항

```
isTradeable은 upsert 시 덮어쓰지 않는다.
→ 관리자가 수동으로 false로 바꾼 값이 크롤링으로 true로 돌아오면 안 됨.
→ 최초 저장 시만 true, 이후 크롤링에서는 해당 필드 업데이트 제외.
```

---

## 5. 오류 처리 전략

### 5.1 피스 미매칭 처리

세트 크롤러에서 `setName + slot`으로 `EquipmentItem`을 찾지 못한 경우:

```
처리 방식:
  해당 피스 스킵 (크롤링 중단 없음)
  경고 로그 저장

로그 형식:
  [SKIP] EquipmentSetPiece 연결 실패
  setName: 지국천왕
  slot: HELMET
  이유: EquipmentItem 조회 실패
  → 수동 처리 필요
```

### 5.2 세트 미매칭 처리

주술 크롤러에서 세트명으로 `EquipmentSet`을 찾지 못한 경우:

```
처리 방식:
  해당 RitualApplicability 스킵
  경고 로그 저장

로그 형식:
  [SKIP] RitualApplicability 연결 실패
  ritualName: 천추
  setName: 파멸자
  이유: EquipmentSet 조회 실패
  → 수동 처리 필요
```

### 5.3 로그 활용

```
크롤링 완료 후 SKIP 로그를 확인하여:
  1. 아이템/세트 크롤러가 먼저 실행됐는지 확인
  2. 누락된 아이템/세트가 있는지 확인
  3. 수동으로 DB에 직접 입력하거나 크롤러 재실행
```

### 5.4 거상짱 데이터 신뢰도 주의

```
확인된 문제:
  주술 적용 슬롯이 실제 게임과 다름
  → 파싱 시 슬롯명 무시, 세트명만 사용 (3.2 참고)

향후 발견되는 데이터 오류:
  로그 남기고 수동 처리
```

---

## 6. 이미지 처리

### 현재 (로컬 개발 단계)

```
이미지 URL만 DB에 저장
S3 업로드 없음
크롤링 데이터 정합성 확인 우선
```

### 추후 (S3 적용 단계)

```
1. AWS S3 버킷 생성
2. 크롤링 시 이미지 다운로드 → S3 업로드
3. DB에 S3 URL 저장
4. 로컬 테스트 완료 후 적용
```

---

## 7. 오픈 이슈

| 항목 | 내용 | 우선순위 |
|---|---|---|
| 아이템 크롤러 수정 | MATERIAL로 저장되는 문제 수정, EquipmentItem으로 저장되게 변경 | 높음 |
| 재료 크롤러 신규 구현 | MaterialItem 전용 크롤러 별도 작성 | 높음 |
| stat_unit 파싱 추가 | 기존 ItemStat 저장 로직에 stat_unit 파싱 추가 | 높음 |
| S3 이미지 업로드 | 데이터 정합성 확인 후 적용 | 낮음 |
| 거상짱 데이터 오류 수동 처리 | 주술 슬롯 한정 표기 외 추가 오류 발견 시 로그 확인 후 처리 | 낮음 |