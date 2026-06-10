# 관심 아이템 시세 기능 구현 초안

메인 페이지(`/`) **관심 아이템 시세** 섹션을 실제 데이터로 동작시키기 위한 구현 계획서다.  
프론트는 이미 `GET /api/home/price-watch`를 호출하지만, 백엔드·관심목록 저장소는 **미구현** 상태다.

---

## 0. 재검토 메모 (2026-06)

| 항목 | 결과 |
|------|------|
| §8 `TradeListing::getPrice` / `WantedListing::getOfferedPrice` | **이슈 없음** — `ListingQueryRepository.search()` → `List<TradeListing>`, `WantedListingQueryRepository.search()` → `List<WantedListing>`, 각 price 필드 존재 (코드 확인 완료) |
| POST 한도·중복 상태코드 | **409 / 422 + errorCode** 로 반영 (§5-0) |
| `serverId` 누락 | **`400` 고정** — 빈 `targets: []` 옵션 제거 (§3-4) |
| `resolveStatKey` 세트 한계 | **누락** 문제로 §3-2에 코드 근거 명시 |
| 배치 IN·인덱스·캐시 | Step 4·§4-3에 초기 구현으로 반영 |
| `GET /api/servers` | **이슈 없음** — `ServerController` 이미 구현됨, 프론트 `api.getServers()` 존재. §2·Step 0b 오기 수정 |
| `sort_order` 컬럼 | **MVP에서 항상 0** — 순서 변경 API 없으므로 2차 전용임을 DDL 주석에 명시 (§4-1 갱신) |
| `GlobalExceptionHandler` 호환 | 기존 핸들러는 `error` 키 사용. `errorCode` 필드·422 응답은 `WatchlistException` + 별도 핸들러 필요. Step 1에 명시 |
| `BANSSANG` + 주술 | `SetTitleGenerator`가 반쌍에서 주술 접두 무시 → `ritual` 필드 전달 시 `400` 또는 `RC:0:MARK:ANY` 강제 (§5-1) |
| ritual 2차 필터 | `searchLatestPerItemIds` top N 후 ritual 필터 시 결과가 5건 미만 가능 — 후보를 넓게 fetch 후 cap 정책 명시 (§8) |
| `findRecentByStatKeysAndServer` | key당 top 5는 `ROW_NUMBER() OVER (PARTITION BY …)` 또는 앱 그룹핑 필요 — 구현 방식 Step 3에 명시 |
| 인덱스 prefix 30 | SET watchKey(`SET:3:COMP:FULL:RC:5:MARK:XX` = 31자+) 초과 가능 → prefix 64 이상으로 보정 (§4-3) |
| 캐시 `allEntries = true` | watchlist add/remove 시 전 사용자 캐시 비워짐 → user 단위 evict로 보정 (Step 4) |
| `Double.NaN` → `null` | `average().orElse(Double.NaN)` → API 응답에서 `null` 변환 필요 (Step 4) |
| `resolveStatKey` BUY 분기 | Step 0 범위에 SELL뿐 아니라 BUY(삽니다 확정) SET 분기도 포함 필요 |

---

## 1. 목표

| 항목 | 설명 |
|------|------|
| 관심 매물 등록 | 로그인 사용자가 **단품/재료(`ITEM`)** 또는 **세트+주술(`SET`)** 단위로 관심목록 추가·삭제 |
| 판매 시세 | 조건에 맞는 **팝니다(ACTIVE)** 등록글 최신 5건 → **평균가** + 목록 |
| 구매 시세 | 조건에 맞는 **삽니다(OPEN)** 등록글 최신 5건 → **평균가** + 목록 |
| 거래완료 | `TradeConfirmed`에서 동일 watch key·서버 **최신 확정 5건** → 가격·시각 목록 |

**범위 밖 (이번 초안)**
- 관심 매물 신규 등록 알림(푸시/웹소켓)
- `TradeStatDaily` 기반 7일/15일 변동률 차트 (기존 UI의 `changeRate`는 2차로 연기)

---

## 2. 현재 코드 갭

### 프론트 (`front/src/app/page.tsx`)

- 로그인 시 `api.getPriceWatch()` 호출
- 기대 필드: `itemId`, `itemName`, `recentAvg`, `prevAvg`, `changeRate` (구 API 가정)
- 관심 등록 UI 없음, 3카테고리(판매/구매/거래완료) 표시 없음
- 세트·주술 등록 UI는 **거래 페이지 필터·`SetPieceConfigurator` 패턴 재사용 가능** (`front/src/app/trade/page.tsx`)

### 백엔드

| 구성요소 | 상태 |
|----------|------|
| `GET /api/home/price-watch` | ❌ 컨트롤러 없음 |
| 관심목록 엔티티/API | ❌ 없음 |
| 판매 최신 N건 by itemId | ✅ `ListingQueryRepository` + `itemId` 필터 |
| 판매 세트+주술 매칭 | ⚠️ `bundleType=EQUIPMENT_SET` + `equipment_set_id` 조회 가능, **주술·구성 필터 로직 신규** (`SetTitleResolver` 재사용) |
| 구매 최신 N건 by itemId | ⚠️ `WantedListingQueryRepository`에 **itemId 필터 없음** |
| 구매 세트+주술 매칭 | ⚠️ `WantedRitualCondition` 존재, **세트 구성 단위 집계 로직 신규** |
| 거래완료 최신 N건 | ⚠️ `TradeConfirmedRepository`에 **statKey+server 조회 없음** |
| SET statKey 스냅샷 | ⚠️ `resolveStatKey()`가 세트 거래 시 **대표 피스 `ITEM:{id}`만** 저장 → SET·비대표 피스 watch의 **거래완료 누락** (§3-2) |

