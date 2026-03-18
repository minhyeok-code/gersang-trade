### 엔티티 구조 설계(초안) — 거상 아이템 거래

> 목표:  
> - **재료 아이템**: 단일 아이템 N개 일괄 판매/개별 판매, 여러 종류 재료를 요구 수량대로 **세트(번들) 판매** 모두 수용  
> - **장비 아이템**: 외변(강화 5만 취급) / 일반(주술), **아이템별 허용 주술 리스트** 노출, 주술 **성공/대성공 표기** 저장  
> - **방어구 5피스 세트**: “풀 XX 00세트”, “풀 00 3XX” 같은 통용 표기를 **등록/조회**에서 생성 가능

---

### 1) 큰 구조(권장 레이어)
- **Catalog(정의/사전 데이터)**: 아이템/세트/주술의 “정의” (어떤 주술이 어떤 아이템에 가능한지)
- **Listing(거래글/등록 데이터)**: 사용자가 실제로 “무엇을 어떤 구성으로 얼마에” 파는지
- **Trade(거래 상태/확정 데이터)**: 신청/확정 및 시세 산출 원천(여기서는 엔티티 구조만 언급)

---

### 2) Catalog 엔티티(정의 데이터)

#### 2.1 `Item`
- **id**
- **name** (표준 아이템명)
- **type**: `MATERIAL` | `EQUIPMENT`
- **tradeCategory**(선택): “재료/장비” 외에도 추후 분류(예: 소모품 등) 확장용
- **검색용 필드(선택)**: 별칭/초성/태그 등

#### 2.2 `MaterialItem` (Item 1:1)
- **itemId(PK/FK → Item)**
- (필요 시) **stackUnitName**: “개/묶음” 등 표시용

> 재료는 “수량 중심”이라 옵션 구조가 단순함. “묶음 판매/개별 판매”는 Listing에서 수량으로 표현.

#### 2.3 `EquipmentItem` (Item 1:1)
- **itemId(PK/FK → Item)**
- **equipmentKind**: `APPEARANCE`(외변) | `NORMAL`(일반)
- **slot**: `WEAPON` | `HELMET` | `ARMOR` | `GLOVES` | `BELT` | `SHOES` | (기타 슬롯 확장)
- **setId(FK, nullable)**: 세트 소속이면 연결 (아래 `EquipmentSet`)

#### 2.4 `EquipmentSet`
- **id**
- **name**: “00세트” (정식 명칭)
- **setType**: `ARMOR_5PIECE` 등 (현재 요구는 방어구 5피스 중심)
- **pieceSlots**: (논리적으로) `{HELMET, ARMOR, GLOVES, BELT, SHOES}`
  - 구현은 관계 테이블(`EquipmentSetPiece`)로 두는 것을 권장

#### 2.5 `EquipmentSetPiece` (세트 구성 정의)
- **id**
- **setId(FK → EquipmentSet)**
- **slot** (위 slot enum)
- **equipmentItemId(FK → EquipmentItem)**: 해당 슬롯의 아이템 정의

> 이렇게 두면 “세트 5피스”를 표준화할 수 있고, Listing에서는 “세트를 판다”가 아니라 “세트 정의를 참고해 특정 피스들을 묶어 판다”를 표현할 수 있음.

---

### 3) 주술(강화) 정의 엔티티

#### 3.1 `Ritual` (주술 정의)
- **id**
- **displayName**: 주술 명칭(예: “XX주술”)
- **ritualType**: `WEAPON` | `ARMOR`
- **successMark**: `<00>`에 들어가는 표기(예: “XX”)  
- **greatSuccessMark**: `<**>`에 해당하는 표기(예: “XX**” 또는 별도 이름)
  - 요구사항: “성공하면 `<00>`”, “대성공하면 `<**>`로 아이템 명시가 바뀜” → **결과 표기 문자열을 별도로 저장**하는 게 안전

