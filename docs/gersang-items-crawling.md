# 크롤링 전략 및 엔티티 설계

## 개요

거상 아이템 거래 사이트의 가성비 계산 기능 및 거래 기능을 위해 아래 두 가지 데이터를 수집한다.

- **마스터 데이터**: 아이템, 용병, 재료 기본 정보 (최초 1회 + 업데이트 시)
- **가격 데이터**: 서버별 재료 실거래가 (월 1회 배치)

---

## 크롤링 소스 확정

### 소스 1 — geota.co.kr (메인, 안전)

- robots.txt: `User-agent: * / Allow: /` → 일반 봇 전체 허용
- 별도 API 없이 **정적 HTML**로 데이터 제공 → Jsoup 파싱 가능
- 아이템 전체 목록, 용병 전체 목록, 서버별 가격 모두 정적 HTML에 포함
- URL 패턴: `keyword` 파라미터로 아이템 검색, `serverId` 파라미터로 서버 구분

### 소스 2 — gerniverse.app (마스터 데이터 상세 정보)

- robots.txt: `User-agent: * / Allow: /` → 일반 봇 전체 허용
- **정적 HTML** (Next.js SSR) → Jsoup 파싱 가능
- 아이템/용병 상세 페이지에서 능력치, 재료, 이미지 경로 수집
- JSON-LD 스키마(`<script type="application/ld+json">`)에 구조화된 데이터 포함

### 소스 3 — gersanginfo.com (참고, 사용 보류)

- 내부 REST API 존재: `/api/game/market/new/items`, `/api/game/market/history`
- 외부 직접 호출 시 `{"message":"외부 접근 차단됨"}` 응답
- Referer 헤더 우회 필요 → 운영자 의도적 차단으로 판단, geota로 대체

---

## 수집 대상 및 URL 패턴

### 아이템 전체 목록

```
URL: https://geota.co.kr/gersang/calculator/item?serverId=1
방식: 정적 HTML 파싱 (li.cursor-pointer 태그 목록)
특징: 전체 아이템명이 한 페이지에 렌더링됨
수집 항목: 아이템명 raw string (파싱 후 분류)
```

### 용병 전체 목록

```
URL: https://geota.co.kr/gersang/calculator/mercenary?serverId=1
방식: 정적 HTML 파싱
특징: 전체 용병명 + 제작 재료 + 재료별 서버 가격이 한 페이지에 렌더링됨
수집 항목: 용병명, 제작 재료명, 재료 수량, 재료 단가(서버별)
```

### 아이템 상세 정보

```
URL: https://gerniverse.app/item/{아이템명(한글)}
방식: 정적 HTML 파싱 + JSON-LD 파싱
수집 항목:
  - 아이템명 (h1 태그)
  - 카테고리 대/소분류 (badge span 태그)
  - 상점 판매가
  - 능력치 (공격력, 생명력, 지력 등)
  - 요구 레벨
  - 제작 비용
  - 필요 재료 (재료명 + 수량) → a[href^=/item/] 링크에서 파싱
  - 이미지 경로 → JSON-LD image 필드: "item/weapon/doll/{영문키}"
```

### 용병 상세 정보

```
URL: https://gerniverse.app/mercenary/{용병명(한글)}
방식: 정적 HTML 파싱 + JSON-LD 파싱
수집 항목:
  - 용병명 (h1 태그)
  - 종류 (각성명왕, 사천왕 등)
  - 저항깎 수치 → JSON-LD additionalProperty에서 파싱
    예: {"name":"마법 저항","value":"100%"} → 디버프 수치로 변환
  - 속성값
  - 고용 재료 (재료명 + 수량)
  - 이미지 경로 → JSON-LD image 필드: "thumbnail/{경로}/{영문키}"
```

### 가격 데이터 (서버별 실거래가)

```
URL: https://geota.co.kr/gersang/yukeuijeon?serverId={1~13}
방식: 정적 HTML 파싱
수집 항목: 아이템명, 수량, 단가, 거래 시각
특징:
  - 서버별 분리 (serverId 파라미터)
  - 페이지네이션 존재 → 빈 데이터 감지 시 종료
  - 전체/실시간/최신순/가격순 정렬 지원 → 최신순으로 수집
```

---

## 아이템명 파싱 및 분류 전략

geota 아이템 목록의 raw 아이템명을 아래 순서로 분류한다.

