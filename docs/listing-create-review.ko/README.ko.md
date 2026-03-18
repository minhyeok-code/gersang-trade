# 매물(거래 등록글) 등록 기능 코드 리뷰 메모 (문제점 정리)

대상 기능: **사용자 아이템 매물 등록(POST `/api/listings`)**

범위: Controller/Service/Repository/DTO/Entity를 읽고 **문제점만 문서화** (코드 수정 없음)

---

## 1) 공개 조회 API 경로가 Security 설정과 불일치 (의도와 다르게 인증 필요해질 수 있음)

### 근거
- `ListingController`의 base path는 `/api/listings`
  - `back/gersangtrade/src/main/java/org/example/gersangtrade/listing/controller/ListingController.java`
- `SecurityConfig`의 공개 GET 허용 경로는 `"/listings/**"`로 되어 있음
  - `back/gersangtrade/src/main/java/org/example/gersangtrade/config/SecurityConfig.java`

### 문제
문서/주석상 “비로그인도 조회 가능” 의도인데, 실제 Security matcher가 `/api/listings`를 포함하지 않으면
GET `/api/listings`, GET `/api/listings/{id}`가 기본 정책(`anyRequest().authenticated()`)에 걸릴 수 있음.

### 영향
- 비로그인 목록/상세 조회가 **401**로 실패할 수 있음 (운영 장애성)
- 프론트/클라가 “공개 조회” 전제 로직이면 화면/기능이 깨짐

### 권장 대응(방향)
- Security matcher와 실제 API prefix(`/api`)를 일치시키기
- 공개 허용 범위를 최소화하면서도 의도된 공개 API는 확실히 permitAll 되도록 테스트로 고정

---

## 2) DELETE API가 실제 삭제를 수행하지 않는데 204를 반환 (기능상 버그)

### 근거
- `ListingController.delete()`는 TODO만 있고 서비스 호출이 없음
  - `back/gersangtrade/src/main/java/org/example/gersangtrade/listing/controller/ListingController.java`

### 문제
클라이언트는 성공(204)으로 인식하지만, 실제로는 아무 것도 변경되지 않을 수 있음.

### 영향
- 사용자 경험(UX) 불일치: “삭제됐다고 보이는데 다시 나타남”
- 데이터 정합성/운영 대응 혼란

### 권장 대응(방향)
- 미구현이면 501/400 등으로 명확히 막거나, 실제 cancel/soft-delete 로직을 연결
- “본인 글만 삭제” 요구가 있으니 권한/소유자 검증도 포함 필요

---

## 3) 요청 검증 누락으로 DB 제약에서 500이 날 수 있음 (길이/범위 등)

### 근거
- 엔티티 컬럼 길이 제한 존재
  - `TradeListing.server` length=30
  - `TradeListing.note` length=500
  - `back/gersangtrade/src/main/java/org/example/gersangtrade/domain/listing/TradeListing.java`
- 요청 DTO에는 길이 제한이 없음
  - `back/gersangtrade/src/main/java/org/example/gersangtrade/listing/dto/request/ListingCreateRequest.java`

### 문제
클라이언트가 긴 문자열을 보내면 validation(400)로 걸러지지 않고,
DB/ORM 단계에서 터져서 500(DataIntegrityViolation 등)로 노출될 가능성이 큼.

### 영향
- “사용자 입력 실수”가 서버 에러(500)로 보임 → 신뢰도/UX 하락
- 로그에 예외가 쌓여 운영 노이즈 증가

### 권장 대응(방향)
- DTO에 길이 제한을 명시하고 400으로 반환되게 설계
- 에러 메시지/코드 일관화

---

## 4) 예외 타입이 `IllegalArgumentException` 위주라 HTTP 코드/응답 일관성이 깨질 가능성

### 근거
- 사용자 없음/아이템 없음/정책 위반을 `IllegalArgumentException`으로 throw
  - `back/gersangtrade/src/main/java/org/example/gersangtrade/listing/service/ListingService.java`
- 프로젝트 내 `@ControllerAdvice`(전역 예외 매핑)가 확인되지 않음(현재 워크스페이스 기준)