#### 3.2 `RitualApplicability` (주술 적용 가능 대상)
- **id**
- **ritualId(FK → Ritual)**
- **equipmentItemId(FK → EquipmentItem)** (또는 `Item` FK)

> 핵심: 사용자가 `00아이템`을 선택하면  
> `RitualApplicability`로 조인해서 **허용된 주술 목록만** 내려줄 수 있음.

---

### 4) Listing(거래 등록) 엔티티

#### 4.1 `TradeListing`
- **id**
- **sellerId** (회원)
- **server** (거상 서버)
- **status**: `ACTIVE` | `IN_TRADE` | `SOLD` | `CANCELLED`
- **price** (게임 내 재화)
- **note/contact** (연락/메모)
- **createdAt/updatedAt**

#### 4.2 `ListingBundle` (거래글 내 “판매 단위”)
> “재료 N개 단일 판매”, “재료 여러 종류 요구수량 세트 판매”, “장비 단품”, “장비 5피스 세트”를 모두 같은 개념으로 포장

- **id**
- **listingId(FK → TradeListing)**
- **bundleType**: `MATERIAL_BUNDLE` | `EQUIPMENT_SINGLE` | `EQUIPMENT_SET`
- **titleOverride(optional)**: 사용자가 임의로 제목을 덮어쓰는 경우(선택)

#### 4.3 `BundleLine` (번들 구성 라인: 여러 아이템+수량)
- **id**
- **bundleId(FK → ListingBundle)**
- **itemId(FK → Item)**: 재료/장비 모두 가능
- **quantity**: 재료면 필수(>=1), 장비면 보통 1
- **sortOrder**

> 재료 “a재료 n개 + b재료 m개 …” 세트 판매는 `BundleLine` 여러 개로 끝.
> “n개를 묶어 한번에/개별로”는 `quantity`로 표현하고, 필요하면 같은 아이템 라인을 여러 개로 쪼개는 UI도 가능.

---

### 5) 장비 상세(번들 라인 확장)
장비는 재료와 달리 “옵션/주술/강화/세트 구성”이 필요하므로 `BundleLine`에 1:1 확장 테이블을 둠.

#### 5.1 `BundleEquipmentDetail` (BundleLine 1:1)
- **bundleLineId(PK/FK → BundleLine)** (해당 라인이 장비일 때만 존재)
- **equipmentItemId(FK → EquipmentItem)** (정합성 체크용)
- **equipmentKindSnapshot**: `APPEARANCE` | `NORMAL` (등록 시점 스냅샷)
- **appearanceEnhanceLevel** (nullable)
  - 정책: 외변은 **5강만 거래 취급** → 저장은 가능하되, Listing 생성/노출 정책에서 0~4는 제한(또는 UI에서 입력 차단)
- **normalHasRitual**: boolean (일반장비 주술 적용 여부)

#### 5.2 `BundleEquipmentRitual` (장비 라인에 적용된 주술 결과)
- **id**
- **bundleLineId(FK → BundleLine)**
- **ritualId(FK → Ritual)**
- **outcome**: `SUCCESS` | `GREAT_SUCCESS`
- **appliedMarkSnapshot**: 문자열 (성공/대성공에 따라 최종 표기를 스냅샷으로 저장)

> 요구사항(성공 `<00>` / 대성공 `<**>` 명칭 변경)을 안정적으로 반영하려면  
> “주술 정의(`Ritual`)” + “결과(`outcome`)” + “최종 표기 스냅샷”을 같이 보관하는 편이 안전.

---

### 6) 방어구 5피스 세트(풀세트/부분주술 표기) 지원

#### 6.1 세트 판매를 어떻게 저장할까?
선택지 A(권장): **세트도 결국 `BundleLine`의 집합**으로 저장
- `ListingBundle.bundleType = EQUIPMENT_SET`
- 구성은 `BundleLine` 5개(HELMET/ARMOR/GLOVES/BELT/SHOES) + 각 라인의 `BundleEquipmentDetail`/`BundleEquipmentRitual`