### 보석 고정 목록 (11종)

```java
GEM_NAMES = ["흑요석", "적혈석", "사금석", "백수정", "월장석",
             "적마노", "남옥", "석웅황", "벽옥", "청수석", "녹림석"]
```

### 구 데이터 필터링 (skip 처리)

강화 단계 시스템은 업데이트로 삭제되었으나 geota에 구 데이터가 잔존한다.
괄호+숫자 패턴이 포함된 아이템은 파싱 시 skip한다.

```
정규식: \(\+\d+\)  → 해당 패턴 포함 시 skip
예: "강화된 흑요석(+1)", "강화된 흑요석(+5)", "<강인한> 강화된 흑요석(+5)" → 전부 skip
```

### Step 1 — 보석 여부 판별

GEM_NAMES 중 하나가 아이템명에 포함되면 → `Gem` 분류

```
"흑요석"              → gem.name="흑요석", gemGrade="기본"
"세공된 흑요석"        → gem.name="흑요석", gemGrade="세공됨"
"강화된 흑요석"        → gem.name="흑요석", gemGrade="강화됨"
"<태산북두> 흑요석"    → gem.name="흑요석", gemGrade="주술됨", ritual=태산북두
```

### Step 2 — 보석이 아닌 경우 Item 분류

정규식: `^<(.+?)>\s*(.+)$`

```
"챠우인형"                        → EquipmentItem, ritual=null, hasSlot=false
"<홈이있는> 챠우인형"              → EquipmentItem, ritual=null, hasSlot=true
"<태산북두> 챠우인형"              → EquipmentItem, ritual=태산북두, hasSlot=false
"홈이있는 <태산북두> 챠우인형"     → EquipmentItem, ritual=태산북두, hasSlot=true
"호선의인형 - 7일"                → EquipmentItem (기간제, 별도 처리 정책 필요)
"각성석", "힘의기억" 등            → MaterialItem
```

### 전체 분류표

| geota 아이템명 | 분류 | 처리 방식 |
|---|---|---|
| `강화된 흑요석(+1)` ~ `(+5)` | **skip** | 구 데이터, 제외 |
| `흑요석` | Gem | gemGrade=기본 |
| `세공된 흑요석` | Gem | gemGrade=세공됨 |
| `강화된 흑요석` | Gem | gemGrade=강화됨 |
| `빛나는 흑요석` | Gem | gemGrade=빛나는 (별도 제작 루트) |
| `<태산북두> 흑요석` | Gem | gemGrade=주술됨, ritual_id 할당 |
| `챠우인형` | EquipmentItem | ritual=null, hasSlot=false |
| `<홈이있는> 챠우인형` | EquipmentItem | ritual=null, hasSlot=true |
| `<태산북두> 챠우인형` | EquipmentItem | ritual=태산북두, hasSlot=false |
| `홈이있는 <태산북두> 챠우인형` | EquipmentItem | ritual=태산북두, hasSlot=true |
| `각성석`, `힘의기억` 등 | MaterialItem | 재료 아이템 |

---

## 이미지 수집 및 S3 저장

### 이미지 URL 패턴

```
아이템 이미지:
  원본: https://images.gerniverse.app/tr:cm-pad_resize,w-120,h-120,f-auto,q-80/{imageKey}.webp
  imageKey 예시: item/weapon/doll/gkstkdwkdmldlsgud
  JSON-LD image 필드에서 추출

용병 이미지:
  원본: https://images.gerniverse.app/tr:cm-pad_resize,w-120,h-120,f-auto,q-80/{imageKey}.webp
  imageKey 예시: thumbnail/myeong-kings/awakening/gakGoondari
  JSON-LD image 필드에서 추출
```

### S3 저장 경로

```
아이템: s3://버킷명/items/{imageKey}.webp
용병:   s3://버킷명/mercenaries/{imageKey}.webp
```

### 저장 흐름

```
gerniverse 상세 페이지 파싱
    ↓
JSON-LD에서 imageKey 추출
    ↓
images.gerniverse.app/{imageKey}.webp 다운로드 (Jsoup ignoreContentType)
    ↓
S3 업로드 (AWS SDK v2 PutObjectRequest)
    ↓
S3 URL → item.imageUrl 또는 mercenary.imageUrl 컬럼 저장
```

---

## 가격 이상치 처리 전략