### 문제
전역 예외 매핑이 없거나 단순하면, 케이스별로 400/404/409로 나뉘어야 할 오류가
500으로 뭉개져 나갈 수 있음.

### 영향
- 클라이언트가 오류 원인을 구분하기 어려움
- “없는 리소스(404)”와 “정책 위반(400/409)”이 섞여 디버깅이 어려움

### 권장 대응(방향)
- 예외를 도메인/케이스별로 구분하거나(최소한 NotFound/BadRequest/Conflict),
  전역 예외 처리에서 명확히 매핑

---

## 5) 주술(ritual) 요청에서 중복 `ritualId`가 오면 DB UNIQUE 제약으로 500 가능

### 근거
- `BundleEquipmentRitual`에 `(bundle_line_id, ritual_id)` UNIQUE 제약 존재
  - `back/gersangtrade/src/main/java/org/example/gersangtrade/domain/listing/BundleEquipmentRitual.java`
- 서비스는 요청 리스트를 그대로 `saveAll()` 하며, 사전 중복 체크가 보이지 않음
  - `back/gersangtrade/src/main/java/org/example/gersangtrade/listing/service/ListingService.java`

### 문제
클라이언트가 같은 `ritualId`를 중복으로 보내면 DB에서 충돌 나고 서버 에러로 보일 가능성.

### 영향
- 사용자 입력/클라 버그가 서버 500으로 노출
- 재시도 시 같은 오류 반복(원인 파악 어려움)

### 권장 대응(방향)
- 요청 단계에서 ritualId 중복을 400으로 차단하거나, 중복을 병합/정규화

---

## 6) 비즈니스 규칙(정책) 누락 가능성: bundleType/정렬/수량 제약

현재 구현은 다음 정합성은 잘 체크함:
- 장비 라인이면 equipmentDetail 필수
- 재료 라인은 `ItemType.MATERIAL`이어야 함
- 장비 상세: 타입, 외변 강화 수치 정책, 주술 적용 가능 여부 검증

다만 정책 요구가 있다면 아래는 추가 검증이 필요할 수 있음(현재 코드에서 강제되지 않음):
- **bundleType별 라인 구성 제한** (예: `EQUIPMENT_SINGLE`은 라인 1개만 허용 등)
- **sortOrder 중복/연속성 규칙** (현재는 중복 sortOrder 저장 가능)
- **장비 quantity 제한** (현재 DTO는 1 이상만 강제 → 장비도 2 이상 가능)

---

## 체크리스트(테스트 관점)
- 비로그인 GET `/api/listings`, `/api/listings/{id}`가 실제로 200인지(보안 설정과 일치 여부)
- 길이 초과 입력(server>30, note>500)이 400으로 처리되는지(또는 500으로 터지는지)
- 주술 리스트에 중복 ritualId를 넣었을 때의 응답(400 vs 500)
- DELETE `/api/listings/{id}`가 실제로 동작하는지(현재는 204만 반환)

---

## 구매 매물(구매 희망 등록글) 등록 기능 추가 리뷰 (POST `/api/wanted`)

대상 기능: **구매 희망 등록(POST `/api/wanted`)**

관련 파일:
- Controller: `back/gersangtrade/src/main/java/org/example/gersangtrade/wanted/controller/WantedListingController.java`
- Service: `back/gersangtrade/src/main/java/org/example/gersangtrade/wanted/service/WantedListingService.java`
- Entity/Repo:
  - `back/gersangtrade/src/main/java/org/example/gersangtrade/domain/wanted/WantedListing.java`
  - `back/gersangtrade/src/main/java/org/example/gersangtrade/domain/wanted/WantedItem.java`
  - `back/gersangtrade/src/main/java/org/example/gersangtrade/domain/wanted/WantedEquipmentCondition.java`
  - `back/gersangtrade/src/main/java/org/example/gersangtrade/domain/wanted/WantedRitualCondition.java`

---

## A) 공개 조회 API 경로가 Security 설정과 불일치 (Wanted도 동일 이슈)