이 방식의 장점:
- “풀세트”도, “변두리 3피스만 주술” 같은 **부분 주술**도 “각 피스에 주술이 있냐”로 자연스럽게 표현 가능
- 조회 시 “주술 적용 피스 수”를 계산하기 쉬움

#### 6.2 조회/표기 생성 규칙(추천)

##### (1) 기본 용어 매핑(슬롯 그룹)
- **갑투**: `HELMET + ARMOR`
- **장신요**: `GLOVES + BELT + SHOES`
- **풀세트**: 위 5슬롯 모두 포함

##### (2) “풀 XX 00세트”
조건(예시 정책):
- 5슬롯 모두 존재(풀세트)
- 5피스 모두 같은 `Ritual`이 적용되어 있고(outcome은 혼합 가능), 적용된 주술 마크가 “XX”로 통일

표기:
- `풀 {주술마크} {세트명}`
  - 예: `풀 XX 00세트`

##### (3) “풀 00 3XX”
조건:
- 5슬롯 모두 존재(풀세트)
- 동일한 `Ritual`이 일부 피스에만 적용되어 있음(예: 3/5)

표기:
- `풀 {세트명} {주술피스수}{주술마크}`
  - 예: `풀 00세트 3XX` (사용자 예시 형태: “풀 00 3XX”)

> 구현 관점: 세트 번들의 `BundleLine(장비)`를 모아  
> - `totalPieces = 5`  
> - `ritualAppliedPieces = count(BundleEquipmentRitual 존재)`  
> - `ritualMark = appliedMarkSnapshot(또는 Ritual.success/greatSuccess mark 규칙)`  
> 를 계산해 표기 문자열을 만들 수 있음.

---

### 7) “아이템 선택 시 주술 리스트 제한”은 어떻게 연결되나?
API/조회 로직 관점(개념):
- 사용자가 등록 UI에서 장비를 선택(= `EquipmentItem` 또는 `Item`)
- 서버는 `RitualApplicability`에서 `equipmentItemId = 선택한 장비`로 조회
- 조인된 `Ritual` 목록만 내려줌

즉, 엔티티 연결은 다음이 핵심:
- `EquipmentItem (1) ← (N) RitualApplicability (N) → (1) Ritual`

---

### 8) 재료 “묶음/개별/여러종류 세트”는 어떻게 수용되나?
- **단일 재료 N개 일괄 판매**: `ListingBundle(MATERIAL_BUNDLE)` 1개 + `BundleLine(item=A, qty=N)` 1개
- **단일 재료를 여러 묶음으로 판매(개별/분할)**:
  - 옵션 1) 라인을 여러 개로 분할: `BundleLine(A, qty=10)` + `BundleLine(A, qty=10)` …
  - 옵션 2) “개별 단가”가 필요하면 `BundleLine`에 `unitPrice`를 추가(정책 결정 필요)
- **여러 재료 세트 판매**: `BundleLine(A, qty=n)`, `BundleLine(B, qty=m)`, … 를 같은 `ListingBundle`에 묶기

---

### 9) 최소 Enum 제안
- `ItemType`: MATERIAL, EQUIPMENT
- `EquipmentKind`: APPEARANCE, NORMAL
- `EquipmentSlot`: WEAPON, HELMET, ARMOR, GLOVES, BELT, SHOES, ...
- `RitualType`: WEAPON, ARMOR
- `RitualOutcome`: SUCCESS, GREAT_SUCCESS
- `ListingStatus`: ACTIVE, IN_TRADE, SOLD, CANCELLED
- `BundleType`: MATERIAL_BUNDLE, EQUIPMENT_SINGLE, EQUIPMENT_SET

---

### 10) 오픈 이슈(엔티티 확정 전 결정하면 좋은 것)
- **재료 개별 판매 단가**: “n개를 개별로”가 ‘한 글에서 여러 개별 라인’인지, ‘단가*수량’인지 정책 필요
- **세트 표기 규칙**: “풀 00 3XX”에서 `00`이 세트명인지/서브명인지(갑투/장신요 포함) 문자열 규칙 확정 필요
- **대성공 표기**: `<**>`가 “완전히 다른 이름”인지, “마크만 다른지” → `Ritual.greatSuccessMark` 형태로 고정 권장