### 재사용 가능 API·로직

| 용도 | 경로/클래스 |
|------|-------------|
| 단품·재료 검색 | `GET /api/items/search?q=` |
| 세트 검색·피스 목록 | `GET /api/sets`, `GET /api/sets/{id}` |
| 주술 목록 | `GET /api/rituals` (또는 세트 피스별 `ritualApplicable` 조회) |
| 판매 목록 | `GET /api/listings?itemId=&server=&status=ACTIVE&size=5` |
| 구매 목록 | `GET /api/wanted?server=&status=OPEN&size=5` (+ itemId/set 필터 확장) |
| 세트 제목·주술 규칙 | `SetTitleGenerator`, `SetTitleResolver` |
| **서버 목록** | `GET /api/servers` ✅ **구현됨** — `ServerController` 존재, 프론트 `api.getServers()` 사용 중 |
| statKey 규약 | `docs/entity-model.ko.md` §13.5 |

---

## 3. 설계 결정 (MVP)

### 3-1. 관심 단위 — `WatchTargetType`

두 가지 등록 타입을 **동일 watchlist**에 공존시킨다.

| 타입 | 용도 | 식별 필드 |
|------|------|-----------|
| `ITEM` | 재료, 단품 장비, 주술 단품 | `itemId`, (선택) `ritualMark` |
| `SET` | 세트 거래 단위 (갑투/변/풀/풀반쌍 + 주술) | `setId`, `composition`, `ritualCount`, `ritualMark` |

#### SET `composition` (세트 구성)

`SetTitleGenerator.BundleKind`와 1:1 대응:

| 값 | 거래 표기 예 |
|----|-------------|
| `GAMTU` | `{세트명}갑투`, `2{마크}{세트명}갑투` |
| `BYEON` | `변{세트명}`, `3{마크}변{세트명}` |
| `BANSSANG` | `{세트명}반쌍` |
| `FULL` | `풀 {세트명}`, `5{마크} 풀 {세트명}` |
| `FULL_BANSSANG` | `풀 {세트명}반쌍`, `5{마크} 풀 {세트명}반쌍` |

#### 주술 필드

| 필드 | ITEM | SET |
|------|------|-----|
| `ritualMark` | 단품 **장비**만 허용. `null`이면 주술 무관. **재료(`MaterialItem`)에 전달 시 `400`** (`INVALID_WATCH_TARGET`) | 동일 마크. `null`이면 주술 없는 구성만 |
| `ritualCount` | 사용 안 함 (`null`) | 주술 적용 피스 수 (0=주술 무관, 2/3/5 등). `SetTitleGenerator` 규칙과 일치. **`BANSSANG`·`FULL_BANSSANG`은 주술 접두가 제목에 반영되지 않으므로 ritual 필드 전달 시 `RC:0:MARK:ANY` 강제** |

표시 라벨(`displayLabel`)은 서버에서 생성:
- `ITEM` → `item.name` (+ ritualMark 있으면 접미)
- `SET` → `SetTitleGenerator.generateByKind(setName, composition, ritualCount, ritualMark)`

### 3-2. watchKey (중복 방지·거래완료 매칭)

`entity-model.ko.md` §13.5 statKey 규약을 watchlist에도 적용한다.

| 타입 | watchKey 형식 | 예시 |
|------|---------------|------|
| ITEM (주술 없음) | `ITEM:{itemId}` | `ITEM:42` |
| ITEM (주술 있음) | `ITEM:{itemId}:RITUAL:{mark}` | `ITEM:42:RITUAL:XX` |
| SET | `SET:{setId}:COMP:{composition}:RC:{ritualCount}:MARK:{mark\|ANY}` | `SET:3:COMP:FULL:RC:5:MARK:XX` |

- `UNIQUE(user_id, watch_key)` 로 중복 등록 방지
- 거래완료 조회: `stat_key_snapshot = watchKey` **정확 일치** 우선  
  SET의 경우 Step 0 이후 `ChatService`가 동일 형식으로 스냅샷 저장

#### `resolveStatKey` 현재 한계 (반드시 인지)

현재 `ChatService.resolveStatKey()` 동작 (`chat/service/ChatService.java`):

```java
// SELL + EQUIPMENT_SET 번들
List<ListingBundle> bundles = listingBundleRepository.findByListingIdOrderByIdAsc(room.getListingId());
List<BundleLine> lines = bundleLineRepository.findByBundleIdOrderBySortOrderAsc(bundles.get(0).getId());
if (!lines.isEmpty()) {
    return "ITEM:" + lines.get(0).getItem().getId();  // 첫 번째 피스만
}
```

- **5피스 세트 등록글** 확정 시 → 위 코드로 **첫 번째 라인 itemId**만 `ITEM:{id}` 저장
- 나머지 4피스·세트 단위(`SET:…`) 키는 저장되지 않음

