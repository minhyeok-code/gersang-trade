# 클리어타임 · 사냥 허브 기획

> 작성일: 2026-06-08  
> 상태: 초안 (Draft)  
> 관련: [`recommend-plan.md`](./recommend-plan.md), [`personalization_cache_design.md`](./personalization_cache_design.md), [`gersang-grade-policy.md`](./gersang-grade-policy.md), [`data-migrations.ko.md`](./data-migrations.ko.md)

---

## 1. 개요

유저가 **덱 + 몬스터 + DPS + 클리어타임**을 기록하고, 충분한 데이터가 모이면 **몬스터별 구간 통계(과스펙·가성비·저스펙)**와 **타인 덱 스냅샷 구경**을 제공하는 기능이다.

| 구분 | 설명 |
|------|------|
| **개인** | 덱 페이지에서 클리어타임 등록 (스냅샷 생성) |
| **공개 (사냥 허브)** | 몬스터별 랭킹·구간 지표·스냅샷 덱 조회 (`/clear-time`) |
| **집계** | 몬스터당 표본 n건 단위 배치로 사분위수·구간 통계 갱신 |

---

## 2. 현재 구현 상태 (2026-06 기준)

### 백엔드

| 항목 | 상태 |
|------|------|
| `POST /api/users/me/clear-time` | ✅ `monsterId`, `deckId`(필수), `clearTimeSeconds` (6~26초) |
| `ClearTimeService` | ✅ `UserService`에서 분리 |
| `UserClearTime` 엔티티 | ✅ 스냅샷 FK, `finalDps`, `isPublic`, `status`, `expGranted` |
| `DeckSnapshot` | ✅ JSON + `content_hash` (동일 구성 재사용) |
| DPS 서버 재계산 | ✅ 저장 시 `DpsCalculatorService` |
| EXP 중복 hash | ✅ |
| EXP 일 3회 상한 | ⏸ 개발 중 비활성 (`ClearTimeService` 주석) — 운영 시 §3.3.1 |
| `isPublic` 토글 | ✅ API·DB, **초기 운영 UI 숨김·강제 public** |
| `GET /api/users/me/clear-times` | ✅ 내 기록 목록 |
| `GET /api/users/me/hunt-hub-status` | ✅ 해금 진행도 |
| `GET /api/hunt/monsters` | ✅ 공개 몬스터 목록 (미해금 허용) |
| `GET /api/hunt/monsters/{id}/records` | ✅ 해금 시 랭킹 (`403` 게이트) |
| `GET /api/hunt/snapshots/{id}` | ✅ 해금 시 스냅샷 (`403` 게이트) |
| 구간 통계 배치 | ❌ Phase 4 |

### 프론트엔드

| 항목 | 상태 |
|------|------|
| `front/src/app/deck/page.tsx` | 덱·몬스터·DPS 계산 ✅ |
| `api.submitClearTime` | ✅ 덱 페이지 DPS 패널 하단 연동 |
| `/clear-time` (사냥 허브) | ❌ 미구현 |

### 덱 모델 주의사항

- `UserDeck` 주석은 “불변 스냅샷”이나, **실제 `DeckService`는 동일 `deckId`를 in-place 수정**한다.
- 따라서 **`deckId`만으로 “당시 덱”을 식별·조회할 수 없다.**
- 클리어타임 저장 시점에 **덱 구성 전체를 `DeckSnapshot`으로 복사·고정**해야 한다.
- `deckId`는 “어느 덱 슬롯에서 기록했는지” 참고용 메타로만 유지한다.

---

## 3. 개인 흐름 (등록)

### 3.1 UX 파이프라인

```
덱 설정 → 몬스터 설정 → DPS 확인 → 클리어타임 저장
```

| 단계 | 스냅샷 |
|------|--------|
| 덱 / 몬스터 / DPS 확인 | ❌ 생성하지 않음 (라이브 데이터) |
| **클리어타임 저장** | **✅ 이 시점에만 스냅샷 생성** |