---

### 11) (권장) 시세 통계를 위한 집계 테이블(rollup) 설계 아이디어

#### 11.1 왜 집계 테이블이 필요할 수 있나?
- 시세는 “조회 빈도↑ + 기간 범위↑” 성격이라, **거래 확정 원천 데이터를 매번 GROUP BY**하면 데이터가 쌓일수록 비용이 커짐
- 특히 “1달 이상/전체 기간” 같은 조회가 생기면 집계가 성능에 크게 영향을 줄 수 있음

#### 11.2 기본 원칙(정석)
- **원천(정답) 테이블**: “거래 확정 로그(Trade Confirmed)”는 반드시 남긴다
- **집계 테이블**: 원천을 기반으로 만든 “조회 최적화 캐시”이며, 필요하면 재생성 가능해야 한다

#### 11.3 집계 단위 추천
- **일 단위 집계**: 7/15/30/90/365 등 대부분 기간을 커버하기 좋음(가장 활용도가 높음)
- **월 단위 집계(선택)**: “1년/전체” 같은 매우 큰 범위를 자주 보면 추가로 고려

#### 11.4 집계 키(무엇 기준으로 시세를 낼지) 설계
시세의 “대상”을 어떤 키로 볼지 먼저 정해야 함. 아래는 확장 가능한 방식:
- **최소 키(권장 시작점)**: `itemId` 단위(아이템명 기준 시세)
- **정밀 키(확장)**: `itemId + (장비면) 주술마크/주술종류 + 외변5강 여부 + (세트면) 세트Id + 주술피스수` 등

> 주의: 키를 너무 세분화하면 집계 테이블이 폭발함.  
> 보통은 “기본 시세(아이템 단위)”를 먼저 만들고, 상세 옵션별 시세는 트래픽/니즈를 보고 확장.

#### 11.5 집계 엔티티(개념)
- `TradeConfirmed`(원천, 여기 문서에서는 미정의): 거래 확정 시점에 1행 생성(가격/수량/대상키 포함)
- `TradeStatDaily`(일 집계):
  - **statDate(YYYY-MM-DD)**
  - **statKey**(예: itemId 또는 itemId+옵션 조합)
  - **tradeCount**(거래 건수)
  - **quantitySum**(거래 수량 합; 재료에 특히 유용)
  - **priceSum**(평균 계산용)
  - **priceMin / priceMax**
- `TradeStatMonthly`(선택, 월 집계): 위 일 집계를 월로 롤업한 형태

#### 11.6 갱신(적재) 전략
- **트랜잭션 이벤트 기반(권장)**: “거래 확정”이 발생할 때
  - 원천(`TradeConfirmed`) insert
  - 같은 트랜잭션 또는 비동기 작업으로 `TradeStatDaily`를 **업서트**
- **배치 기반(대안)**:
  - 매일/매시간 “전날 데이터”를 다시 집계해 넣는 방식(재처리가 쉬움)

#### 11.7 조회 시 “원천 vs 집계”를 어떻게 쓸까?
- 7/15/30일 같은 짧은 구간은:
  - 데이터가 작으면 원천으로 바로 계산도 가능
  - 하지만 **일 집계**가 있으면 항상 일 집계에서 합산하는 게 단순하고 빠름
- “1달 이상/전체”는:
  - **일/월 집계**에서 합산하는 쪽이 확실히 유리

#### 11.8 집계 테이블 도입 시 주의점
- **정합성**: 집계는 캐시이므로 “원천이 정답”이어야 함(집계 재생성 가능)
- **취소/정정**: 거래 확정 취소/가격 수정 같은 이벤트가 있으면
  - 원천은 “수정/취소 이벤트”로 남기고
  - 집계는 “증감 반영” 또는 “해당 기간 재집계” 전략 중 택1