**영향**

| watch 타입 | 거래완료 탭 |
|------------|-------------|
| `ITEM` (단품·재료·특정 피스) | 해당 `ITEM:{id}`로 조회 가능 |
| `ITEM` (세트의 **비대표** 피스 id로 등록) | 세트 거래 확정이 대표 피스 키로만 쌓이면 **내역 누락** |
| `SET` (풀 5XX 등) | Step 0 전까지 **거의 항상 비어 있음** (기존 확정은 대표 피스 `ITEM:{id}`만 존재) |

→ “세트 등록글 가격이 섞인다”가 아니라, **세트·비대표 피스 거래완료가 statKey 불일치로 조회되지 않는 구조적 누락**이다.  
Step 0(SET statKey) 완료 전까지 UI에 거래완료 빈 목록 가능 — **데이터 한계 안내** 문구 권장 (툴팁 수준이 아님).

### 3-3. 매칭 규칙 (판매·구매)

#### 판매 (`TradeListing`)

| 타입 | 1차 DB 필터 | 2차 애플리케이션 필터 |
|------|-------------|----------------------|
| ITEM | `itemId` in bundle lines | `ritualMark` 있으면 해당 라인 주술 마크 일치 |
| SET | `bundleType=EQUIPMENT_SET`, `equipment_set_id=setId` | `SetWatchMatcher.matchesSellBundle(bundle, composition, ritualCount, ritualMark)` — 내부에서 `SetTitleResolver`로 제목 재계산 후 비교 |

#### 구매 (`WantedListing`)

| 타입 | 매칭 |
|------|------|
| ITEM | wanted item `item_id` 일치 + (ritualMark 있으면) `WantedRitualCondition`의 applied mark 일치 |
| SET | 해당 `setId` 피스 itemId들이 wanted에 포함 + 주술 조건이 watch `ritualCount`/`ritualMark`와 호환. **`SetWatchMatcher.matchesWantedListing(...)`** 신규 |

> 구매 쪽 SET 매칭은 판매보다 느슨할 수 있음(MVP: 세트 피스 중 **대표 피스 1개 이상** + 주술 조건 일치). 정밀도는 테스트로 보정.

### 3-4. 서버 스코프

- 시세는 **사용자가 선택한 서버 1개** 기준
- API: `GET /api/home/price-watch?serverId={id}` — **`serverId` 필수**
- **`serverId` 누락·null·0 이하** → **`400 Bad Request`** (빈 `targets: []` 반환하지 않음)

```json
{
  "errorCode": "SERVER_ID_REQUIRED",
  "message": "serverId 쿼리 파라미터가 필요합니다."
}
```

- 프론트: `getServer()` 없으면 API 호출 전에 서버 미선택 UI만 표시 (400에 의존하지 않음)
- `ServerRepository.findById(serverId)` 실패 → **`404 Not Found`** (`SERVER_NOT_FOUND`)
- 성공 시 `name`으로 listing/wanted 필터, `server_snapshot` 매칭

### 3-5. 평균·건수

- 판매·구매: 조건 일치 등록글 **최신 5건** 가격의 단순 산술평균 (`count==0` → `avgPrice: null`)
- 거래완료: **평균 없음**, 최신 5건 목록만

### 3-6. 관심목록 한도·삭제

- 사용자당 최대 **5개** watch entry (ITEM·SET 합산)
- 6번째 등록 시 **`422 Unprocessable Entity`** + `errorCode: WATCH_LIMIT_EXCEEDED` (중복과 HTTP 코드 분리)
- 중복 watchKey 등록 시 **`409 Conflict`** + `errorCode: DUPLICATE_WATCH_ITEM`
- **삭제 필수** — 홈 카드·관심목록 UI에서 항목별 삭제 버튼 → `DELETE /api/watchlist/{entryId}`
  - `entryId` = `user_watch_targets.id` (ITEM/SET 공통)
  - 본인 소유만 삭제 (`403`), 없는 id `404`, 성공 `204 No Content`
  - 이미 삭제된 항목 재삭제: `404` (idempotent하지 않음 — 프론트는 목록 갱신 후 UI에서 제거)

---

## 4. 데이터 모델

### 4-1. 테이블 `user_watch_targets`

```sql
CREATE TABLE user_watch_targets (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    target_type     VARCHAR(10)  NOT NULL,  -- ITEM | SET
    watch_key       VARCHAR(255) NOT NULL,
    item_id         BIGINT       NULL,
    set_id          BIGINT       NULL,
    composition     VARCHAR(20)  NULL,      -- GAMTU | BYEON | BANSSANG | FULL | FULL_BANSSANG
    ritual_count    INT          NULL,      -- SET 전용 (0~5)
    ritual_mark     VARCHAR(20)  NULL,
    sort_order      INT          NOT NULL DEFAULT 0,  -- 2차 순서 변경 기능 전용. MVP에서는 항상 0 (정렬 기준: created_at)
    created_at      DATETIME(6)  NOT NULL,
    CONSTRAINT fk_uwt_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_uwt_item FOREIGN KEY (item_id) REFERENCES items(id),
    CONSTRAINT fk_uwt_set  FOREIGN KEY (set_id)  REFERENCES equipment_sets(id),
    CONSTRAINT uq_uwt_user_watch_key UNIQUE (user_id, watch_key),
    INDEX idx_uwt_user_sort (user_id, sort_order, created_at)
);
```