### 3.2 저장 시 함께 고정할 데이터

한 트랜잭션으로 저장한다.

| 필드 | 설명 |
|------|------|
| `monsterId` | 대상 몬스터 |
| `clearTimeSeconds` | 평균 클리어타임 (초, 정수, **6~26**) |
| `deckId` | **필수** — 본인 소유 덱인지 서버 검증. 당시 덱 조회용이 **아님** |
| `finalDps` | 저장 시점 DPS — **서버 재계산 후 스냅샷** |
| `DeckSnapshot` | 아래 §3.2.1 스키마로 **덱 전체 복사** |
| `isPublic` | **`UserClearTime` 컬럼** — 사냥 허브 랭킹·공개 API 노출 여부 (기본 `true`) |
| `recordedAt` | 기록 시각 |

#### 3.2.1 `isPublic` 운영 정책

| 구분 | 내용 |
|------|------|
| **엔티티** | `UserClearTime.is_public` (스냅샷이 아님) |
| **DB 기본값** | `true` |
| **API** | `ClearTimeRequest.isPublic` (optional, null → true) |
| **초기 운영** | `gersang.hunt.clear-time-public-toggle-enabled: false` — UI·요청값 무시, **항상 public** |
| **추후** | 설정 `true` + 덱 페이지 체크박스 노출 시 유저 선택 반영 |

초기에는 공개/비공개 선택을 **프론트에 노출하지 않는다**. 백엔드·DB 필드는 유지해 두고, 운영 데이터를 보며 비공개 옵션 필요 여부를 판단한다.

#### 3.2.2 `DeckSnapshot` 스키마

`DeckDetailResponse`만으로는 부족하다. DPS 재현·공개 덱 뷰어에 필요한 항목을 **JSON 한 덩어리**로 저장한다.

| 포함 항목 | 출처·비고 |
|-----------|-----------|
| 덱 메타 | 이름, `attrXValue`, `totalResDown` |
| 멤버 12슬롯 | 용병 ID·이름, 레벨, 보너스 스탯 |
| 슬롯 장비·주술 | `DeckMemberSlotResponse` 동등 정보 |
| **용병 특성** | `characteristicId` + `selectedLevel` (DPS에 직접 사용, API 응답에 없음) |
| 덱 효과 | 정령·진법·층진 등 (`DeckEffectResponse`) |
| DPS 계산 컨텍스트 | `resistanceType`, `memberInputs` (§3.6) |

| 스냅샷 컬럼 | 설명 |
|-------------|------|
| `content_json` | 위 항목 직렬화 |
| `content_hash` | `content_json`을 **정규화(canonical) 후 해시** — 동일 덱 구성 판별용 |
| `created_at` | 생성 시각 |

**`content_hash`란?**  
스냅샷 JSON을 필드 순서·키 정렬 등으로 **항상 같은 문자열**이 나오게 만든 뒤 SHA-256 등으로 만든 지문이다. `deckId`가 같아도 장비를 바꾸면 hash가 달라지고, 구성이 완전히 같으면 hash가 같다. EXP 중복 방지·“동일 덱” 판별에 쓴다.

공개 허브·스냅샷 뷰어는 **`GET /api/hunt/snapshots/{id}` → `DeckSnapshot`만** 조회한다. `GET /api/decks/{id}`는 본인 라이브 덱 전용.

### 3.3 중복 저장·EXP 정책

| 규칙 | 내용 |
|------|------|
| 기록 중복 | 동일 유저 + 동일 `deckId` + 동일 몬스터 **허용** (스펙업 후 재기록) |
| 동일 덱 판별 | `deckId`가 아니라 **`DeckSnapshot.content_hash`** |
| EXP 미지급 (중복) | **동일 몬스터 + 동일 `content_hash`** 재저장 시 EXP 없음 (기록은 저장 가능) |
| 클리어타임 EXP 일일 상한 | **하루 최대 3회** 지급 (`+5 EXP` × 3, KST) — **운영 정책** (개발 중 §3.3.1 참고) |
| 클리어타임 유효 범위 | **6초 이상 27초 미만** (6~26초). 미만·이상은 **400 거부** |