- **중복 방지**: 확정 이벤트가 중복 처리되면 집계가 틀어지므로
  - 원천에 고유키(예: tradeId)로 **멱등성** 확보가 중요

---

### 12) (추가기능) 능력치별 가성비 비교를 위한 능력치 마스터 + 월 시세 연결

#### 12.1 필요한 데이터 2가지
- **아이템 능력치(마스터/레퍼런스 데이터)**: 아이템이 “속성값/속성깎/저항깎”을 얼마나 주는지
- **전월 평균가(월 집계)**: `TradeStatMonthly`에서 “지난 달 평균가”를 가져오기

#### 12.2 엔티티(개념)
- `ItemStat`
  - `itemId(FK → Item)`
  - `statType`: `ELEMENT_VALUE` | `ELEMENT_PIERCE`(속성깎) | `RESIST_PIERCE`(저항깎)
    - `ELEMENT_VALUE`: 아이템이 부여하는 **속성값(δ)**. 데미지 계산 공식 `n% = (3x-y)/2`의 x 구성요소로 사용됨
    - `ELEMENT_PIERCE`: 속성깎 (상대 속성값 감소)
    - `RESIST_PIERCE`: 저항깎 (상대 저항값 감소)
  - `element(optional)`: 속성(화/수/풍/지/번개 등)까지 나누고 싶으면 사용
  - `value`

> `ELEMENT_VALUE`는 **가성비 비교 기능**(속성값 ÷ 전월 평균가)과 **속성값 데미지 계산기**(구매 시 데미지 증가분 m%p)에서 공유 사용됨.
> 별도 테이블 없이 `ItemStat` 행 하나로 두 기능을 모두 커버.
> 세부 계산 로직은 `docs/value-for-money-feature.ko.md` 6절 참고.
- `TradeStatMonthly`
  - `statMonth(YYYY-MM)`
  - `statKey = itemId` (가성비 1차 버전은 item 단위 추천)
  - `avgPrice`, `tradeCount`, `min/max(optional)`

#### 12.3 조회 시 조합(개념)
- 카테고리(속성값/속성깎/저항깎) 선택 → `ItemStat(statType=...)` 기준으로 아이템 목록 구성
- 각 아이템에 대해 전월 `TradeStatMonthly`를 조인
- 화면 표시:
  - `아이템명 / 능력치 값 / 전월 평균가 / 가성비 점수(능력치 ÷ 평균가)`
- 거래건수(`tradeCount`)가 낮으면 “표본 적음” 같은 신뢰도 힌트 제공 권장

> 상세 기획은 `docs/value-for-money-feature.ko.md` 참고.

---

## 13) 개정 사항 — 기획 검토 반영

> 기획 에이전트 보완 검토 결과를 반영한 추가/수정 사항.

### 13.1 신규 엔티티

#### `User` (사용자)
- **id**
- **oauthProvider**: `”google”` (MVP; Kakao는 추가기능)
- **oauthId**: 소셜 로그인 고유 ID
- **nickname**, **email**
- **role**: `USER` | `ADMIN`
- **status**: `ACTIVE` | `BLOCKED`
  - `BLOCKED`: 거래 불가 상태. 하드딜리트 없음, 영구 소프트 상태로 유지
  - `blockedUntil`(nullable): null=영구 차단, 미래 날짜=기간 차단 만료일
  - `blockReason`(nullable): 차단 사유
- **deletedAt**(nullable): null=활성, non-null=소프트 삭제. 1년 후 배치 하드딜리트 예정
- **createdAt**, **updatedAt**
- `UNIQUE(oauthProvider, oauthId)`

#### `TradeApplication` (거래 신청)
- **id**
- **listingId(FK → TradeListing)**
- **buyerId(FK → User)**
- **status**: `PENDING` | `ACCEPTED` | `REJECTED` | `CANCELLED`
- **message**(nullable): 구매자 메모
- **respondedAt**(nullable): 판매자 응답 시각
- **createdAt**, **updatedAt**