수집된 거래 단가를 아이템별로 집계할 때 IQR 방식으로 이상치를 제거한다.

```
1. 서버별 아이템 거래 단가 목록 수집
2. 오름차순 정렬
3. Q1 (25번째 백분위수), Q3 (75번째 백분위수) 계산
4. IQR = Q3 - Q1
5. 유효 범위: [Q1 - 1.5×IQR, Q3 + 1.5×IQR]
6. 유효 범위 내 거래만 사용
7. 유효 거래의 평균가(avgPrice), 최저가(minPrice) 저장
8. 집계에 사용된 거래 건수(sampleCount) 함께 저장
9. 최소 샘플 수 미달 시 (권장: 5건 미만) → 해당 서버/아이템 조합 skip
```

### avgPrice vs minPrice 활용

- `avgPrice`: 가성비 계산기 기본값 → 일반적인 시세 기준
- `minPrice`: 가성비 계산기 최적 옵션 → 최저가 구매 시 기준
- 유저가 직접 가격 수정 시 `priceOverride` 값을 우선 사용

---

## 엔티티 설계

기존 프로젝트에 이미 구현된 엔티티와의 관계를 명시한다.

### 레이어 구분

```
Catalog  — 아이템/세트/주술의 정의 데이터 (마스터)
Listing  — 사용자 거래 등록 데이터
Trade    — 거래 신청/확정 및 집계
```

---

### [기존 구현] Ritual (주술 정의)

```
테이블: rituals
주요 필드:
  - id
  - displayName     : 주술 명칭 (예: "태산북두")
  - ritualType      : WEAPON | ARMOR
  - successMark     : 성공 시 표기 마크
  - greatSuccessMark: 대성공 시 표기 마크
```

### [기존 구현] RitualApplicability (주술 적용 가능 대상 매핑)

```
테이블: ritual_applicabilities
주요 필드:
  - id
  - ritual          : FK → Ritual
  - equipmentItem   : FK → EquipmentItem
UNIQUE: (ritual_id, equipment_item_id)
```

### [기존 구현] EquipmentSet (장비 세트)

```
테이블: equipment_sets
주요 필드:
  - id
  - name    : 세트명 (예: "챠우세트")
  - setType : ARMOR_5PIECE 등
```

### [기존 구현] EquipmentSetPiece (세트 구성 피스 정의)

```
테이블: equipment_set_pieces
주요 필드:
  - id
  - set             : FK → EquipmentSet
  - slot            : HELMET | ARMOR | GLOVES | BELT | SHOES 등
  - equipmentItem   : FK → EquipmentItem
UNIQUE: (set_id, slot)
```

### [기존 구현] EquipmentItem (장비 아이템 — 피스 단위)

```
테이블: equipment_items (Item 1:1 확장)
주요 필드:
  - itemId (PK/FK → Item)
  - equipmentKind : APPEARANCE | NORMAL
  - slot          : WEAPON | HELMET | ARMOR | GLOVES | BELT | SHOES
  - set           : FK → EquipmentSet (nullable, 세트 미소속이면 null)
크롤링 추가 필드 (기존 없을 경우):
  - ritualApplicable : boolean (주술 가능 여부)
  - hasSlotOption    : boolean (홈이있는 버전 존재 여부)
```

### [기존 구현] Item (아이템 공통)

```
테이블: items
주요 필드:
  - id
  - name          : 표준 아이템명
  - type          : MATERIAL | EQUIPMENT
  - tradeCategory : 분류 확장용
```

### [기존 구현] MaterialItem (재료 아이템)

```
테이블: material_items (Item 1:1 확장)
주요 필드:
  - itemId (PK/FK → Item)
  - stackUnitName : "개/묶음" 등 표시용
```

---

### [크롤링 추가] Server (서버)

```sql
CREATE TABLE servers (
    server_id   INT PRIMARY KEY,
    name        VARCHAR(20) NOT NULL,
    is_active   BOOLEAN DEFAULT TRUE
);

-- 초기 데이터 (13개 서버 고정)
INSERT INTO servers VALUES
(1, '백호', true), (2, '주작', true), (3, '현무', true),
(4, '청룡', true), (5, '봉황', true), (6, '해태', true),
(7, '세종', true), (8, '신구', true), (9, '단군', true),
(10, '비호', true), (11, '태극', true), (12, '화랑', true),
(13, '태왕', true);
```

