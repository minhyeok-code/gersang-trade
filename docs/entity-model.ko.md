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
- **name**: “지국천왕” 등 (정식 세트명)
- **totalPieces**: 세트 최대 피스 수 (예: 지국천왕=6, 브리트라=2)
- **isTradeable**: 현재 메타 세트 여부. false이면 거래 목록 노출 제외. 관리자 개별 토글. 크롤링 시 true로 저장

> `setType`은 제거. “3셋”/”5셋” 구분은 세트 자체 속성이 아니라 판매자가 몇 피스를 올리냐에 따라 결정되는 값이므로 Listing 단에서 계산.

#### 2.5 `EquipmentSetPiece` (세트 구성 정의)
- **id**
- **setId(FK → EquipmentSet)**
- **slot** (위 slot enum)
- **equipmentItemId(FK → EquipmentItem)**: 해당 슬롯의 아이템 정의
- **pieceCount**: 착용 개수. 기본 1. 반지(RING)는 2
- `UNIQUE(set_id, slot)`

> 이렇게 두면 “세트 5피스”를 표준화할 수 있고, Listing에서는 “세트를 판다”가 아니라 “세트 정의를 참고해 특정 피스들을 묶어 판다”를 표현할 수 있음.

#### 2.6 `EquipmentSetEffect` (세트 착용 수별 추가 능력치) — 신규
- **id**
- **equipmentSetId(FK → EquipmentSet)**
- **requiredPieces**: 몇 종 착용 시 발동하는지 (2, 3, 4, 5, 6)
- **statType**: `StatType` enum
- **statValue**: 수치
- **statUnit**: `StatUnit` enum — `FLAT` | `PERCENT`
- **element**: `Element` enum — `NONE`(모든 속성 공통) | `FIRE`/`EARTH` 등(특정 속성)
- `UNIQUE(equipment_set_id, required_pieces, stat_type, element)`

#### 2.7 `RitualSetEffect` (주술 세트 효과) — 신규
- **id**
- **ritualId(FK → Ritual)**
- **outcome**: `RitualOutcome` — `SUCCESS` | `GREAT_SUCCESS`. 천추 성공/북두칠성(대성공)을 같은 ritual_id로 구분
- **equipmentSetId(FK → EquipmentSet)**: 주술 가능 세트가 고정이므로 NOT NULL
- **requiredRitualPieces**: 몇 피스에 적용해야 발동하는지 (3 또는 5)
- **statType**: `StatType` enum
- **statValue**: 수치
- **statUnit**: `StatUnit` enum
- **element**: `Element` enum — `NONE` 또는 특정 속성
- `UNIQUE(ritual_id, outcome, equipment_set_id, required_ritual_pieces, stat_type)`

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
- **equipmentSetId(FK → EquipmentSet, nullable)**: `bundleType=EQUIPMENT_SET`일 때만 non-null. 세트 효과 계산 및 자동 명칭 생성에 사용
- **titleOverride(optional)**: 사용자가 임의로 제목을 덮어쓰는 경우(선택)

#### 4.3 `BundleLine` (번들 구성 라인: 여러 아이템+수량)
- **id**
- **bundleId(FK → ListingBundle)**
- **itemId(FK → Item)**: 재료/장비 모두 가능
- **equipmentSetPieceId(FK → EquipmentSetPiece, nullable)**: `EQUIPMENT_SET` 매물에서 이 라인이 어느 피스(투구/갑옷 등)인지 연결. 주술 세트 효과 계산에 필요. 단품·재료는 null
- **quantity**: 재료면 필수(>=1), 장비면 보통 1. 반지 쌍은 2
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
- `EquipmentSlot`: WEAPON, HELMET, ARMOR, GLOVES, BELT, SHOES, RING
- `RitualType`: WEAPON, ARMOR
- `RitualOutcome`: SUCCESS, GREAT_SUCCESS
- `ListingStatus`: ACTIVE, IN_TRADE, SOLD, CANCELLED
- `BundleType`: MATERIAL_BUNDLE, EQUIPMENT_SINGLE, EQUIPMENT_SET
- `StatUnit`: FLAT, PERCENT, LEVEL(예약)

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
  - `statType`: `StatType` enum. MVP: `ELEMENT_VALUE` | `ELEMENT_PIERCE` | `RESIST_PIERCE`
    - `ELEMENT_VALUE`: 아이템이 부여하는 **속성값(δ)**. 데미지 계산 공식 `n% = (3x-y)/2`의 x 구성요소로 사용됨
    - `ELEMENT_PIERCE`: 속성깎 (상대 속성값 감소)
    - `RESIST_PIERCE`: 저항깎 (상대 저항값 감소)
  - `element`: `Element` enum. 속성별 구분 시 사용. null 대신 `NONE`을 사용하여 UNIQUE 제약 정상 동작
  - `value`
  - `statUnit`: `StatUnit` enum — `FLAT` | `PERCENT`. UI 표시 및 계산 모호함 제거용 (예: 타저 25가 25% 인지 수치인지 구분)

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