> **거래 EXP와 분리:** 일 3회 상한은 **클리어타임 저장 EXP만** 해당한다. 거래 확정 EXP(§3-1)에는 일일 상한 없음. 지급 로직·카운트도 `ClearTimeService`에서만 처리한다.  
> EXP 상세: [`gersang-grade-policy.md`](./gersang-grade-policy.md) §3-2

#### 3.3.1 개발 / 운영 — 클리어타임 EXP 일 3회 상한

| 환경 | 동작 |
|------|------|
| **개발 (현재)** | 일 3회 상한 **비활성** — 중복 `content_hash`만 EXP 미지급, 그 외 저장마다 `+5 EXP` |
| **운영 (전환 시)** | `ClearTimeService`에서 아래 주석 **해제** |

**파일:** `back/gersangtrade/src/main/java/org/example/gersangtrade/hunt/service/ClearTimeService.java`

1. `EXP_DAY_ZONE`, `DAILY_CLEAR_TIME_EXP_GRANT_LIMIT = 3` 상수 주석 해제  
2. `resolveExpEarned()` 내 `todayExpGrants` 카운트·`>= DAILY_CLEAR_TIME_EXP_GRANT_LIMIT` 분기 주석 해제  

```java
// 운영 활성화 예시 (현재는 주석 처리됨)
private static final int DAILY_CLEAR_TIME_EXP_GRANT_LIMIT = 3;

LocalDateTime startOfDay = LocalDate.now(EXP_DAY_ZONE).atStartOfDay();
long todayExpGrants = clearTimeRepository
        .countByUserIdAndRecordedAtGreaterThanEqualAndExpGrantedTrue(userId, startOfDay);
if (todayExpGrants >= DAILY_CLEAR_TIME_EXP_GRANT_LIMIT) {
    return 0L;
}
```

운영 전환 체크리스트: 위 코드 복구 → `gersang-grade-policy.md` §3-2와 일치 확인 → 일 3회 초과 시 `expGranted=false`·`+0 EXP` 동작 테스트.

### 3.4 덱 페이지 UI (1차)

- DPS 패널 하단에 클리어타임 입력 (분+초 또는 초).
- “평균 클리어타임” 안내 문구.
- 저장 성공 시 EXP 토스트 (`+5 EXP`, 미지급 시 `+0 EXP` 안내).
- 해금 진행도: `서로 다른 몬스터 N/3` (§5 참고).
- **공개/비공개 체크박스: 초기 운영 미노출** (§3.2.1). `isPublic` 필드는 보내지 않음 → 서버가 `true` 저장.

### 3.5 API 요청 검증 (`ClearTimeRequest`)

| 필드 | 검증 |
|------|------|
| `monsterId` | 필수, 존재하는 몬스터 |
| `deckId` | **필수**, 요청 유저 소유 덱 |
| `clearTimeSeconds` | 필수, **6 ≤ 값 ≤ 26** |

### 3.6 DPS `resistanceType` (저항 종류)

몬스터는 **타격 저항**과 **마법 저항** 두 가지가 있다. DPS 계산 시 덱 데미지가 **어느 저항 축**과 맞물리는지 정하는 값이 `resistanceType`이다.

**덱 편성 기준 — 서버 자동 판별** (`DeckResistanceTypeResolver`)

| 덱 내 사천왕·각성사천왕 (`MercenaryType`) | 해당 용병 속성 (`Mercenary.nature`) | `resistanceType` |
|------------------------------------------|-------------------------------------|------------------|
| `HEAVENLY_KING` 또는 `AWAKENED_HEAVENLY_KING` | **풍속성 (`WIND`)** — 예: 광목천왕, 각성 광목천왕 | `HITTING` (몬스터 **타격저항**) |
| 그 외 사천왕·각성사천왕 (화·수·뇌·토 등) | — | `MAGIC` (몬스터 **마법저항**) |
| 사천왕·각성사천왕 미편성 | — | `MAGIC` |