### [크롤링 추가] Gem (보석)

보석은 4단계 상태를 가진 별개 아이템으로 관리한다.

```
[{보석명}] → 세공 → [세공된 {보석명}] → 강화 → [강화된 {보석명}] → 주술 → [<주술명> {보석명}]
예: 흑요석 → 세공된 흑요석 → 강화된 흑요석 → <태산북두> 흑요석
빛나는 흑요석: 별도 제작 루트 (시간의가루 + 흑요석 조각)
```

```sql
CREATE TABLE gems (
    gem_id      BIGINT PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL,   -- 기본 보석명 (흑요석, 적혈석 등 11종)
    gem_grade   VARCHAR(20) NOT NULL,    -- 기본 | 세공됨 | 강화됨 | 빛나는 | 주술됨
    ritual_id   BIGINT NULL,             -- gem_grade="주술됨"일 때만 값 존재
    image_url   VARCHAR(500),
    created_at  DATETIME DEFAULT NOW(),
    UNIQUE KEY uq_gem (name, gem_grade, ritual_id),
    FOREIGN KEY (ritual_id) REFERENCES rituals(id)
);

-- 초기 데이터 (기본 보석 11종)
INSERT INTO gems (name, gem_grade) VALUES
('흑요석', '기본'), ('적혈석', '기본'), ('사금석', '기본'),
('백수정', '기본'), ('월장석', '기본'), ('적마노', '기본'),
('남옥', '기본'), ('석웅황', '기본'), ('벽옥', '기본'),
('청수석', '기본'), ('녹림석', '기본');
```

### [크롤링 추가] MaterialPriceHistory (재료 가격 집계)

```sql
CREATE TABLE material_price_history (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    item_id         BIGINT NOT NULL,           -- MaterialItem FK
    server_id       INT NOT NULL,
    year_month      CHAR(7) NOT NULL,          -- '2025-03' 형식
    avg_price       BIGINT NOT NULL,           -- 이상치 제거 후 평균가
    min_price       BIGINT NOT NULL,           -- 이상치 제거 후 최저가
    sample_count    INT DEFAULT 0,             -- 집계에 사용된 거래 건수
    crawled_at      DATETIME DEFAULT NOW(),
    UNIQUE KEY uq_price_history (item_id, server_id, year_month),
    FOREIGN KEY (item_id) REFERENCES items(id),
    FOREIGN KEY (server_id) REFERENCES servers(server_id)
);
```

---

### [Listing 레이어 — 기존 설계 기반] TradeListing (거래 게시글)

```
테이블: trade_listings
주요 필드:
  - id
  - seller          : FK → User
  - server          : FK → Server (서버 필수)
  - status          : ACTIVE | IN_TRADE | SOLD | CANCELLED
  - price           : 총 가격 (게임 내 재화)
  - note            : 연락/메모
  - hidden          : boolean (관리자 숨김)
  - deletedAt       : nullable (소프트 삭제)
  - createdAt, updatedAt
```

### [Listing 레이어 — 기존 설계 기반] ListingBundle (판매 단위)

```
테이블: listing_bundles
주요 필드:
  - id
  - listing         : FK → TradeListing
  - bundleType      : MATERIAL_BUNDLE | EQUIPMENT_SINGLE | EQUIPMENT_SET
  - titleOverride   : nullable (사용자 임의 제목)
```

### [Listing 레이어 — 기존 설계 기반] BundleLine (번들 구성 라인)

```
테이블: bundle_lines
주요 필드:
  - id
  - bundle          : FK → ListingBundle
  - item            : FK → Item (재료/장비 모두 가능)
  - quantity        : 수량 (재료 필수, 장비 보통 1)
  - sortOrder
```

### [Listing 레이어 — 기존 설계 기반] BundleEquipmentDetail (장비 상세)

```
테이블: bundle_equipment_details
주요 필드:
  - bundleLineId    : PK/FK → BundleLine (@MapsId)
  - equipmentItem   : FK → EquipmentItem
  - equipmentKindSnapshot : APPEARANCE | NORMAL (등록 시점 스냅샷)
  - enhanceLevel    : nullable (외변 정책: 5만 유효)
  - hasRitual       : boolean (주술 적용 여부)
```

### [Listing 레이어 — 기존 설계 기반] BundleEquipmentRitual (주술 결과)