### 근거
- `WantedListingController`의 base path는 `/api/wanted`
- `SecurityConfig`의 공개 GET 허용 경로는 `"/listings/**", "/stats/**"`만 존재

### 문제/영향
판매 매물과 동일하게, `/api/wanted`가 permitAll 범위에 포함되지 않으면
GET `/api/wanted`, GET `/api/wanted/{id}`가 인증 필요로 동작할 수 있음(401).

---

## B) 요청 검증 누락으로 DB 제약에서 500이 날 수 있음 (길이/범위)

### 근거
- 엔티티 컬럼 길이 제한 존재
  - `WantedListing.server` length=30
  - `WantedListing.note` length=500
  - `back/gersangtrade/src/main/java/org/example/gersangtrade/domain/wanted/WantedListing.java`
- 요청 DTO에는 길이 제한이 없음
  - `WantedListingCreateRequest`의 `server`, `note`
  - `back/gersangtrade/src/main/java/org/example/gersangtrade/wanted/dto/request/WantedListingCreateRequest.java`

### 문제/영향
- 길이 초과 입력이 validation(400)에서 걸러지지 않으면 DB/ORM 단계에서 500으로 노출될 수 있음.

---

## C) 주술 조건(ritualConditions) 중복 ritualId 입력 시 UNIQUE 제약으로 500 가능

### 근거
- `WantedRitualCondition`에 `(wanted_item_id, ritual_id)` UNIQUE 제약 존재
  - `back/gersangtrade/src/main/java/org/example/gersangtrade/domain/wanted/WantedRitualCondition.java`
- 서비스는 요청 리스트를 그대로 `saveAll()`하며 사전 중복 체크가 보이지 않음
  - `back/gersangtrade/src/main/java/org/example/gersangtrade/wanted/service/WantedListingService.java`

### 문제/영향
클라이언트가 같은 `ritualId`를 중복으로 보내면 DB에서 충돌 → 서버 500으로 보일 가능성이 큼.

---

## D) 예외 타입이 `IllegalArgumentException`/`IllegalStateException` 위주 (응답 코드 일관성 리스크)

### 근거
- 사용자 없음/아이템 없음/정책 위반/차단 사용자 등을 `IllegalArgumentException`, `IllegalStateException`으로 처리
  - `back/gersangtrade/src/main/java/org/example/gersangtrade/wanted/service/WantedListingService.java`

### 문제/영향
전역 예외 매핑이 없거나 단순할 경우, 400/404/409로 구분되어야 할 오류가 500으로 섞여 나갈 수 있음.

---

## E) 비즈니스 규칙(정책) 누락 가능성: sortOrder/quantity, 장비 조건 세부 정책

현재 구현은 다음 정합성은 체크함:
- 장비 조건이 있으면 `ItemType.EQUIPMENT`인지 확인
- 장비 조건의 주술 조건은 “적용 가능한 주술인지”를 검증
- `hasRitual=true`면 주술 조건 리스트 1개 이상 요구(`isRitualConsistent()`)
- 차단 사용자(`UserStatus.BLOCKED`)는 구매 희망 등록 불가

다만 정책 요구가 있다면 아래는 추가 검증이 필요할 수 있음(현재 코드에서 강제되지 않음):
- **sortOrder 중복/연속성 규칙** (중복 sortOrder 저장 가능)
- **장비 quantity 제한** (DTO는 1 이상만 강제 → 장비도 2 이상 가능)
- **외변(APPEARANCE) 장비의 최소 강화 수치 정책**은 주석에만 있고 코드에서 강제되지 않음
  - `WantedEquipmentCondition` 주석: “외변은 null 또는 5”

---

## 구매 희망 등록 체크리스트(테스트 관점)
- 비로그인 GET `/api/wanted`, `/api/wanted/{id}`가 실제로 200인지(보안 설정과 일치 여부)
- 길이 초과 입력(server>30, note>500)이 400으로 처리되는지(또는 500으로 터지는지)
- 주술 조건 리스트에 중복 ritualId를 넣었을 때의 응답(400 vs 500)
- 차단 사용자(BLOCKED)가 POST `/api/wanted` 호출 시 기대한 코드/메시지로 거부되는지