**제약 (애플리케이션 검증)**
- `target_type=ITEM` → `item_id` NOT NULL, `set_id` NULL
- `target_type=SET` → `set_id` NOT NULL, `composition` NOT NULL

### 4-2. JPA·Enum·패키지 배치

기존 `domain/user/`에는 `User`, `RefreshToken`, `UserClearTime` 등만 있음. **엔티티·enum은 동일 패키지**에 추가하고, **Repository는 레이어별 분리** (`listing/repository/`, `wanted/repository/`와 동일 패턴).

| 파일 | 패키지 | 설명 |
|------|--------|------|
| `WatchTargetType.java` | `domain/user/enums/` | `ITEM`, `SET` |
| `SetComposition.java` | `domain/user/enums/` | `GAMTU`, `BYEON`, … |
| `UserWatchTarget.java` | `domain/user/` | JPA 엔티티 (`UserWatchItem` 아님 — 타입 혼재 반영) |
| `UserWatchTargetRepository.java` | `user/repository/` | `findByUserIdOrderBy…`, `countByUserId`, `existsByUserIdAndWatchKey`, `deleteByIdAndUserId` |
| `WatchKeyBuilder.java` | `watchlist/service/` | watchKey 생성·파싱 |
| `SetWatchMatcher.java` | `watchlist/service/` | 판매·구매 세트+주술 매칭 |

Flyway: `V{n}__user_watch_targets.sql`

### 4-3. `trade_confirmed` 조회 인덱스 (Step 3 마이그레이션에 포함)

거래완료 탭 조회 빈도가 높으므로 statKey·서버·시각 복합 인덱스를 명시한다.

```sql
-- V{n}__trade_confirmed_statkey_index.sql (또는 Step 3 마이그레이션에 포함)
ALTER TABLE trade_confirmed
  ADD INDEX idx_tc_statkey_server_confirmed (stat_key_snapshot(64), server_snapshot(30), confirmed_at DESC);
```

- `cancelled = false`는 쿼리 WHERE에 유지 (부분 인덱스는 MySQL 버전에 따라 선택)
- prefix **64** — SET watchKey(`SET:3:COMP:FULL_BANSSANG:RC:5:MARK:XX` = 40자+)가 30자를 초과할 수 있음. exact match에서 prefix 초과 시 row 재검증이 발생하므로 여유있게 설정

---

## 5. API 명세

### 5-0. 공통 에러 응답

비즈니스 오류는 HTTP 상태 + JSON body로 구분한다. 프론트는 **`errorCode`** 로 분기한다.

> **기존 `GlobalExceptionHandler` 호환 주의**  
> 현재 전역 핸들러는 `error` 키를 사용하고 `IllegalStateException → 409` 고정이다.  
> watchlist 전용 `WatchlistException`과 별도 `@ExceptionHandler`를 추가해 `errorCode`·`422` 응답을 처리한다 (기존 핸들러 수정 금지 — 기존 API 응답 깨짐 방지).  
> 신규 에러 응답에는 프론트 파싱 일관성을 위해 **`error` 필드도 함께** 포함한다.

```json
{
  "errorCode": "DUPLICATE_WATCH_ITEM",
  "error": "conflict",
  "message": "이미 등록된 관심 매물입니다."
}
```

| errorCode | HTTP | 용도 |
|-----------|------|------|
| `DUPLICATE_WATCH_ITEM` | `409` | 동일 `watchKey` 재등록 |
| `WATCH_LIMIT_EXCEEDED` | `422` | 5개 한도 초과 (`current`, `max` 포함, §5-1) |
| `SERVER_ID_REQUIRED` | `400` | `price-watch` serverId 누락 |
| `SERVER_NOT_FOUND` | `404` | 잘못된 serverId |
| `WATCH_TARGET_NOT_FOUND` | `404` | DELETE 대상 없음 |
| `WATCH_FORBIDDEN` | `403` | 타인 watch entry 삭제 시도 |

### 5-1. 관심목록

#### `GET /api/watchlist`

```json
[
  {
    "id": 1,
    "targetType": "SET",
    "watchKey": "SET:3:COMP:FULL:RC:5:MARK:XX",
    "displayLabel": "5XX 풀 공명",
    "itemId": null,
    "setId": 3,
    "setName": "공명",
    "composition": "FULL",
    "ritualCount": 5,
    "ritualMark": "XX",
    "sortOrder": 0,
    "createdAt": "2026-06-09T10:00:00"
  },
  {
    "id": 2,
    "targetType": "ITEM",
    "watchKey": "ITEM:101",
    "displayLabel": "흑요석",
    "itemId": 101,
    "setId": null,
    "composition": null,
    "ritualCount": null,
    "ritualMark": null,
    "sortOrder": 0,
    "createdAt": "2026-06-09T10:05:00"
  }
]
```

#### `POST /api/watchlist`

**단품·재료**
```json
{ "targetType": "ITEM", "itemId": 101 }
```

**단품 장비 + 주술**
```json
{ "targetType": "ITEM", "itemId": 42, "ritualMark": "XX" }
```