> `gersang-grade-policy.md` 반영 — 등급·EXP·매너점수 필드 추가

- **id**
- **oauthProvider**: `”google”` (MVP; Kakao는 추가기능)
- **oauthId**: 소셜 로그인 고유 ID
- **nickname**, **email**
- **role**: `USER` | `ADMIN`
- **status**: `ACTIVE` | `BLOCKED`
  - `BLOCKED`: 거래 불가 상태. 하드딜리트 없음, 영구 소프트 상태로 유지
  - `blockedUntil`(nullable): null=영구 차단, 미래 날짜=기간 차단 만료일
  - `blockReason`(nullable): 차단 사유
- **grade**: `GradeLevel` Enum — `HAENGSANG(행상)` | `BOSANG(보상)` | `GAEKSANG(객상)` | `DAESANG(대상)` | `GEOSANG(거상)`. 기본값: `HAENGSANG`
- **gradeStep**: Integer — 현재 등급 내 호봉(1부터 시작). 거상은 null. 기본값: 1
- **totalExp**: Long — 누적 EXP. 기본값: 0. 등급·호봉 계산의 원천
- **mannerScore**: Integer — 매너점수. 기본값: **60**, 범위: 0~100
- **tradeCount**: Integer — 거래 확정 횟수. 기본값: 0. 프로필 공개 항목
- **deletedAt**(nullable): null=활성, non-null=소프트 삭제. 1년 후 배치 하드딜리트 예정
- **createdAt**, **updatedAt**
- `UNIQUE(oauthProvider, oauthId)`

> 프로필 공개 정보 (상대방 거래 전 신뢰도 확인용):
> ```
> 거래량    매너점수    등급
>  128건     73점    객상 4좌
> ```

#### `TradeApplication` (거래 신청) — **Deprecated**

> ⚠️ `trade-flow-design.ko.md` 반영 — `ChatRoom` 엔티티로 대체됨. 구현하지 않음.

#### `Report` (신고)

> `report-system.ko.md` 기준. 사용자 신고 + 자동 감지(SYSTEM) 통합.

- **id**
- **reporterType**: `USER` | `SYSTEM`
- **reporterId(FK → User, nullable)**: SYSTEM 감지 시 null
- **targetType**: `USER` | `TRADE_LISTING` | `WANTED_LISTING` | `CHAT_MESSAGE`
- **targetId**: 신고 대상 ID
- **chatRoomId(FK → ChatRoom, nullable)**: 채팅 관련 신고 시 참조
- **reasonCategory**: `FRAUD` | `ABUSE` | `FAKE_LISTING` | `CASH_TRADE` | `OTHER`
- **description**(nullable): 상세 내용. SYSTEM 감지는 자동 생성
- **evidenceUrl**(nullable): 증빙 스크린샷 URL
- **status**: `PENDING` | `REVIEWING` | `PROCESSED` | `DISMISSED`
- **adminNote**(nullable): 처리자 메모
- **processedBy(FK → User, nullable)**: 처리한 관리자
- **processedAt**(nullable)
- **createdAt**, **updatedAt**

### 13.2 `TradeConfirmed` 필드 확정

> `trade-flow-design.ko.md` 반영 — `applicationId` → `chatRoomId`로 교체