> 상태 전이 규칙:
> - `PENDING → ACCEPTED`: 판매자 수락 → `TradeListing.status = IN_TRADE`
> - `PENDING → REJECTED`: 판매자 거절
> - `PENDING|ACCEPTED → CANCELLED`: 구매자 취소 → 수락 상태였으면 `TradeListing.status = ACTIVE`로 복귀
> - `ACCEPTED` 상태의 신청이 확정되면 `TradeConfirmed` 생성 → `TradeListing.status = SOLD`

#### `Report` (신고)
- **id**
- **reporterId(FK → User)**
- **targetType**: `USER` | `TRADE_LISTING` | `TRADE_APPLICATION`
- **targetId**: 신고 대상 ID
- **reasonCategory**: `FRAUD` | `ABUSE` | `FAKE_LISTING` | `CASH_TRADE` | `OTHER`
- **description**: 상세 내용
- **evidenceUrl**(nullable): 증빙 스크린샷 URL
- **status**: `PENDING` | `PROCESSED` | `DISMISSED`
- **adminNote**(nullable): 처리자 메모
- **processedAt**(nullable)
- **createdAt**, **updatedAt**

### 13.2 `TradeConfirmed` 필드 확정
- **id**
- **listingId(FK, nullable)**: 리스팅 삭제/숨김 후에도 확정 기록 유지를 위해 nullable
- **applicationId(FK, nullable)**: 확정된 신청 참조
- **sellerId(FK → User, nullable)**: 스냅샷용 (User 삭제 후에도 기록 보존)
- **buyerId(FK → User, nullable)**
- **serverSnapshot**: 확정 당시 서버명 스냅샷
- **confirmedPrice**: 확정 당시 가격 스냅샷
- **statKeySnapshot**: 집계 키 스냅샷 (예: `ITEM:1`, `SET:2`)
- **confirmedAt**
- **cancelled**(boolean, 기본값 false): 취소 여부. 집계 재산출 시 제외 대상
- **cancelledAt**(nullable)

### 13.3 기존 엔티티 수정 사항

#### `TradeListing` 수정
- **hidden**(boolean, 기본값 false) 추가: 관리자 숨김. 판매자 취소(`CANCELLED`)와 구분
- **deletedAt**(nullable) 추가: 소프트 삭제 (1년 보관 후 배치 하드딜리트)
- **server**: 거상 서버명. 추후 Enum 전환 예정; 현재는 String

#### `BundleEquipmentDetail` 수정
`appearanceEnhanceLevel` + `normalHasRitual` → `enhanceLevel` + `hasRitual`로 통합. 최종 필드 구성:

| 필드 | 타입 | Null | 설명 |
|------|------|------|------|
| `bundleLineId` | Long (PK/FK) | NO | `BundleLine.id` 공유 (@MapsId) |
| `bundleLine` | BundleLine | NO | 1:1 연결 대상 |
| `equipmentItem` | EquipmentItem (FK) | NO | 정합성 체크 및 주술 필터링 용도 |
| `equipmentKindSnapshot` | EquipmentKind | NO | 등록 시점 스냅샷 (`APPEARANCE` \| `NORMAL`) |
| `enhanceLevel` | Integer | YES | 강화 수치. 외변 정책: 5만 유효 / 일반: 실제 수치 |
| `hasRitual` | boolean | NO | 주술 적용 여부 — true면 `BundleEquipmentRitual` 존재 |

#### `TradeStatDaily` / `TradeStatMonthly` 수정
- **quantitySum** 추가: 재료 거래 수량 합계. “개당 평균가 = priceSum / quantitySum” 계산용