- 판별 대상은 **몬스터 속성이 아니라 덱에 올린 사천왕/각성사천왕의 속성**이다.
- 풍속성 사천왕·각성사천왕이 **1명이라도** 있으면 해당 덱 전체 DPS는 타격저항 축으로 계산한다.
- API 요청에 `resistanceType`을 **보내지 않으면** 위 규칙으로 자동 결정한다.
- 명시적으로 내면 그 값을 우선한다 (관리·디버그용).
- 스냅샷 저장 시 **실제 계산에 쓴 `resistanceType`** 을 함께 고정한다.

---

## 4. 공개 — 사냥 허브 (`/clear-time`)

### 4.1 제공 기능

1. **몬스터로 조회** — 검색·목록
2. **구간 지표 (과스펙·가성비·저스펙)** — 클리어타임 사분위수 기준, 구간별 대표 DPS
3. **구간별 기록 목록** — 공개된 클리어타임 + 스냅샷 요약
4. **덱 상세** — 스냅샷 기준 **읽기 전용** 전체 구성

### 4.2 구간 정의 (IQR · 클리어타임 기준)

몬스터별 유효 공개 기록의 클리어타임 분포에서 **사분위수(p25, p50, p75)** 를 구하고, 기록을 아래 세 구간으로 나눈다.

| 구간 (라벨) | 클리어타임 조건 | 의미 |
|-------------|-----------------|------|
| **과스펙** | **p25 이하** (가장 빠른 25%) | 평균보다 훨씬 빠른 클리어 |
| **가성비스펙** | **p25 초과 ~ p75 이하** | 중간 밴드 (IQR) |
| **저스펙** | **p75 초과** (가장 느린 25%) | 상대적으로 느린 클리어 |

각 구간 카드에는 해당 구간 기록들의 **DPS 평균(또는 중앙값)** 을 함께 표시한다.  
클리어타임으로 구간을 나누고, DPS는 그 구간의 **설명 지표(벤치마크)**.

기록 목록 API의 구간 필터는 위 경계값(`p25_seconds`, `p75_seconds`)으로 **조회 시점에 판정**하거나, 집계 배치 시점의 경계를 캐시 테이블에서 읽는다.

### 4.3 표본 수·배치 트리거

| 조건 | 동작 |
|------|------|
| 몬스터당 유효 공개 기록 **< 30건** | 구간 카드 **“데이터 수집 중”**, 사분위수 미표시 |
| **≥ 30건** 도달 | 최초 사분위수·구간 통계 배치 실행 |
| 이후 | 유효 기록이 **10건 단위**로 늘 때마다 재집계 트리거 |

- **POST 동기 집계 금지** — 저장은 가볍게, **Spring Batch / 비동기 Job**으로 집계.
- Job은 **idempotent 전체 재계산** (삭제·숨김 반영).

### 4.4 API 방향 (신규, 덱 API와 분리)

| 메서드 | 경로 (예) | 설명 |
|--------|-----------|------|
| `GET` | `/api/hunt/monsters` | 허브용 몬스터 목록·표본 수 |
| `GET` | `/api/hunt/monsters/{id}/stats` | 사분위수·구간 통계 |
| `GET` | `/api/hunt/monsters/{id}/records` | 공개 기록·랭킹 (구간 필터) |
| `GET` | `/api/hunt/snapshots/{id}` | 스냅샷 덱 상세 (읽기 전용) |

- 기존 `GET /api/decks/{id}`는 **본인 전용** 유지.
- 공개 조회는 **스냅샷 ID** 기준.
- 랭킹·기록 목록의 작성자 표시: **서비스 닉네임** (`User.nickname`). 게임 내 캐릭터명이 아님.

### 4.5 서버 구분 (2단계 검토)

- 거상은 서버별 스펙 격차가 큼 → 집계·랭킹을 **몬스터 + 서버** 단위로 분리 검토.

---

## 5. 사냥 허브 해금 (기여 게이트)