- **id**
- **listingType**: `SELL` | `BUY` 스냅샷
- **listingId(FK, nullable)**: 리스팅 삭제/숨김 후에도 확정 기록 유지를 위해 nullable
- **chatRoomId(FK → ChatRoom, nullable, UNIQUE)**: 확정된 채팅방 참조. UNIQUE로 이중 확정 방지
- **sellerId(FK → User, nullable)**: 게시자 스냅샷 (User 삭제 후에도 기록 보존)
- **buyerId(FK → User, nullable)**: 상대방 스냅샷
- **serverSnapshot**: 확정 당시 서버명 스냅샷
- **confirmedPrice**: 확정 당시 가격 스냅샷 (ChatRoom.finalPrice)
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
| `equipment_set_effects` | `UNIQUE(equipment_set_id, required_pieces, stat_type, element)` — element는 NONE 상수값 사용 |
| `ritual_set_effects` | `UNIQUE(ritual_id, outcome, equipment_set_id, required_ritual_pieces, stat_type)` |
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
- `FIRE` | `WATER` | `WIND` | `EARTH` | `THUNDER` | `ADAPTIVE` | `NONE`
- `ADAPTIVE`: 착용 용병의 속성을 따라감. 용병 속성이 FIRE면 화속성값 +n으로 적용
- `NONE`: 속성 구분 없는 능력치 (힘, 방어 등). null 대신 사용하여 UNIQUE 제약 정상 동작

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

---

### 14.6 `Mercenary` / `MercenaryStat` / `MercenaryMaterial` — `docs/mercenary.md` 반영

> 크롤링 전략 및 엔티티 상세는 `back/gersangtrade/docs/mercenary.md` 참고.
> 모든 PK는 Long Auto Increment 사용.

#### `Mercenary` (용병 마스터)

| 필드 | 타입 | Null | 설명 |
|------|------|------|------|
| `id` | Long (PK, AI) | NO | |
| `name` | String (unique) | NO | 용병 풀네임. 예: "각성 군다리명왕" |
| `key` | String (unique) | YES | gerniverse 내부 키. 상세 파싱 후 채워진다 |
| `category` | `MercenaryCategory` Enum | YES | 주인공/사천왕/각성사천왕/명왕/각성명왕/전설장수/신수/흉수/각성흉수/고용몬스터/전직몬스터/정령몬스터/각성장수/개조장수/2차장수/1차장수/용병 |
| `nation` | `Nation` Enum | YES | 조선/중국/일본/대만/인도/NONE |
| `nature` | `Nature` Enum | YES | 화/수/뇌/풍/토/NONE. 무속성은 NONE |
| `nature_value` | Integer | YES | 속성값. 공식 `n% = (3x - y) / 2`의 x 구성 요소. 무속성은 null |
| `is_coming_soon` | boolean | NO | 출시 예정 여부. true이면 크롤링 대상 제외 |
| `image_url` | String | YES | S3 업로드 URL |
| `crawled_at` | LocalDateTime | YES | null이면 상세 크롤링 미완료. `MercenaryDetailReader`의 처리 대상 판별 기준 |
| `created_at`, `updated_at` | LocalDateTime | NO | BaseEntity |

> `nature_value`는 `MercenaryStat(ELEMENT_VALUE)`에도 저장된다. Mercenary 직접 필드는 계산기 빠른 접근용 역정규화.

#### `MercenaryStat` (용병 스탯)

`StatType`은 `ItemStat`과 공유한다.
MVP 필수: `ELEMENT_VALUE` / `ELEMENT_PIERCE` / `RESIST_PIERCE`
Phase 2 확장: `STRENGTH` / `VITALITY` / `DEXTERITY` / `INTELLECT` / `DEFENSE` / `SIGHT` / `HIT_RATE` / `CRITICAL_CHANCE` / `MIN_POWER` / `MAX_POWER` / `MAGIC_RESISTANCE` / `HITTING_RESISTANCE`

| 필드 | 타입 | Null | 설명 |
|------|------|------|------|
| `id` | Long (PK, AI) | NO | |
| `mercenary_id` | FK → Mercenary | NO | |
| `stat_key` | `StatType` Enum | NO | |
| `stat_value` | Integer | NO | |