### 13.4 유니크 제약 추가 (DB 무결성)
| 테이블 | 제약 |
|-------|------|
| `users` | `UNIQUE(oauthProvider, oauthId)` |
| `ritual_applicabilities` | `UNIQUE(ritualId, equipmentItemId)` |
| `equipment_set_pieces` | `UNIQUE(setId, slot)` |
| `bundle_equipment_rituals` | `UNIQUE(bundleLineId, ritualId)` |
| `item_stats` | `UNIQUE(itemId, statType, element)` — element는 NONE 상수값 사용(null 대신) |
| `trade_stat_daily` | `UNIQUE(statDate, statKey)` |
| `trade_stat_monthly` | `UNIQUE(statMonth, statKey)` |
| `value_metric_monthly` | `UNIQUE(month, itemId, statType, element)` |

### 13.5 statKey 형식 규약
| 단계 | 형식 | 예시 |
|------|------|------|
| MVP (1단계) | `ITEM:{itemId}` | `ITEM:42` |
| 확장 (2단계) | `ITEM:{itemId}:RITUAL:{mark}` | `ITEM:42:RITUAL:XX` |
| 확장 (3단계) | `SET:{setId}:RITUAL_COUNT:{n}:MARK:{mark}` | `SET:3:RITUAL_COUNT:5:MARK:XX` |

### 13.6 `ItemStat.element` Enum (NONE 포함)
- `FIRE` | `WATER` | `WIND` | `EARTH` | `LIGHTNING` | `NONE`
- `NONE`: 속성 구분 없는 능력치. null 대신 사용하여 유니크 제약 적용 가능하게 함
- 실제 속성 종류는 게임 정의에 따라 확정 필요

---

## 14) 크롤링 기반 신규 엔티티 — `gersang-items-crawling.md` 반영

> 크롤링 전략 전문은 `docs/gersang-items-crawling.md` 참고.

### 14.1 `Server` (거상 서버)

- **server_id** (PK, INT): 1~13 고정
- **name**: 서버명 (백호/주작/현무/청룡/봉황/해태/세종/신구/단군/비호/태극/화랑/태왕)
- **is_active**: boolean

> `TradeListing.server`(기존 String)의 추후 FK 전환 대상. 현재는 String 유지.

### 14.2 `Gem` (보석)

보석은 4단계 상태를 가지는 별개 엔티티로 `Item`과는 별도 관리한다.

- **gem_id** (PK)
- **name**: 기본 보석명 (흑요석/적혈석 등 11종)
- **gem_grade**: `기본` | `세공됨` | `강화됨` | `빛나는` | `주술됨`
- **ritual_id** (FK → Ritual, nullable): `gem_grade="주술됨"`일 때만 값 존재
- **image_url**
- `UNIQUE(name, gem_grade, ritual_id)`

### 14.3 `MaterialPriceHistory` (재료 월간 가격 집계)

geota.co.kr 크롤링 결과를 IQR 이상치 제거 후 집계하는 테이블. 가성비 계산기 기본값으로 사용된다.

- **id** (PK)
- **item_id** (FK → Item, MaterialItem 대상)
- **server_id** (FK → Server)
- **year_month** (CHAR 7, 예: `'2025-03'`)
- **avg_price**: 이상치 제거 후 평균가
- **min_price**: 이상치 제거 후 최저가
- **sample_count**: 집계에 사용된 거래 건수
- `UNIQUE(item_id, server_id, year_month)`

### 14.4 `BundleEquipmentGem` (장비에 박힌 보석)

기존 설계에서 누락된 엔티티. 장비 피스에 박힌 보석 정보를 저장한다.

- **id** (PK)
- **bundle_line_id** (FK → BundleLine)
- **gem_id** (FK → Gem)

### 14.5 `EquipmentItem` 추가 필드 (크롤링으로 보완)

| 필드 | 설명 |
|------|------|
| `ritualApplicable` | 주술 적용 가능 여부 |
| `hasSlotOption` | `<홈이있는>` 버전 존재 여부 |

> 두 필드 모두 gerniverse 상세 파싱 후 갱신. 기존 `RitualApplicability`로도 주술 가능 여부 판단 가능하나, 빠른 필터링을 위해 플래그 형태로도 보유.