```
테이블: bundle_equipment_rituals
주요 필드:
  - id
  - bundleLine      : FK → BundleLine
  - ritual          : FK → Ritual
  - outcome         : SUCCESS | GREAT_SUCCESS
  - appliedMarkSnapshot : 최종 표기 문자열 스냅샷
UNIQUE: (bundle_line_id, ritual_id)
```

### [Listing 레이어 — 추가] BundleEquipmentGem (장비에 박힌 보석)

기존 설계에 누락된 엔티티. 장비 피스에 박힌 보석 정보를 저장한다.

```sql
CREATE TABLE bundle_equipment_gems (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    bundle_line_id  BIGINT NOT NULL,
    gem_id          BIGINT NOT NULL,
    FOREIGN KEY (bundle_line_id) REFERENCES bundle_lines(id),
    FOREIGN KEY (gem_id) REFERENCES gems(gem_id)
);
```

---

### 체크박스 UI → 엔티티 연결 흐름

```
[판매자 등록 UI]

1. 서버 선택 → TradeListing.server_id

2. 세트 선택 (예: 챠우세트)
   → EquipmentSet 조회
   → EquipmentSetPiece 5개 렌더링

3. 피스별 체크박스
   ☑ 챠우 투구    → BundleLine (item=챠우투구)
                    → BundleEquipmentDetail (hasRitual=true)
                    → BundleEquipmentRitual (ritual=태산북두, outcome=SUCCESS)
   ☐ 챠우 갑옷
   ☑ 챠우 장갑    → BundleLine (item=챠우장갑)
                    → BundleEquipmentDetail (hasRitual=false)
   ☑ 챠우 바지    → BundleLine (item=챠우바지)
                    → BundleEquipmentDetail (hasRitual=true)
                    → BundleEquipmentRitual (ritual=용감무쌍, outcome=GREAT_SUCCESS)
   ☐ 챠우 신발

4. 주술 드롭다운 렌더링
   → RitualApplicability WHERE equipment_item_id = 선택한 피스
   → 허용된 Ritual 목록만 노출

5. 제출
   → TradeListing 1개
   → ListingBundle 1개 (bundleType=EQUIPMENT_SET)
   → BundleLine 3개 (체크된 피스 수만큼)
   → BundleEquipmentDetail 3개
   → BundleEquipmentRitual N개 (주술 적용 피스만)
```

### 세트 표기 자동 생성 규칙

```
totalPieces         = BundleLine 수
ritualAppliedPieces = BundleEquipmentRitual 존재하는 라인 수
ritualMark          = appliedMarkSnapshot (주술 마크)

5피스 + 전체 동일 주술 → "풀 {ritualMark} {세트명}"
                          예: "풀 XX 챠우세트"

5피스 + 일부 주술      → "풀 {세트명} {ritualAppliedPieces}{ritualMark}"
                          예: "풀 챠우세트 3XX"

N피스 (N<5)           → "{N}피스 {세트명}" 또는 커스텀 titleOverride 사용
```

---

## 테이블 생성 순서 (FK 의존성)

```
1. servers
2. rituals
3. gems                       (rituals 참조)
4. items
5. material_items             (items 참조)
6. equipment_sets
7. equipment_items            (items, equipment_sets 참조)
8. equipment_set_pieces       (equipment_sets, equipment_items 참조)
9. ritual_applicabilities     (rituals, equipment_items 참조)
10. material_price_history   (items, servers 참조)
11. users
12. trade_listings            (users, servers 참조)
13. listing_bundles           (trade_listings 참조)
14. bundle_lines              (listing_bundles, items 참조)
15. bundle_equipment_details  (bundle_lines, equipment_items 참조)
16. bundle_equipment_rituals  (bundle_lines, rituals 참조)
17. bundle_equipment_gems     (bundle_lines, gems 참조)
18. trade_applications        (trade_listings, users 참조)
19. trade_confirmed           (trade_listings, trade_applications, users 참조)
20. trade_stat_daily          (집계, 원천 참조 없음)
21. trade_stat_monthly        (집계, 원천 참조 없음)
```

---

## Spring Batch 크롤링 구조

### 의존성

```xml
<dependency>
    <groupId>org.jsoup</groupId>
    <artifactId>jsoup</artifactId>
    <version>1.17.2</version>
</dependency>
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>s3</artifactId>
    <version>2.25.0</version>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-batch</artifactId>
</dependency>
```