- `UNIQUE(mercenary_id, stat_key)`
- 크롤링 재실행 시 delete-reinsert 패턴으로 재적재

#### `MercenaryMaterial` (전직 재료)

재료는 **아이템** 또는 **용병** 중 하나다. 두 필드가 동시에 설정되는 경우는 없다.

| 필드 | 타입 | Null | 설명 |
|------|------|------|------|
| `id` | Long (PK, AI) | NO | |
| `result_mercenary_id` | FK → Mercenary | NO | 완성되는 용병 |
| `material_mercenary_id` | FK → Mercenary | YES | 재료 용병. 아이템 재료이면 null |
| `material_item_key` | String | YES | 재료 아이템명(gerniverse URL 디코딩). 용병 재료이면 null |
| `quantity` | Integer | NO | |
| `required_level` | Integer | YES | 전직 요구 레벨 |
| `required_credit` | Integer | YES | 전직 요구 공헌도 |

- 중복 방지: delete-reinsert 패턴(`deleteByResultMercenaryId` 후 재삽입)
- `material_item_key`를 통해 `MaterialPriceHistory`(아이템명 기준)와 비용 계산 연결

#### 관련 Enum 신규 추가

| Enum | 값 |
|------|-----|
| `MercenaryCategory` | PROTAGONIST / FOUR_HEAVENLY_KINGS / FOUR_HEAVENLY_KINGS_AWAKENING / MYEONG_KING / MYEONG_KING_AWAKENING / LEGENDARY_GENERAL / DIVINE_BEAST / EVIL_BEAST / EVIL_BEAST_AWAKENING / HIRED_MONSTER / EVOLVE_MONSTER / SPIRIT_MONSTER / GENERAL_AWAKENING / MODIFIED_GENERAL / SECOND_GRADE_GENERAL / FIRST_GRADE_GENERAL / MERCENARY |
| `Nation` | JOSEON / CHINA / JAPAN / TAIWAN / INDIA / NONE |
| `Nature` | FIRE / WATER / THUNDER / AIR / EARTH / NONE |

#### `StatType` Enum 확장

`ItemStat`, `MercenaryStat`, `EquipmentSetEffect`, `RitualSetEffect`에서 공유한다.

| 그룹 | 값 | 비고 |
|------|-----|------|
| 아이템·용병 공통 | `ELEMENT_VALUE` / `ELEMENT_PIERCE` / `RESIST_PIERCE` | MVP 기준 |
| 공격력 공통 | `MIN_POWER` / `MAX_POWER` | |
| 기본 스탯 (용병·세트효과·주술세트효과 공통) | `STRENGTH` / `VITALITY` / `DEXTERITY` / `INTELLECT` / `DEFENSE` | |
| 용병 전용 | `SIGHT` / `HIT_RATE` / `CRITICAL_CHANCE` | |
| 저항 (용병·세트효과 공통) | `MAGIC_RESISTANCE` / `HITTING_RESISTANCE` | |
| 세트효과·주술세트효과 전용 | `DAMAGE_PERCENT` / `SKILL_DAMAGE_PERCENT` / `FIELD_MOVE_SPEED` | 신규 |

---

## 15) 등급·EXP·매너점수 시스템 엔티티 — `gersang-grade-policy.md` 반영

> 상세 정책은 `docs/gersang-grade-policy.md` 참고.

### 15.1 Grade 관련 Enum

#### `GradeLevel` (등급)

| Enum 값 | 표시명 | 등급 | 단위 | 최대 호봉 |
|---------|--------|------|------|---------|
| `HAENGSANG` | 행상 (行商) | 5등급 | 패 | 3 |
| `BOSANG` | 보상 (褓商) | 4등급 | 패 | 5 |
| `GAEKSANG` | 객상 (客商) | 3등급 | 좌 | 7 |
| `DAESANG` | 대상 (大商) | 2등급 | 방 | 10 |
| `GEOSANG` | 거상 (巨商) | 1등급 | — | — (호봉 없음) |

#### EXP 임계값 (누적 기준)

| 등급·단계 | 누적 EXP 시작점 |
|---------|--------------|
| 행상 1패 | 0 |
| 보상 1패 | 150 |
| 객상 1좌 | 900 |
| 대상 1방 | 3,700 |
| 거상 진입 | 20,000 |