### 5.1 목적

공개 허브는 **타인이 올린 데이터**가 있어야 의미 있다.  
기여 없이 읽기만 하면 **콜드 스타트**가 발생하므로, **최소 기여 후 해금**한다.

### 5.2 조건

```
서로 다른 몬스터에 대한 유효 클리어타임 기록 ≥ 3건
```

- **3번 아무 몬스터**가 아니라 **`DISTINCT monster_id ≥ 3`**.
- `hidden` / 신고로 숨긴 기록은 **집계·해금 카운트에서 제외**.

### 5.3 boolean 컬럼 사용 안 함

- User에 `canAccessClearTimeHub` 같은 **boolean 컬럼은 두지 않는다.**
- 해금 여부는 서버에서 **`distinctMonsterClearCount >= 3`** 로 판단한다.
- (선택) 성능용으로 `distinct_monster_clear_count`만 User 또는 통계 테이블에 캐시할 수 있다.

### 5.4 서버 제한 (필수)

프론트 라우트만 막으면 API 우회 가능. **백엔드에서 강제**한다.

| API | `distinctMonsterClearCount < 3` |
|-----|----------------------------------|
| `GET /api/hunt/monsters/{id}/records` (상세·랭킹) | `403` |
| `GET /api/hunt/snapshots/{id}` | `403` |

**소프트 게이트 (UX 권장)**

- 미해금: 몬스터 목록·안내·진행도 `2/3`는 허용 가능.
- 미해금: 타인 스냅샷·덱 상세·구간 리더보드는 **403**.

### 5.5 해금 상태 API

`GET /api/users/me/hunt-hub-status` (로그인 필수)

| 필드 | 설명 |
|------|------|
| `distinctMonsterCount` | ACTIVE 기록 기준 서로 다른 몬스터 수 |
| `requiredDistinctMonsters` | 해금 필요 수 (기본 3, `gersang.hunt.unlock-required-distinct-monsters`) |
| `unlocked` | `distinctMonsterCount >= required` |

덱 페이지 클리어타임 패널에 `N/3` 진행도 표시.

### 5.6 카운트 저장

| 방식 | 설명 |
|------|------|
| **A. 조회 시 계산** | `COUNT(DISTINCT monster_id) WHERE user_id = ? AND hidden = false` — MVP 적합 |
| **B. User 컬럼 캐시** | `distinct_monster_clear_count` — 저장·숨김 시 갱신 |

### 5.7 해금 UX

- 덱 페이지: `사냥 허브 해금까지 2/3 몬스터`
- 3종 달성 시: 토스트 + `/clear-time` 링크 활성화

---

## 6. 배치 — 몬스터별 구간 통계

### 6.1 집계 테이블 (예: `monster_clear_time_stats`)

| 컬럼 | 설명 |
|------|------|
| `monster_id` | PK |
| `sample_count` | 유효 표본 수 |
| `p25_seconds`, `p50_seconds`, `p75_seconds` | 사분위수 (클리어타임) |
| `over_spec_avg_dps` | 과스펙 구간(p25 이하) DPS 평균 |
| `value_spec_avg_dps` | 가성비 구간(p25~p75) DPS 평균 |
| `under_spec_avg_dps` | 저스펙 구간(p75 초과) DPS 평균 (nullable) |
| `aggregated_at` | 마지막 집계 시각 |

트리거 조건은 §4.3 (최소 30건, 이후 10건 단위).

---

## 7. 신고

### 7.1 대상

- 기존 `Report` 인프라 확장.
- `ReportTargetType`에 **`DECK_SNAPSHOT`** 추가 (스냅샷 단위 신고 권장).

### 7.2 처리 정책

| 방식 | 채택 |
|------|------|
| 자동 **삭제** | ❌ |
| 자동 **숨김** (`hidden` / `status`) | ✅ — `TradeListing.hidden`과 동일 계열 |

- 동일 스냅샷에 **중복 신고 방지** (신고자당 1회).

---

## 8. 엔티티 관계 (목표)