### Job 1 — 마스터 데이터 수집 (최초 1회 + 관리자 수동 트리거)

```
Step 1: ItemListReader
  - geota /gersang/calculator/item 파싱
  - li.cursor-pointer 태그에서 아이템명 raw string 전체 수집
  - 구 데이터 skip: 괄호+숫자 패턴(\(\+\d+\)) 포함 시 제외
  - GEM_NAMES 매칭으로 Gem/Item 분류
  - Gem → gems 테이블 UPSERT
  - EquipmentItem / MaterialItem → items 테이블 UPSERT

Step 2: ItemDetailWriter
  - items 테이블에서 image_url IS NULL 항목 조회
  - gerniverse /item/{아이템명} 파싱
  - 능력치, 재료, 이미지 수집
  - 이미지 S3 업로드 후 image_url 업데이트

Step 3: MercenaryListReader
  - geota /gersang/calculator/mercenary 파싱
  - 용병명 전체 수집 → mercenaries 테이블 UPSERT

Step 4: MercenaryDetailWriter
  - gerniverse /mercenary/{용병명} 파싱
  - 저항깎, 속성값, 재료, 이미지 수집
  - S3 업로드 후 image_url 업데이트
```

### Job 2 — 가격 데이터 수집 (매월 1일 새벽 3시 자동 실행)

```
Step 1: servers 테이블 전체 조회 (13개)

Step 2: 서버별 육의전 페이지 순환 (커스텀 ItemReader)
  - Spring Batch ItemReader의 read()가 null 반환 시 Step 자동 종료
  - geota /gersang/yukeuijeon?serverId={id} 페이지 순환
  - 수집 항목: 아이템명, 수량, 단가

Step 3: ItemProcessor — IQR 이상치 제거 및 집계
  - 서버+아이템 단위로 단가 목록 수집
  - IQR 방식 이상치 제거
  - 최소 샘플 5건 미만 시 skip
  - avgPrice, minPrice, sampleCount 산출

Step 4: ItemWriter — material_price_history UPSERT
  - 같은 year_month + server_id 조합이면 덮어쓰기
```

### 스케줄러

```java
@Scheduled(cron = "0 0 3 1 * *")  // 매월 1일 새벽 3시
public void runMonthlyCrawl() { ... }

@SpringBootApplication
@EnableScheduling
@EnableBatchProcessing
public class Application { ... }
```

### 관리자 수동 트리거 API

```
POST /admin/crawler/master   → 마스터 데이터 수집 Job 실행
POST /admin/crawler/price    → 가격 데이터 수집 Job 즉시 실행
```

### 크롤링 공통 설정

```
User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64)
요청 딜레이: 1~2초 (서버 부하 방지)
타임아웃: 10초
재시도: 최대 3회 (IOException 발생 시)
```

---

## 가성비 계산기에서 가격 조회 흐름

```
유저가 가성비 계산 요청
    ↓
material_price_history에서 직전 달 서버별 avgPrice / minPrice 조회
    ↓
유저의 priceOverride 값이 있으면 → 그 값 우선 사용
없으면 → avgPrice 기본값으로 표시 (유저가 minPrice로 전환 가능)
    ↓
데미지 상승률 ÷ 최종 가격 = 가성비 점수
```

---

## 폴더 구조 제안

```
src/main/java/
├── domain/
│   ├── catalog/
│   │   ├── Ritual.java
│   │   ├── RitualApplicability.java
│   │   ├── EquipmentSet.java
│   │   ├── EquipmentSetPiece.java
│   │   ├── EquipmentItem.java
│   │   ├── MaterialItem.java
│   │   ├── Item.java
│   │   └── Gem.java                      ← 신규 추가
│   ├── listing/
│   │   ├── TradeListing.java
│   │   ├── ListingBundle.java
│   │   ├── BundleLine.java
│   │   ├── BundleEquipmentDetail.java
│   │   ├── BundleEquipmentRitual.java
│   │   └── BundleEquipmentGem.java       ← 신규 추가
│   └── trade/
│       ├── TradeApplication.java
│       ├── TradeConfirmed.java
│       ├── TradeStatDaily.java
│       └── TradeStatMonthly.java
├── crawler/
│   ├── job/
│   │   ├── MasterDataJobConfig.java
│   │   └── PriceCrawlJobConfig.java
│   ├── reader/
│   │   ├── ItemListReader.java
│   │   ├── MercenaryListReader.java
│   │   └── YukeuijeonPageReader.java
│   ├── processor/
│   │   ├── ItemNameParser.java           ← Gem/Item 분류 로직
│   │   └── PriceIqrProcessor.java
│   ├── writer/
│   │   ├── ItemDetailWriter.java
│   │   ├── MercenaryDetailWriter.java
│   │   └── PriceHistoryWriter.java
│   └── scheduler/
│       └── CrawlerScheduler.java
├── config/
│   └── S3Config.java
└── admin/
    └── CrawlerAdminController.java
```