**세트 + 구성 + 주술**
```json
{
  "targetType": "SET",
  "setId": 3,
  "composition": "FULL",
  "ritualCount": 5,
  "ritualMark": "XX"
}
```

**세트, 주술 없음**
```json
{
  "targetType": "SET",
  "setId": 3,
  "composition": "FULL",
  "ritualCount": 0,
  "ritualMark": null
}
```

- `201 Created` + 등록된 `WatchTargetResponse` body

**에러 (상태코드·errorCode 분리)**

| 조건 | HTTP | errorCode |
|------|------|-----------|
| watchKey 중복 | `409` | `DUPLICATE_WATCH_ITEM` |
| 5개 한도 초과 | `422` | `WATCH_LIMIT_EXCEEDED` (+ `current`, `max`) |
| 없는 item/set | `404` | `ITEM_NOT_FOUND` / `SET_NOT_FOUND` |
| composition·주술 조합 위반 | `400` | `INVALID_WATCH_TARGET` |
| **재료에 `ritualMark` 전달** | `400` | `INVALID_WATCH_TARGET` |
| **`BANSSANG`·`FULL_BANSSANG`에 ritual 전달** | `400` or 자동 보정 | `INVALID_WATCH_TARGET` (400) 또는 `RC:0:MARK:ANY` 강제 후 `201` — 팀 정책에 따라 선택 |

```json
// 중복 (409)
{ "errorCode": "DUPLICATE_WATCH_ITEM", "error": "conflict", "message": "이미 등록된 관심 매물입니다." }

// 한도 초과 (422) — 프론트가 current/max로 "5/5" 표시 가능
{
  "errorCode": "WATCH_LIMIT_EXCEEDED",
  "error": "unprocessable_entity",
  "message": "관심목록은 최대 5개까지 등록할 수 있습니다.",
  "current": 5,
  "max": 5
}
```

프론트: `422` → “목록이 가득 찼습니다”, `409` → “이미 등록됨” 토스트 분리.

#### `DELETE /api/watchlist/{entryId}`

관심목록 항목 1건 삭제. 로그인 필수, **본인이 등록한 entry만** 삭제 가능.

| HTTP | 조건 |
|------|------|
| `204 No Content` | 삭제 성공 |
| `401 Unauthorized` | 비로그인 |
| `403 Forbidden` | 타인의 entry |
| `404 Not Found` | 존재하지 않는 `entryId` |

- 경로 파라미터는 `itemId`가 아니라 **`entryId`** (watchlist 행 PK). ITEM/SET 타입 혼재 시에도 단일 삭제 API로 처리.
- 삭제 후 프론트: `GET /api/watchlist` 또는 `GET /api/home/price-watch` 재조회로 목록·시세 갱신.

---

### 5-2. 홈 시세 집계

#### `GET /api/home/price-watch?serverId=3`

```json
{
  "serverId": 3,
  "serverName": "천상계",
  "targets": [
    {
      "entryId": 1,
      "targetType": "SET",
      "watchKey": "SET:3:COMP:FULL:RC:5:MARK:XX",
      "displayLabel": "5XX 풀 공명",
      "sell": {
        "avgPrice": 1500000000,
        "count": 3,
        "listings": [
          { "listingId": 10, "price": 1400000000, "displayTitle": "5XX 풀 공명", "createdAt": "..." }
        ]
      },
      "buy": {
        "avgPrice": 1200000000,
        "count": 2,
        "listings": [
          { "wantedId": 5, "offeredPrice": 1100000000, "createdAt": "..." }
        ]
      },
      "completed": {
        "count": 2,
        "dataQuality": "OK",
        "trades": [
          { "confirmedPrice": 1450000000, "confirmedAt": "..." }
        ]
      }
    }
  ]
}
```

응답 최상위 키는 `items` 대신 **`targets`** (ITEM/SET 혼재 반영).

**`completed.dataQuality` 값**

| 값 | 조건 |
|----|------|
| `"OK"` | watchKey가 `ITEM:{id}` 형식이고 해당 아이템의 거래 확정이 정상 집계됨 |
| `"LIMITED"` | `SET:…` watchKey이거나 비대표 피스 ITEM인 경우 — Step 0 전까지 기존 확정 데이터가 없을 수 있음 |

프론트: `"LIMITED"`이면 거래완료 탭에 "데이터 축적 중입니다" 안내 표시 (툴팁이 아닌 인라인 문구).

**에러**

| 조건 | HTTP | errorCode |
|------|------|-----------|
| `serverId` 쿼리 누락 | `400` | `SERVER_ID_REQUIRED` |
| 존재하지 않는 serverId | `404` | `SERVER_NOT_FOUND` |
| 비로그인 | `401` | (공통) |

---

## 6. 백엔드 구현 단계

### Step 0 — SET statKey 스냅샷 (거래완료 연동)