```
UserClearTime
  ├── user_id
  ├── monster_id
  ├── clear_time_seconds
  ├── final_dps
  ├── deck_id              (참고용, FK 아님)
  ├── deck_snapshot_id     (FK)
  ├── is_public
  ├── status               (ACTIVE | HIDDEN | ...)
  └── recorded_at

DeckSnapshot (불변)
  ├── content_json
  ├── content_hash
  └── created_at
```

### 8.1 기존 데이터

현재 운영 DB에 저장된 `user_clear_times` 행은 **없음**. 스냅샷 컬럼 추가 시 백필 불필요.

---

## 9. DB 마이그레이션 시점

`DeckSnapshot`·`UserClearTime` 확장 컬럼·`monster_clear_time_stats` 등 **Flyway 마이그레이션은 운영 테스트 직전 단계**에서 일괄 적용한다.

- 개발 환경: `ddl-auto`로 스키마 선행 가능
- 프로덕션: Flyway 단일 소스 — [`data-migrations.ko.md`](./data-migrations.ko.md)에 DM 항목으로 등록
- 구현 중에는 엔티티·서비스 로직을 먼저 맞추고, 스키마 확정 후 마이그레이션 SQL 작성

---

## 10. 구현 단계

| Phase | 내용 |
|-------|------|
| **1** | 덱 페이지 등록 UI, 저장 시 스냅샷+DPS, `isPublic`, EXP 정책 완성 |
| **2** | `GET` 내 기록, 해금 카운트, `distinct >= 3` 서버 제한 |
| **3** | `/clear-time` 페이지, 공개 API, 스냅샷 덱 뷰어 |
| **4** | 사분위수 배치, 과스펙·가성비·저스펙 UI |
| **5** | 신고·자동 숨김, 관리자 연동 |

---

## 11. 관련 코드·문서

| 구분 | 경로 |
|------|------|
| 클리어타임 저장 | `ClearTimeService`, `UserClearTime` |
| 사냥 허브 조회·게이트 | `HuntHubService`, `HuntHubAccessService`, `HuntHubController` |
| DPS | `DpsCalculatorService`, `front/src/app/deck/page.tsx` |
| 덱 편집 | `DeckService` |
| 신고 | `Report`, `ReportTargetType` |
| EXP 정책 | [`gersang-grade-policy.md`](./gersang-grade-policy.md) |
| 데이터 마이그레이션 | [`data-migrations.ko.md`](./data-migrations.ko.md) |

---

## 12. 변경 이력

| 날짜 | 내용 |
|------|------|
| 2026-06-08 | 초안 — 스냅샷 저장 시점, 해금 `distinctMonsterClearCount >= 3` |
| 2026-06-08 | IQR 구간(과스펙·가성비·저스펙), EXP 정책, 스냅샷 스키마, 마이그레이션 시점, `ClearTimeService` 분리 반영 |
| 2026-06-08 | 클리어타임 EXP 일일 상한 거래와 분리 명시, `resistanceType` 풍속성=HITTING 자동 판별 |
| 2026-06-09 | `resistanceType` — 몬스터 속성이 아닌 덱 내 풍속성 사천왕·각성사천왕 기준으로 수정 |
| 2026-06-09 | `isPublic` 기본 true, 초기 운영 강제 public·UI 숨김 (`clear-time-public-toggle-enabled`) |
| 2026-06-09 | Phase 1 — `DeckSnapshot`, DPS·스냅샷 저장, 덱 페이지 클리어타임 UI |
| 2026-06-09 | Phase 2 — 내 기록·해금 상태 API, hunt 공개 API 403 게이트, 덱 페이지 진행도 |
| 2026-06-09 | Phase 3 — `/clear-time` 페이지, 스냅샷 덱 뷰어(특성·장비), 용병 특성 카탈로그 API |
| 2026-06-09 | 개발 편의 — 클리어타임 EXP 일 3회 상한 `ClearTimeService` 주석 비활성, §3.3.1 운영 전환 가이드 |
