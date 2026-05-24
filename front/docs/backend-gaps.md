# 백엔드 미흡/수정 필요 사항

> 프론트 개발 중 발견된 백엔드 누락·미흡 항목을 기록한다.  
> 프론트 윤곽이 잡히면 이 파일을 기준으로 백엔드 코드를 수정·추가한다.
>
> **상태 표기**
> - `⚠️ 부분구현` — API는 있지만 필요한 기능이 빠져 있음
> - `❌ 미구현` — API 자체가 없음
> - `❓ 점검필요` — 구현 여부 불확실, 실제 동작 확인 필요
> - `✅ 해결됨` — 백엔드 수정 완료

---

## 알려진 미흡 사항 (초기 목록)

### 1. 리스팅 목록 — `itemId` 필터 미구현
- **경로**: `GET /api/listings`
- **상태**: `⚠️ 부분구현`
- **문제**: `itemId` 쿼리 파라미터가 구현되지 않아 특정 아이템의 매물 목록 조회 불가.  
  아이템 자동완성으로 itemId를 선택한 뒤 해당 아이템 리스팅만 필터링하는 흐름이 깨짐.
- **필요 작업**: `ListingQueryService`(또는 해당 서비스)에 `itemId` 조건 추가

---

### 2. 시세 조회 — `days` 파라미터 미구현
- **경로**: `GET /api/items/{itemId}/price-history`
- **상태**: `⚠️ 부분구현`
- **문제**: 현재 `from`/`to` 날짜 직접 입력 방식. UI는 5일/10일/15일 버튼 선택 방식.  
  프론트에서 `days=10` 넘기면 백엔드가 오늘부터 N일 전 범위로 자동 계산해야 함.
- **필요 작업**: `days` 파라미터 추가 (또는 `from`/`to`를 프론트에서 계산해서 넘기는 방향으로 통일)

---

### 3. 판매·구매 리스팅 — 단일 목록 API 부재
- **경로**: `GET /api/listings` (판매), `GET /api/wanted` (구매)
- **상태**: `⚠️ 부분구현`
- **문제**: 거래 페이지 UI는 SELL/BUY를 한 화면에 좌/우로 나눠 보여줌.  
  현재 컨트롤러가 분리되어 있어 프론트에서 두 번 호출해야 하거나 통합 API가 필요함.
- **필요 작업**: 통합 여부 결정 — (A) `GET /api/listings?type=SELL|BUY|ALL` 통합 API 추가,  
  또는 (B) 프론트에서 병렬 호출로 처리

---

### 4. 판매 리스팅 수정 API 미구현
- **경로**: `PATCH /api/listings/{listingId}`
- **상태**: `❌ 미구현`
- **문제**: 등록한 리스팅의 가격·설명 수정이 불가능함.
- **필요 작업**: `PATCH /api/listings/{listingId}` 구현 (가격, 설명 수정)

---

### 5. 유저 등급 API — 점검 필요
- **경로**: `GET /api/grades`, `GET /api/users/me/grade`
- **상태**: `❓ 점검필요`
- **문제**: 구현 여부 불확실. 실제 응답 구조 확인 필요.
- **필요 작업**: 실제 엔드포인트 동작 확인 후 상태 업데이트

---

### 6. 홈 화면 — 관심 아이템 시세 API 점검 필요
- **경로**: `GET /api/home/price-watch`
- **상태**: `❓ 점검필요`
- **문제**: 구현 여부 불확실. 관심 아이템 등록/해제 API도 존재 여부 불명확.
- **필요 작업**: 관심 아이템 등록(`POST /api/watchlist`)·해제(`DELETE /api/watchlist/{itemId}`)·  
  시세 조회 흐름 전체 확인

---

### 7. 홈 화면 — 스펙업 추천 미구현
- **경로**: `GET /api/home/spec-up`
- **상태**: `❌ 미구현`
- **비고**: DPS 계산기 완성 후 이어서 구현 예정. 현재 우선순위 낮음.

---

## 프론트 개발 중 추가 발견 사항

### 8. 덱 장비 — 세트 전체 피스 조회/일괄 장착 API 미구현
- **경로**: `GET /api/sets/{setId}` / `PUT /api/decks/{deckId}/members/{memberId}/sets/{setId}`
- **상태**: `✅ 해결됨`
- **해결 내용**:
  - `GET /api/sets/{setId}` 응답에 `pieces` 배열 추가 (`SetDetailResponse`)  
    각 피스: `{ itemId, itemName, imageUrl, slot, equipSlot, ritualApplicable, pieceCount }`
  - `PUT /api/decks/{deckId}/members/{memberId}/sets/{setId}` 추가  
    반지(pieceCount=2)는 RING_1·RING_2 모두 장착. 기존 슬롯은 교체.

---

### 9. 덱 장비 — 주술 세트효과 적용/조회 API 미구현
- **경로**: `GET /api/decks/{deckId}/members/{memberId}/stats`
- **상태**: `✅ 해결됨`
- **해결 내용**:
  - `MemberStatResponse`에 `ritualStats`, `ritualSetEffects` 필드 추가
  - `ritualStats`: 착용 슬롯별 주술(outcome 포함) 스탯 합산 (`RitualStat` 조회)
  - `ritualSetEffects`: 발동된 세트효과 목록. 각 항목 — `ritualName`, `setName`, `outcome`, `appliedPieces`, `requiredPieces`, `statType`, `statValue`
  - `totalStats`에 `ritualStats` + 발동된 `ritualSetEffects` 자동 합산
  - `findByDeckMemberIdWithDetails` 쿼리에 `equipmentSet`, `sr.ritual` fetch join 추가

---

### 10. 덱 특성 — 용병 레벨/특성 선택 저장 API 미구현
- **경로**: `GET/PUT /api/decks/{deckId}/members/{memberId}/characteristics`
- **상태**: `✅ 해결됨`
- **해결 내용**:
  - `GET /api/decks/{deckId}/members/{memberId}/characteristics` 추가  
    응답: 용병의 전체 특성 카탈로그 + 현재 선택 레벨(`selectedLevel`, 미선택 시 null) + 레벨별 수치
  - `PUT /api/decks/{deckId}/members/{memberId}/characteristics` — 이미 구현됨
  - `PATCH /api/decks/{deckId}/members/{memberId}/level` — 이미 구현됨

---

### 11. 아이템 검색 — 세트명 기반 세트 식별자 부족
- **경로**: `GET /api/items/search`
- **상태**: `✅ 해결됨`
- **해결 내용**: `ItemSearchResult`에 `setId` 필드 추가. jOOQ 쿼리에서 `es.id AS set_id` 추가.

---

<!-- 프론트 개발하면서 발견되면 아래 형식으로 추가 -->
<!--
### N. 제목
- **경로**: `METHOD /api/...`
- **상태**: `⚠️ 부분구현` / `❌ 미구현` / `❓ 점검필요`
- **문제**: 구체적으로 무엇이 문제인지
- **필요 작업**: 백엔드에서 해야 할 작업
-->