| 작업 | 파일 |
|------|------|
| `resolveStatKey` 확장 | `chat/service/ChatService.java` |
| SELL + EQUIPMENT_SET 번들 | `SET:{setId}:COMP:{composition}:RC:{n}:MARK:{mark\|ANY}` 형식 저장 (`SetTitleResolver`로 composition·ritualCount·mark 산출) |
| **BUY + EQUIPMENT_SET 번들** | 삽니다 확정 시 wanted 첫 번째 item이 세트 피스인 경우도 동일 SET 형식으로 저장 — `WantedItem → EquipmentItem → setId` 조회 후 판단 |
| 기존 ITEM 거래 | 기존 `ITEM:{itemId}` 유지 |
| Test | `ChatServiceTest` — SELL 세트 / BUY 세트 확정 시 statKey 형식, ITEM 거래 기존 형식 유지 |

> Step 0 없이도 판매·구매 시세는 동작. **거래완료(SET)** 는 신규 확정부터 데이터 축적.

### Step 1 — 관심목록 CRUD

| 작업 | 파일 |
|------|------|
| Flyway | `V{n}__user_watch_targets.sql` |
| Entity·Enum | `UserWatchTarget`, `WatchTargetType`, `SetComposition` |
| `WatchKeyBuilder` | watchKey 생성·검증 |
| **`WatchlistException`** | `errorCode`(String) + `httpStatus` 보유. `DUPLICATE_WATCH_ITEM`(409) / `WATCH_LIMIT_EXCEEDED`(422) / `INVALID_WATCH_TARGET`(400) 등 전용 예외 |
| **`WatchlistExceptionHandler`** | `@RestControllerAdvice` — `WatchlistException` → `{ errorCode, error, message, (current, max) }` 응답. 기존 `GlobalExceptionHandler` 수정 없음 |
| Service·Controller | `WatchlistService` (add 시 `count >= 5` 거부, 재료 ritualMark·BANSSANG ritual 검증, `remove(entryId, userId)`), `WatchlistController` (`GET`/`POST`/`DELETE`) |
| Test | ITEM/SET 등록, 중복 **409**, 한도 **422**, DELETE 본인/타인/404, 잘못된 composition 400, 재료 ritualMark 400, BANSSANG ritual 보정 |

### Step 2 — Listing·Wanted 검색 확장

| 작업 | 설명 |
|------|------|
| `ListingQueryRepository` | `setId`, `bundleType=EQUIPMENT_SET` 필터 파라미터 추가 |
| `WantedListingQueryRepository` | `itemId`, `setId`(피스 JOIN) 필터 추가 |
| `SetWatchMatcher` | 판매 번들·구매 희망 세트+주술 2차 필터 |
| Test | 풀 5XX 세트만 통과, 갑투-only 제외 등 |

### Step 3 — 거래완료 최신 N건 + 인덱스

```java
// 단건
findRecentByStatKeyAndServer(String watchKey, String serverName, int limit)

// 배치 (watchKey 목록 ≤5)
findRecentByStatKeysAndServer(List<String> watchKeys, String serverName, int limitPerKey)
```

- `stat_key_snapshot = watchKey`, `cancelled = false`, `ORDER BY confirmed_at DESC`
- Flyway: §4-3 `idx_tc_statkey_server_confirmed` 추가 (prefix 64)
- SET watchKey prefix(`LIKE 'SET:{setId}%'`) 과거 호환은 2차

**`findRecentByStatKeysAndServer` 구현 방식 선택**

| 방식 | 설명 | 권장 여부 |
|------|------|-----------|
| `ROW_NUMBER() OVER (PARTITION BY stat_key_snapshot ORDER BY confirmed_at DESC)` 서브쿼리 | MySQL 8.0+ 윈도우 함수. 쿼리 1회, 앱 처리 최소 | ✅ 권장 |
| `watchKeys IN (...)` 넉넉히 fetch 후 앱에서 key별 정렬·slice(5) | 구현 단순. watch ≤5이므로 과잉 fetch 미미 | 대안 (윈도우 함수 불가 시) |

watch 최대 5개 + key당 5건 → 최대 25행 fetch. 어느 방식이든 부하 무시 가능.

### Step 4 — PriceWatchService + HomeController (배치·캐시 포함)

**배치 집계 (처음부터 — N+1 방지)**

관심목록 최대 5개이지만, target마다 3쿼리 루프(최대 15쿼리+α) 대신 **그룹별 배치**로 설계한다.

```java
// 1) 거래완료 — watchKey IN (≤5) 1쿼리 (+ 앱에서 key별 top 5)
completedMap = tradeConfirmedRepo.findRecentByStatKeysAndServer(watchKeys, serverName, 5);

// 2) ITEM 타입 itemId들 — 판매/구매 각 1쿼리 (ROW_NUMBER PARTITION BY item_id)
//    QueryDSL: ROW_NUMBER() OVER (PARTITION BY item_id ORDER BY created_at DESC) ≤ 5
sellByItemId  = listingQueryRepo.searchLatestPerItemIds(server, ACTIVE, itemIds, 5);
buyByItemId   = wantedQueryRepo.searchLatestPerItemIds(server, OPEN, itemIds, 5);

// 3) SET 타입 — setId별 후보 배치 조회 → SetWatchMatcher로 key별 top 5 (후보는 setId IN 1~2쿼리)
```

SET는 2차 필터(`SetWatchMatcher`)가 필요해 ITEM만큼 단순 IN+ROW_NUMBER가 어렵다. SET watch 수 ≤5이므로 **후보 조회는 배치, 매칭·cap은 서비스**로 처리.