> `User.totalExp`에서 `GradeLevel`과 `gradeStep`을 역산하는 로직은 `ExpGradeCalculator` 유틸에서 담당한다.
> EXP 지급 시 totalExp를 갱신하고 grade·gradeStep을 즉시 재계산하여 User에 반영한다.

---

### 15.2 `TradeReview` (거래 평가)

거래 확정 후 양측이 서로를 평가하는 테이블. **블라인드 방식** — 3일 만료 시점에 일괄 공개.

| 필드 | 타입 | Null | 설명 |
|------|------|------|------|
| `id` | Long (PK) | NO | |
| `tradeConfirmedId` | Long (FK → TradeConfirmed) | NO | |
| `reviewerId` | Long (FK → User) | NO | 평가자 |
| `targetId` | Long (FK → User) | NO | 평가 대상 |
| `rating` | Enum | YES | `GOOD` \| `NEUTRAL` \| `BAD`. null = 미제출 |
| `revealAt` | LocalDateTime | NO | `confirmedAt + 3일`. 이 시각 이후 공개 |
| `isPublished` | boolean | NO | 기본 false. 배치 Job이 `revealAt` 경과 후 true로 전환 |
| `submittedAt` | LocalDateTime | YES | 평가 제출 시각. null = 미제출 |
| `createdAt` | LocalDateTime | NO | |

**유니크 제약:**
```sql
UNIQUE(tradeConfirmedId, reviewerId)
-- 거래 확정 1건당 평가자 1명이 1개의 평가만 제출 가능
```

**생성 시점:** 거래 확정 트랜잭션 내에서 **양측에 대해 각 1건씩 총 2건** 자동 생성.
```
TradeReview(reviewer=poster,      target=counterparty, revealAt=now+3일)
TradeReview(reviewer=counterparty, target=poster,       revealAt=now+3일)
```

**`rating` 효과:**

| 평가 | EXP 효과 | 매너점수 효과 |
|------|---------|------------|
| `GOOD` (👍 좋음) | +15 EXP | +2점 |
| `NEUTRAL` (😐 보통) | 0 EXP | 0점 |
| `BAD` (👎 나쁨) | −20 EXP | −3점 |

> 미제출(null)은 효과 없음. 평가 효과는 `isPublished` 전환 시 일괄 반영 (배치 Job).

---

### 15.3 신규 엔티티 추가에 따른 유니크 제약

| 테이블 | 제약 |
|-------|------|
| `trade_reviews` | `UNIQUE(trade_confirmed_id, reviewer_id)` |
| `trade_confirmed` | `UNIQUE(chat_room_id)` — 이중 확정 방지 |

---

### 15.4 채팅 관련 신규 엔티티 (trade-flow-design.ko.md 반영)

#### `ChatRoom`

| 필드 | 타입 | Null | 설명 |
|------|------|------|------|
| `id` | Long (PK) | NO | |
| `listingType` | Enum | NO | `SELL` \| `BUY` |
| `listingId` | Long | NO | TradeListing.id 또는 WantedListing.id |
| `initiationType` | Enum | NO | `NEGOTIATE` (흥정하기) \| `APPLY` (거래신청) |
| `posterId` | Long (FK → User) | NO | 게시물 작성자 |
| `counterpartyId` | Long (FK → User) | NO | 채팅 개설자 |
| `status` | Enum | NO | `OPEN` \| `POSTER_CONFIRMED` \| `COMPLETED` \| `CLOSED` |
| `finalPrice` | Long | YES | 실제 거래가. 미설정 시 listing.price 사용 |
| `posterConfirmedAt` | LocalDateTime | YES | |
| `counterpartyConfirmedAt` | LocalDateTime | YES | |
| `completedAt` | LocalDateTime | YES | |
| `createdAt` | LocalDateTime | NO | |
| `updatedAt` | LocalDateTime | NO | |

**유니크 제약:** `UNIQUE(listing_type, listing_id, counterparty_id, status)` — OPEN 상태 중복 방지

#### `ChatMessage`