---

## ⚠️ 구현 전 추가 검증 필요 사항

### 검증 1 — geota 아이템 목록 HTML 선택자 확인

```
URL: https://geota.co.kr/gersang/calculator/item?serverId=1
확인 항목:
  - li.cursor-pointer 태그가 실제 아이템 목록과 일치하는지 재확인
  - 아이템명 이외의 텍스트가 같은 선택자에 포함되는지 여부
  - keyword URL 파라미터로 특정 아이템 직접 접근 가능 여부 (배치 처리 시 활용)
```

### 검증 2 — geota 용병 목록 HTML 선택자 확인

```
URL: https://geota.co.kr/gersang/calculator/mercenary?serverId=1
확인 항목:
  - 용병명 선택자
  - 재료명 + 수량 선택자
  - 재료 단가 선택자 (숫자 + 냥 형식)
  - "더보기" 버튼 존재 여부 → 전체 목록이 한번에 나오는지 확인
```

### 검증 3 — geota 육의전 페이지네이션 서버사이드 여부 확인

```
URL: https://geota.co.kr/gersang/yukeuijeon?serverId=1&page=2
확인 항목:
  - page 파라미터가 서버 사이드로 동작하는지 (현재 클라이언트 필터링으로 추정)
  - 클라이언트 사이드라면 전체 데이터가 한번에 렌더링되는지 확인
  - 전체 거래 건수 확인 (IQR 처리 가능한 충분한 샘플인지)
  - 소규모 서버(신구, 단군 등) 최소 샘플 기준 충족 여부
```

### 검증 4 — gerniverse 아이템 JSON-LD 구조 재확인

```
URL: https://gerniverse.app/item/각성석 (재료 아이템)
확인 항목:
  - 재료 아이템과 장비 아이템의 JSON-LD 구조 차이
  - image 필드가 항상 배열인지, null이 올 수 있는지
  - 일부 아이템에 이미지가 없는 경우 fallback 처리 방식
```

### 검증 5 — gerniverse 용병 저항깎 수치 파싱 방식

```
URL: https://gerniverse.app/mercenary/{저항깎 용병명}
확인 항목:
  - JSON-LD additionalProperty의 value 형식 ("100%" vs 숫자)
  - 마법저항깎과 타격저항깎 분리 여부
  - 스킬로 인한 저항깎 (조건부)과 기본 저항깎 구분 방식
    예: 각성 군다리명왕 — 공중 몬스터 마법저항 10 감소 스킬
        → 고정 수치 저장 vs 스킬 설명 텍스트 저장 정책 결정 필요
```

### 검증 6 — 보석 기간제/특수 패턴 확인

```
URL: https://geota.co.kr/gersang/calculator/item?serverId=1
확인 항목:
  - 보석에 "- 7일" 기간제 패턴이 존재하는지
  - "빛나는 {보석명}" 외 다른 특수 등급 보석이 있는지
  - 11종 외에 추가 보석이 있는지 (게임 업데이트 반영 여부)
  - 보석 가락지/반지 형태 아이템 처리 방식
    예: "흑요석가락지", "<황색> 흑요석가락지" → Item으로 분류할지 Gem으로 분류할지
```

### 검증 7 — EquipmentItem 기존 구조 확인

```
확인 항목:
  - 기존 EquipmentItem에 ritual 관련 컬럼이 있는지
  - hasSlot(홈이있는) 컬럼이 필요한지, 별도 아이템명으로 구분하는지
  - EquipmentItem과 Item의 name 컬럼이 순수 아이템명인지
    (주술/홈이있는 prefix 없는 순수 이름으로 저장되어야 함)
```