**평균 계산 (서비스 레이어)** — 코드베이스 확인 완료 (§0)

| Repository | 반환 타입 | 가격 getter |
|------------|-----------|-------------|
| `ListingQueryRepository.search()` | `List<TradeListing>` | `getPrice()` |
| `WantedListingQueryRepository.search()` | `List<WantedListing>` | `getOfferedPrice()` |

```java
// PriceWatchService 내부
// listings: DB top N 후보, ritual 2차 필터 적용 (ITEM+ritual watch)
// → 필터 후 count < 5 허용. 후보는 top 20으로 넓게 fetch 후 필터·cap
OptionalDouble rawSellAvg = listings.stream().mapToLong(TradeListing::getPrice).average();
Long sellAvg = rawSellAvg.isPresent() ? (long) rawSellAvg.getAsDouble() : null;  // NaN → null

OptionalDouble rawBuyAvg = wantedList.stream().mapToLong(WantedListing::getOfferedPrice).average();
Long buyAvg = rawBuyAvg.isPresent() ? (long) rawBuyAvg.getAsDouble() : null;
```

> **ritual 2차 필터 정책**: DB에서 itemId 기준 최신 **top 20** fetch → 앱에서 ritualMark 필터 → 최대 5건 cap.  
> 필터 후 5건 미만이면 그대로 반환 (`count: N`). **"최소 5건 보장"은 없음** — 일치하는 매물이 그만큼인 것.

API 응답 DTO 조립은 서비스·컨트롤러에서 `ListingSummaryResponse` 등으로 변환 후 `avgPrice`·`count`만 노출.

**캐시 (초기부터 적용 — 2차로 미루지 않음)**

```java
@Cacheable(value = "priceWatch", key = "#userId + ':' + #serverId")
public PriceWatchResponse getPriceWatch(Long userId, Integer serverId) { ... }

// Spring Cache 와일드카드 미지원 → CacheManager로 userId 단위 수동 evict
// (서버 13개 × userId 키 순회 evict 또는 캐시 키를 userId만으로 단순화)
private void evictPriceWatchByUser(Long userId) {
    Cache cache = cacheManager.getCache("priceWatch");
    if (cache != null) {
        serverRepository.findAll().forEach(s ->
            cache.evictIfPresent(userId + ":" + s.getId()));
    }
}

public WatchTargetResponse add(...) {
    // ...
    evictPriceWatchByUser(userId);
}

public void remove(Long entryId, Long userId) {
    // ...
    evictPriceWatchByUser(userId);
}
```

- `CacheConfig`에 `priceWatch` 캐시 등록, TTL 1~3분
- **`allEntries = true` 금지** — 다른 사용자 캐시까지 비워짐
- watchlist 변경 시 해당 userId 키만 evict

| 작업 | 파일 |
|------|------|
| DTO | `PriceWatchResponse`, `PriceWatchTargetResponse`, 스냅샷 하위 DTO |
| Repository | `searchLatestPerItemIds`, `findRecentByStatKeysAndServer` (QueryDSL) |
| Service | `home/service/PriceWatchService.java` |
| Controller | `home/controller/HomeController.java` — serverId 누락 시 400 고정 |
| Config | `CacheConfig` — `priceWatch` region |

---

## 7. 프론트엔드 구현 단계

### Step 5 — API 타입·메서드 (`front/src/lib/api.ts`)

```ts
export type WatchTargetType = 'ITEM' | 'SET';
export type SetComposition = 'GAMTU' | 'BYEON' | 'BANSSANG' | 'FULL' | 'FULL_BANSSANG';

export interface WatchTargetDto {
  id: number;
  targetType: WatchTargetType;
  watchKey: string;
  displayLabel: string;
  itemId?: number | null;
  setId?: number | null;
  composition?: SetComposition | null;
  ritualCount?: number | null;
  ritualMark?: string | null;
}

export interface WatchTargetAddBody {
  targetType: WatchTargetType;
  itemId?: number;
  setId?: number;
  composition?: SetComposition;
  ritualCount?: number;
  ritualMark?: string | null;
}

getWatchlist: () => request<WatchTargetDto[]>('/api/watchlist'),
addWatchTarget: (body: WatchTargetAddBody) =>
  request<WatchTargetDto>('/api/watchlist', { method: 'POST', body: JSON.stringify(body) }),
removeWatchTarget: (entryId: number) =>
  request<void>(`/api/watchlist/${entryId}`, { method: 'DELETE' }),
getPriceWatch: (serverId: number) =>
  request<PriceWatchResponseDto>(`/api/home/price-watch?serverId=${serverId}`),
```

### Step 6 — 메인 페이지 UI

**`components/home/InterestPriceWatchPanel.tsx`** (분리 권장)