| 필드 | 타입 | Null | 설명 |
|------|------|------|------|
| `id` | Long (PK) | NO | |
| `chatRoomId` | Long (FK → ChatRoom) | NO | |
| `senderId` | Long (FK → User) | YES | SYSTEM 메시지는 null |
| `content` | String (최대 1000자) | NO | |
| `messageType` | Enum | NO | `TEXT` \| `SYSTEM` |
| `flagged` | boolean | NO | 기본 false. 자동 감지 시 true |
| `flagReason` | String | YES | 감지된 패턴 목록 |
| `hidden` | boolean | NO | 기본 false. 관리자 숨김 처리 시 true |
| `archivedAt` | LocalDateTime | YES | null=사용자 열람 가능 / non-null=6개월 경과 숨김 |
| `sentAt` | LocalDateTime | NO | |

#### `Notification`

| 필드 | 타입 | Null | 설명 |
|------|------|------|------|
| `id` | Long (PK) | NO | |
| `userId` | Long (FK → User) | NO | 수신자 |
| `type` | Enum | NO | `CHAT_OPENED` \| `CHAT_MESSAGE` \| `POSTER_CONFIRMED` \| `TRADE_COMPLETED` \| `REVIEW_REQUESTED` \| `REVIEW_PUBLISHED` \| `CASH_TRADE_DETECTED` \| `REPORT_RECEIVED` \| `REPORT_PROCESSED` \| `USER_WARNED` \| `USER_BLOCKED` |
| `chatRoomId` | Long (FK → ChatRoom) | YES | |
| `message` | String | NO | 알림 문구 |
| `isRead` | boolean | NO | 기본 false |
| `createdAt` | LocalDateTime | NO | |

---

## 16) 용병 특성·유저 덱 엔티티 — `Mercenary-characteristic-crawling.md` 및 설계 논의 반영

> 작성일: 2026-04-22

### 16.1 카탈로그 레이어 — 신규 엔티티

#### `MercenaryCharacteristic` (용병 특성)

각성 사천왕·명왕·주인공·전설장수의 특성 트리 노드. 전설장수 패시브도 이 엔티티로 통합 관리 (gerniverse RSC payload가 동일 구조로 제공).

| 필드 | 타입 | Null | 설명 |
|------|------|------|------|
| `id` | Long (PK, AI) | NO | |
| `mercenary_id` | FK → Mercenary | NO | |
| `characteristic_key` | String (UNIQUE) | NO | gerniverse 내부 키. 크롤링 UPSERT 기준 |
| `name` | String | NO | 특성명. 예: "광풍", "기습" |
| `point` | Integer | YES | 포인트 비용. 각성 특성은 null |
| `description` | String | YES | |
| `required_characteristic_key` | String | YES | 선행 특성 키(FK 대신 String). null이면 루트 노드 |

- 크롤링 재실행 시 delete-reinsert 패턴
- `UNIQUE(characteristic_key)`

#### `MercenaryCharacteristicLevel` (특성 레벨별 수치)

한 특성에 label × level 조합으로 행이 생성된다. 각성 특성(`point: null`)은 행 미생성.

| 필드 | 타입 | Null | 설명 |
|------|------|------|------|
| `id` | Long (PK, AI) | NO | |
| `characteristic_id` | FK → MercenaryCharacteristic | NO | |
| `label` | String | NO | 수치 항목명. 예: "풍극진멸 데미지", "타격저항력" (원본 텍스트) |
| `level` | Integer | NO | 1부터 시작. 각성 사천왕·명왕·주인공 max 5, 전설장수 max 10 |
| `amount` | String | NO | 원본 수치 문자열. 예: "20%", "500" |
| `amount_value` | Float | YES | 파싱된 Float 수치. "20%" → 20.0. 파싱 불가 시 null |
| `stat_type` | StatType (Enum) | YES | label → StatType 자동 매핑. 미매핑은 null(관리자 수동 보정 대상) |

- `UNIQUE(characteristic_id, label, level)`
- `stat_type` 매핑 규칙: `"저항깎"/"저항감소"` 포함 → `RESIST_PIERCE`, `"속성값"` 포함 → `ELEMENT_VALUE`, 그 외 → `null`

#### 비즈니스 규칙 (검증용)