| UI | 동작 |
|----|------|
| 등록 탭 | `단품·재료` / `세트` 토글 |
| 단품·재료 | `SearchBar` → `POST { targetType: ITEM, itemId }` |
| 세트 | 세트 검색(`GET /api/sets`) → 구성 선택(갑투/변/풀/풀반쌍) → 주술(없음/마크/피스수) — **거래 페이지 사이드바와 동일 옵션** |
| 목록 | `displayLabel` + 타입 뱃지(`세트`/`단품`) + **삭제(X)** → `removeWatchTarget(entry.id)` |
| 한도 안내 | 등록 5/5일 때 추가 버튼 비활성 + “최대 5개” 문구 |
| POST 에러 | `WATCH_LIMIT_EXCEEDED` (422, `current`/`max`) / `DUPLICATE_WATCH_ITEM` (409) 분기 |
| 거래완료 빈 목록 | SET·비대표 피스 — §3-2 한계 안내 문구 (Step 0 전) |
| 시세 카드 | 판매 평균 / 구매 평균 / 거래완료 5건 |
| 갱신 | `server-changed`, watchlist 변경 시 재조회 |

**표시 예**
```
[세트] 5XX 풀 공명                              [삭제]
판매 평균  15억 전 (3건)    구매 평균  12억 전 (2건)
거래완료   14.5억 · 15억 · 14억
```

### Step 7 — 문서·체크리스트 갱신

- `api-checklist-spec.md`, `backend-gaps.md` §6

---

## 8. 집계 쿼리·평균 계산 요약

| 카테고리 | ITEM | SET |
|----------|------|-----|
| 판매 | DB top **20** fetch → ritual 2차 필터 → cap 5 | `setId IN` 후보 → `SetWatchMatcher` → top 5 |
| 구매 | DB top **20** fetch (wanted) → ritual 필터 → cap 5 | set 피스 기반 후보 → `SetWatchMatcher` → cap 5 |
| 거래완료 | `watchKey IN (...)` 배치, key=`ITEM:…` | key=`SET:…` (**Step 0 이후** 신규 확정부터) |

> **ritual 2차 필터**: DB에서 itemId 기준 top 20 → 앱에서 ritualMark 일치 → 최대 5건 cap. 필터 후 count < 5는 허용 (매물이 그만큼인 것). "최소 5건 보장" 없음.

**평균가** (§0 재검토: 시그니처·getter **문제 없음**)

- `TradeListing.getPrice()`, `WantedListing.getOfferedPrice()` — Repository가 엔티티 반환
- 평균은 `PriceWatchService`에서만 계산

**거래완료 데이터 한계** — §3-2 `resolveStatKey` 참고. Step 0 이전 SET watch·비대표 피스 ITEM watch는 completed가 비어 있을 수 있음.

---

## 9. 테스트 계획

| 레이어 | 케이스 |
|--------|--------|
| WatchKeyBuilder | ITEM/SET 키 생성, MARK ANY, 파싱 round-trip |
| WatchlistService | ITEM·SET 등록, 한도 **422**, 중복 **409**, DELETE 본인만, errorCode body |
| SetWatchMatcher | 5XX 풀 매칭 O, 3XX 변만 매칭 X, 주술 없는 풀 매칭 |
| PriceWatchService | 배치 쿼리, SET sell avg, ITEM buy 0건, completed watchKey exact, 캐시 hit/evict |
| HomeController | serverId 누락 **400** 고정 |
| ChatService | SET 확정 statKey 스냅샷 |
| 프론트 | `409`/`422` errorCode 분기, 세트 등록, 삭제 후 갱신, 5개 한도 UI |

---

## 10. 구현 순서 (권장)

```
Step 0  ChatService SET statKey 스냅샷 (거래완료 SET 연동, SELL+BUY 양쪽)
Step 1  user_watch_targets + Watchlist CRUD (ITEM·SET) + WatchlistException 핸들러
Step 2  Listing/Wanted setId 필터 + SetWatchMatcher
Step 3  TradeConfirmed watchKey 최신 5건 쿼리
Step 4  PriceWatchService + HomeController
Step 5  api.ts
Step 6  메인 UI (단품·세트 등록 + 3카테고리 시세)
Step 7  문서 갱신
```

예상 작업량: 백엔드 2~3일, 프론트 1~1.5일 (SET 매칭·UI 포함).

---

## 11. 2차 확장 (참고)

| 항목 | 설명 |
|------|------|
| `changeRate` | `TradeStatDaily` 7일 전·후 avg 비교 |
| 알림 | watch target 신규 listing 등록 알림 |
| statKey 복합 컬럼 | `entity-model-review.md` 권장안 — 문자열 파싱 대신 FK 컬럼 |
| SET completed 과거 데이터 | `LIKE 'SET:{setId}%'` prefix 백필·마이그레이션 |
| 비로그인 | 서버별 인기 시세 기본 노출 |

> 캐시는 §Step 4에서 **초기 구현** (2차 아님).

---

## 12. 관련 파일

| 구분 | 경로 |
|------|------|
| 홈 UI | `front/src/app/page.tsx` |
| 거래 필터·세트 UI 참고 | `front/src/app/trade/page.tsx`, `components/value-test/SetPieceConfigurator.tsx` |
| 세트 제목 규칙 | `listing/service/SetTitleGenerator.java`, `SetTitleResolver.java` |
| statKey 규약 | `docs/entity-model.ko.md` §13.5 |
| statKey 생성(현재) | `chat/service/ChatService.resolveStatKey()` |
| 판매 검색 | `listing/repository/ListingQueryRepository.java` |
| 구매 검색 | `wanted/repository/WantedListingQueryRepository.java` |
| 거래 확정 | `trade/repository/TradeConfirmedRepository.java` |