| 용병 종류 | 특성 수 | 최대 레벨 |
|---------|--------|---------|
| 각성 사천왕 / 각성 명왕 / 주인공 | 4개 + 각성 특성 1개 | 5 |
| 전설장수 | 2개 | 10 |

---

### 16.2 유저 덱 레이어 — 신규 엔티티

#### `UserDeck` (유저 용병 덱)

| 필드 | 타입 | Null | 설명 |
|------|------|------|------|
| `id` | Long (PK, AI) | NO | |
| `user_id` | FK → User | NO | |
| `is_active` | boolean | NO | 유저당 최대 1개 true |
| `attr_x_value` | Integer | YES | 저장 시점 속성값 합산 캐시 |
| `total_res_down` | Integer | YES | 저장 시점 저항깎 합산 캐시 |
| `created_at` | LocalDateTime | NO | 불변 스냅샷. updatedAt 없음 |

- 덱은 불변 스냅샷. 수정 시 새 행 생성 후 `is_active` 전환

#### `UserDeckMember` (덱 내 용병 슬롯)

| 필드 | 타입 | Null | 설명 |
|------|------|------|------|
| `id` | Long (PK, AI) | NO | |
| `deck_id` | FK → UserDeck | NO | |
| `mercenary_id` | FK → Mercenary | NO | 주인공(PROTAGONIST)도 Mercenary로 통일 |
| `slot_index` | Integer | NO | 0~11. 덱 안에서 고유 |

- `UNIQUE(deck_id, slot_index)`

#### `UserDeckMemberEquip` (슬롯 용병 장비 상세)

카테고리별로 사용 필드가 다르다.

| 필드 | 타입 | Null | 설명 |
|------|------|------|------|
| `id` | Long (PK, AI) | NO | |
| `deck_member_id` | FK → UserDeckMember (UNIQUE) | NO | 1:1 |
| `equipment_set_id` | FK → EquipmentSet | YES | LEGENDARY_GENERAL 전용 |
| `enhance_level` | Integer | YES | LEGENDARY_GENERAL 세트 강화 수치 |
| `set_piece_count` | Integer | YES | LEGENDARY_GENERAL 세트 피스 수 |
| `has_affinity` | boolean | NO | LEGENDARY_GENERAL 인연 여부 |
| `equipment_item_id` | FK → EquipmentItem | YES | MYEONG_KING / MYEONG_KING_AWAKENING 전용 |

| 카테고리 | Equip 행 생성 | 사용 필드 |
|---------|------------|---------|
| PROTAGONIST / FOUR_HEAVENLY_KINGS(_AWAKENING) | ❌ | — |
| MYEONG_KING / MYEONG_KING_AWAKENING | ✅ | `equipment_item_id` |
| LEGENDARY_GENERAL | ✅ | `equipment_set_id`, `enhance_level`, `set_piece_count`, `has_affinity` |
| 그 외 | ❌ | — |

#### `UserDeckMemberCharacteristic` (선택된 특성)

전설장수 패시브도 `MercenaryCharacteristic`으로 통합되어 이 엔티티로 관리된다.

| 필드 | 타입 | Null | 설명 |
|------|------|------|------|
| `id` | Long (PK, AI) | NO | |
| `deck_member_id` | FK → UserDeckMember | NO | |
| `characteristic_id` | FK → MercenaryCharacteristic | NO | |
| `selected_level` | Integer | NO | 선택한 레벨. 각성 사천왕·명왕·주인공 1~5, 전설장수 1~10 |

- `UNIQUE(deck_member_id, characteristic_id)`

---

### 16.3 합산 계산 흐름 (UserDeckService.calculateTotalStats)

```
for each UserDeckMember:
  1. MercenaryStat → RESIST_PIERCE, ELEMENT_VALUE 기본값 합산
  2. UserDeckMemberCharacteristic × selectedLevel
       → MercenaryCharacteristicLevel 조회
       → statType == RESIST_PIERCE 이면 totalResDown += amountValue
       → statType == ELEMENT_VALUE 이면 attrXValue += amountValue
       → statType == null 이면 skip (미매핑, 관리자 보정 전)

→ 최종 attrXValue, totalResDown → UserDeck.applyStats() 캐싱
```
