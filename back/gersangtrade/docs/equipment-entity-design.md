# 장비 세트 엔티티 설계 문서

> 거상 거래 플랫폼 — 장비 세트/주술 관련 엔티티 변경사항 및 설계 확정본  
> 작성일: 2026-04-28

---

## 목차
1. [배경 및 도메인 개념](#1-배경-및-도메인-개념)
2. [엔티티 변경사항 총정리](#2-엔티티-변경사항-총정리)
3. [StatType / StatUnit 정의](#3-stattype--statunit-정의)
4. [엔티티별 상세 설계](#4-엔티티별-상세-설계)
5. [데이터 예시](#5-데이터-예시)
6. [거래 매물 표현 예시](#6-거래-매물-표현-예시)
7. [크롤링 방향](#7-크롤링-방향)

---

## 1. 배경 및 도메인 개념

### 1.1 세트 구성

거상의 장비 세트는 여러 피스(부위)로 구성된다.

```
지국천왕 세트 (6종)
  ├── 투구
  ├── 갑옷
  ├── 요대
  ├── 신발
  ├── 장갑
  └── 반지 x2 (pieceCount=2)
```

### 1.2 세트 효과

착용한 피스 수에 따라 추가 능력치가 부여된다.

```
지국천왕:
  2종 → 힘+50
  3종 → 힘+100
  4종 → 지력+100
  5종 → 타저+25%
  6종 → 힘+400, 속성값+5 (땅속성+3)
```

### 1.3 주술

장비 피스에 주술 보석을 박아 추가 능력치를 부여하는 강화 시스템이다.

```
주술 종류:
  천추   → 힘+120  (북두칠성 대성공 시 힘+180)
  천선   → 지력+120
  천기   → 생명+120
  천권   → 민첩+120
  개양   → 힘/민/생/지 +40, 속성값+1
  요광   → 힘/민/생/지 +40, 데미지+1%
  옥형   → 힘/민/생/지 +40
```

### 1.4 주술 세트 효과

동일 주술을 여러 피스에 적용하면 추가 효과가 부여된다.

```
천추 주술:
  3세트 → 힘+150, 데미지+2%
  5세트 → 힘+150, 데미지+3%

북두칠성(천추 대성공):
  3세트 → 힘+250, 화속성값+2
  5세트 → 힘+250, 화속성값+3
```

### 1.5 거래 명칭 규칙

명칭 공식: `{주술수+주술명} {피스구성명}{세트명} {반쌍여부}` (상세 규칙은 4.8 참고)

```
풀지국          = 반지 제외 5피스 (주술 없음, 반지 없음)
풀지국 반쌍     = 반지 제외 5피스 + 반지쌍 (quantity=2)
3북두 풀지국    = 반지 제외 5피스 + 3피스 북두칠성 주술
5북두 풀지국    = 반지 제외 5피스 + 5피스 북두칠성 주술
5북두 풀지국 반쌍 = 반지 제외 5피스 + 5피스 북두칠성 주술 + 반지쌍
갑투지국        = 갑옷 + 투구 (2종)
```

> "풀"은 `totalPieces=6` 세트에서 **반지 제외 5피스**일 때만 사용한다.

### 1.6 거래가치 기준

주술 자체가 거래가치 없는 것이 아니라, **주술 + 세트 조합**에 따라 거래가치가 결정된다.

```
천기 + 광목천왕셋 → 거래가치 있음 ✅
천기 + 미트라셋   → 거래가치 없음 ❌ (미트라셋이 구버전 메타)
```

따라서 `EquipmentSet.isTradeable` 컬럼으로 세트 단위의 거래가치를 관리한다.

---

## 2. 엔티티 변경사항 총정리

### 수정 대상

| 엔티티 | 변경 내용 |
|---|---|
| `EquipmentSet` | `setType` 제거, `totalPieces` 추가, `isTradeable` 추가 |
| `ItemStat` (및 stat 관련 전체) | `stat_unit` 컬럼 추가 |

### 신규 추가 대상

| 엔티티 | 역할 |
|---|---|
| `EquipmentSetEffect` | 세트 착용 수(2종/3종/6종)별 추가 능력치 |
| `RitualSetEffect` | 주술 세트(3셋/5셋)별 추가 능력치 |
| `BundleEquipmentRitual` | 피스별 주술 결과 (SUCCESS/GREAT_SUCCESS + 마크 스냅샷). `BundleLine`과 1:1 연결 |

---

## 3. StatType / StatUnit 정의

### 3.1 StatType 전체 목록

능력치 종류를 정의하는 **enum**이다. `ItemStat`, `EquipmentSetEffect`, `RitualSetEffect` 전체에서 공통으로 사용한다.

> 기존 이름(`HITTING_RESISTANCE`, `MAGIC_RESISTANCE`)은 **유지**한다.  
> 하단 3종은 세트효과/주술효과에서만 등장하는 신규 추가 타입이다.

| StatType | 설명 | 기본 단위 | 비고 |
|---|---|---|---|
| `STRENGTH` | 힘 | FLAT | 기존 |
| `DEXTERITY` | 민첩 | FLAT | 기존 |
| `VITALITY` | 생명 | FLAT | 기존 |
| `INTELLECT` | 지력 | FLAT | 기존 |
| `DEFENSE` | 방어 | FLAT | 기존 |
| `MIN_POWER` | 최소공격력 | FLAT | 기존 |
| `MAX_POWER` | 최대공격력 | FLAT | 기존 |
| `HITTING_RESISTANCE` | 타격저항(타저) | PERCENT | 기존 — 이름 유지 |
| `MAGIC_RESISTANCE` | 마법저항(마저) | PERCENT | 기존 — 이름 유지 |
| `ELEMENT_VALUE` | 속성값 | FLAT | 기존 |
| `ELEMENT_PIERCE` | 속성깎 | FLAT | 기존 |
| `RESIST_PIERCE` | 저항깎 | FLAT | 기존 |
| `DAMAGE_PERCENT` | 데미지 증가 | PERCENT | **신규** |
| `SKILL_DAMAGE_PERCENT` | 스킬 데미지 증가 | PERCENT | **신규** |
| `FIELD_MOVE_SPEED` | 필드 이동속도 | FLAT | **신규** |

> **LEVEL 단위**: 확장용 예약. 현재 사용하는 stat에서는 이속도 FLAT으로 처리한다.

### 3.2 StatUnit 정의

`stat_value`의 단위를 명시하여 UI 표시와 계산 로직의 모호함을 제거하는 **enum**이다.  
`ItemStat`, `EquipmentSetEffect`, `RitualSetEffect` 모두 이 enum을 사용한다.

| StatUnit | 설명 | UI 표시 예시 |
|---|---|---|
| `FLAT` | 수치 그대로 | `힘 +400` |
| `PERCENT` | 퍼센트 | `타저 +25%`, `데미지 +2%` |
| `LEVEL` | 단계 | (현재 미사용, 확장용 예약) |

### 3.3 stat_unit이 필요한 이유

```
❌ stat_unit 없을 때:
  HITTING_RESISTANCE  stat_value=25  → 25%인지 25 수치인지 모호
  DAMAGE_PERCENT      stat_value=2   → 2%인지 그냥 2인지 모호
  FIELD_MOVE_SPEED    stat_value=1   → 단위가 뭔지 모름

✅ stat_unit 있을 때:
  HITTING_RESISTANCE  stat_value=25  stat_unit=PERCENT → UI: "타저 +25%"
  DAMAGE_PERCENT      stat_value=2   stat_unit=PERCENT → UI: "데미지 +2%"
  FIELD_MOVE_SPEED    stat_value=1   stat_unit=LEVEL   → UI: "이속 +1단계"
```

### 3.4 크롤링 코드 영향

`stat_unit` 추가로 기존 크롤링 코드 수정이 필요하다.

```
수정 대상:
  ItemStat 저장 로직 전체
  → statType 파싱 시 stat_unit도 함께 결정해서 저장

예시:
  "타저30%" 파싱 → statType=HITTING_RESISTANCE, statValue=30, statUnit=PERCENT
  "힘300"   파싱 → statType=STRENGTH,         statValue=300, statUnit=FLAT
```

---

## 4. 엔티티별 상세 설계

### 4.1 EquipmentSet (수정)

```java
@Entity
@Table(name = "equipment_sets")
public class EquipmentSet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 세트 명칭 — 예: "지국천왕", "각성지국천왕" */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    // ❌ 제거: setType ("3셋", "5셋", "풀" 은 거래 등록 시 결정되는 값)

    /** 세트 최대 피스 수 — 예: 6 */
    @Column(name = "total_pieces", nullable = false)
    private Integer totalPieces;

    /**
     * 현재 메타 세트 여부.
     * false이면 거래 목록에서 노출 제외.
     * 기본값 true, 관리자가 개별 토글 가능.
     * 크롤링 시 전부 true로 저장.
     */
    @Column(name = "is_tradeable", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean isTradeable = true;
}
```

**변경 이유:**
- `setType`: "3셋", "5셋"은 세트 자체의 속성이 아니라 판매자가 몇 개 착용해서 파는지에 따라 달라지는 값 → `TradeItemPiece`의 피스 수로 자동 계산
- `totalPieces`: 세트가 몇 종으로 구성되는지 (지국천왕=6, 브리트라=2 등)
- `isTradeable`: 게임 메타 변화에 따라 관리자가 유연하게 관리

---

### 4.2 EquipmentSetEffect (신규)

세트 착용 수에 따른 추가 능력치를 저장한다.

```java
@Entity
@Table(
    name = "equipment_set_effects",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"equipment_set_id", "required_pieces", "stat_type", "element"}
    )
)
public class EquipmentSetEffect {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 어떤 세트의 효과인지 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_set_id", nullable = false)
    private EquipmentSet equipmentSet;

    /** 몇 종 착용 시 발동하는 효과인지 (2, 3, 4, 5, 6) */
    @Column(name = "required_pieces", nullable = false)
    private Integer requiredPieces;

    /** 능력치 종류 */
    @Enumerated(EnumType.STRING)
    @Column(name = "stat_type", nullable = false, length = 30)
    private StatType statType;

    /** 능력치 수치 */
    @Column(name = "stat_value", nullable = false)
    private Integer statValue;

    /** 능력치 단위 — FLAT / PERCENT / LEVEL */
    @Enumerated(EnumType.STRING)
    @Column(name = "stat_unit", nullable = false, length = 10)
    private StatUnit statUnit;

    /**
     * 속성 종류 — ELEMENT_VALUE인 경우만 사용, 나머지 null.
     * null = 모든 속성 공통 증가 (예: 속성값+5).
     * non-null = 특정 속성 증가 (예: EARTH → 땅속성+3).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "element", length = 20)
    private Element element;
}
```

---

### 4.3 RitualSetEffect (신규)

동일 주술을 여러 피스에 적용했을 때 발동하는 추가 능력치를 저장한다.

```java
@Entity
@Table(
    name = "ritual_set_effects",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"ritual_id", "outcome", "equipment_set_id", "required_ritual_pieces", "stat_type"}
    )
)
public class RitualSetEffect {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 어떤 주술의 세트 효과인지 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ritual_id", nullable = false)
    private Ritual ritual;

    /**
     * 주술 결과 구분 — SUCCESS(성공) / GREAT_SUCCESS(대성공).
     * 천추 SUCCESS와 천추 GREAT_SUCCESS(북두칠성)는 같은 ritual_id이지만
     * 세트 효과가 다르므로 outcome으로 구분한다.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false, length = 20)
    private RitualOutcome outcome;

    /**
     * 어떤 세트에 적용되는 효과인지.
     * 주술은 시도 가능한 장비 세트가 고정되어 있으므로 NOT NULL.
     * 동일 주술이라도 세트가 다르면 별도 행으로 저장한다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_set_id", nullable = false)
    private EquipmentSet equipmentSet;

    /** 주술을 몇 피스에 적용해야 발동하는지 (3 또는 5) */
    @Column(name = "required_ritual_pieces", nullable = false)
    private Integer requiredRitualPieces;

    /** 능력치 종류 */
    @Enumerated(EnumType.STRING)
    @Column(name = "stat_type", nullable = false, length = 30)
    private StatType statType;

    /** 능력치 수치 */
    @Column(name = "stat_value", nullable = false)
    private Integer statValue;

    /** 능력치 단위 — FLAT / PERCENT / LEVEL */
    @Enumerated(EnumType.STRING)
    @Column(name = "stat_unit", nullable = false, length = 10)
    private StatUnit statUnit;

    /**
     * 속성 종류 — ELEMENT_VALUE인 경우만 사용, 나머지 null.
     * null = 모든 속성 공통 증가.
     * non-null = 특정 속성 증가 (예: EARTH, FIRE 등).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "element", length = 20)
    private Element element;
}
```

**주의사항:**
- 천추(SUCCESS)와 북두칠성(GREAT_SUCCESS)은 **같은 `ritual_id=천추`** 를 사용한다.
- `outcome` 컬럼으로 SUCCESS / GREAT_SUCCESS를 구분하며, `BundleEquipmentRitual`과 동일한 `RitualOutcome` enum을 재사용한다.

---

### 4.4 ListingBundle (수정)

> 기존 코드: `listing_bundles` 테이블, `bundleType` / `titleOverride` 보유

**추가할 것:** `equipment_set_id` FK 컬럼

- `bundleType = EQUIPMENT_SET`일 때 어떤 세트인지 명시하기 위해 `EquipmentSet` FK를 추가한다.
- `EQUIPMENT_SINGLE` 또는 `MATERIAL_BUNDLE`이면 null.
- 세트 효과 계산, 자동 명칭 생성 시 이 FK를 통해 `EquipmentSet`을 참조한다.

---

### 4.5 BundleLine (수정)

> 기존 코드: `bundle_lines` 테이블, `item` / `quantity` / `sortOrder` 보유

**추가할 것:** `equipment_set_piece_id` FK 컬럼

- 세트 매물(`EQUIPMENT_SET`)일 때 이 라인이 어떤 세트 피스(투구/갑옷 등)인지 연결하기 위해 `EquipmentSetPiece` FK를 추가한다.
- 단품 또는 재료 매물이면 null.
- 이 FK가 있어야 "지국천왕 갑옷 피스"임을 알 수 있고, 주술 세트 효과 계산도 가능하다.

**반지 수량 처리:**
- 반지 풀 = `quantity=2`, BundleLine 1행
- 반지 반쌍 = `quantity=1`, BundleLine 1행
- 기존 `quantity` 컬럼으로 처리 가능, 별도 컬럼 추가 불필요

---

### 4.6 BundleEquipmentDetail (확인)

> 기존 코드: `bundle_equipment_details` 테이블, `enhanceLevel` / `hasRitual` / `gem` 보유

**추가 불필요** — 현재 구조로 충분하다.

- 주술 여부는 `hasRitual` boolean으로 표시
- 실제 주술 종류/결과는 `BundleEquipmentRitual`에서 조회
- 보석은 `gem` FK로 직접 연결 (장비당 최대 1개)

---

### 4.7 BundleEquipmentRitual (확인)

> 기존 코드: `bundle_equipment_rituals` 테이블, `ritual` / `outcome` / `appliedMarkSnapshot` 보유

**추가 불필요** — 현재 구조로 충분하다.

- `outcome = SUCCESS` → 천추 성공
- `outcome = GREAT_SUCCESS` → 북두칠성 (천추 대성공)
- 이 방식이 별도 Ritual 행으로 분리하는 것보다 훨씬 깔끔하다.
- `appliedMarkSnapshot`으로 등록 당시 표기를 보존하므로 카탈로그 변경에도 안전하다.

---

### 4.8 자동 명칭 생성 규칙

`BundleLine` 데이터를 기반으로 거래 명칭을 자동 생성한다. `titleOverride`가 있으면 자동 생성 명칭 대신 사용한다.

**피스 조합별 명칭:**

> "풀" 은 **5피스 세트(totalPieces=6)에서 반지를 제외한 5종 착용**일 때만 사용한다.  
> 브리트라(totalPieces=2)처럼 소규모 세트는 isTradeable=false이며 명칭 자동생성 대상 아님.

| 피스 구성 | 명칭 | 적용 조건 |
|---|---|---|
| 반지 제외 5피스 | 풀 | totalPieces=6인 세트만 |
| 반지 제외 5피스 + 반지쌍(quantity=2) | 풀 반쌍 | totalPieces=6인 세트만 |
| 장갑+요대+신발 | 변 | |
| 갑옷+투구 | 갑투 | 소규모 세트 포함 |
| 장갑만 | 장신 | |
| 장갑+요대 | 장요 | |
| 요대+신발 | 요신 | |
| 그 외 조합 | 부분 | |

**반지 단품 거래 (EQUIPMENT_SINGLE):**
- 반짝 = 반지 1개 (quantity=1)
- 반쌍 = 반지 2개 (quantity=2)
- 세트 매물에서 반지는 무조건 quantity=2 (쌍), 반지 1개짜리 세트 매물은 없음

**전체 명칭 공식:**
```
{주술수+주술명} {피스구성명}{세트명} {반쌍여부}

예:
  풀지국
  풀지국 반쌍
  3북두 풀지국
  3북두 풀지국 반쌍
  5북두 변지국
  갑투지국
```

---

### 5.1 EquipmentSetEffect — 지국천왕 세트

| equipment_set_id | required_pieces | stat_type | stat_value | stat_unit | element |
|---|---|---|---|---|---|
| (지국천왕) | 2 | STRENGTH | 50 | FLAT | null |
| (지국천왕) | 3 | STRENGTH | 100 | FLAT | null |
| (지국천왕) | 4 | INTELLECT | 100 | FLAT | null |
| (지국천왕) | 5 | HITTING_RESISTANCE | 25 | PERCENT | null |
| (지국천왕) | 6 | STRENGTH | 400 | FLAT | NONE |
| (지국천왕) | 6 | ELEMENT_VALUE | 5 | FLAT | ADAPTIVE |  ← 속성값+5: 용병 속성 추종
| (지국천왕) | 6 | ELEMENT_VALUE | 3 | FLAT | EARTH |  ← 땅속성+3: 땅속성 고정 증가 (별도 효과)

### 5.2 RitualSetEffect — 천추 / 북두칠성

> `equipment_set_id`는 NOT NULL. 주술을 시도할 수 있는 세트가 고정되어 있으므로  
> 동일 주술+동일 required_pieces라도 세트가 다르면 별도 행으로 저장한다.

| ritual_id | outcome | equipment_set_id | required_ritual_pieces | stat_type | stat_value | stat_unit | element |
|---|---|---|---|---|---|---|---|
| (천추) | SUCCESS | (지국천왕) | 3 | STRENGTH | 150 | FLAT | null |
| (천추) | SUCCESS | (지국천왕) | 3 | DAMAGE_PERCENT | 2 | PERCENT | null |
| (천추) | SUCCESS | (지국천왕) | 5 | STRENGTH | 150 | FLAT | null |
| (천추) | SUCCESS | (지국천왕) | 5 | DAMAGE_PERCENT | 3 | PERCENT | null |
| (천추) | GREAT_SUCCESS | (지국천왕) | 3 | STRENGTH | 250 | FLAT | null |
| (천추) | GREAT_SUCCESS | (지국천왕) | 3 | ELEMENT_VALUE | 2 | FLAT | FIRE |
| (천추) | GREAT_SUCCESS | (지국천왕) | 5 | STRENGTH | 250 | FLAT | null |
| (천추) | GREAT_SUCCESS | (지국천왕) | 5 | ELEMENT_VALUE | 3 | FLAT | FIRE |

---

## 6. 거래 매물 표현 예시

### 세트 매물 — "3북두 풀지국" (반지 없이)

```
TradeListing
  └── ListingBundle (EQUIPMENT_SET, equipmentSet=지국천왕)
        ├── BundleLine: 투구  quantity=1 → 북두(GREAT_SUCCESS)
        ├── BundleLine: 갑옷  quantity=1 → 북두(GREAT_SUCCESS)
        ├── BundleLine: 요대  quantity=1 → 북두(GREAT_SUCCESS)
        ├── BundleLine: 신발  quantity=1 → 주술 없음
        └── BundleLine: 장갑  quantity=1 → 주술 없음
        ← 반지 BundleLine 없음 → "풀"
```

### 세트 매물 — "5북두 풀지국 반쌍" (반지쌍 포함)

```
TradeListing
  └── ListingBundle (EQUIPMENT_SET, equipmentSet=지국천왕)
        ├── BundleLine: 투구  quantity=1 → 북두(GREAT_SUCCESS)
        ├── BundleLine: 갑옷  quantity=1 → 북두(GREAT_SUCCESS)
        ├── BundleLine: 요대  quantity=1 → 북두(GREAT_SUCCESS)
        ├── BundleLine: 신발  quantity=1 → 북두(GREAT_SUCCESS)
        ├── BundleLine: 장갑  quantity=1 → 북두(GREAT_SUCCESS)
        └── BundleLine: 반지  quantity=2 → 주술 없음
        ← 반지 quantity=2 → "풀 반쌍"
```

### 세트 매물 — "갑투"

```
TradeListing
  └── ListingBundle (EQUIPMENT_SET, equipmentSet=지국천왕)
        ├── BundleLine: 투구  quantity=1 → 주술 없음
        └── BundleLine: 갑옷  quantity=1 → 주술 없음
```

### 단품 매물 — "북두 지국천왕 갑옷"

```
TradeListing
  └── ListingBundle (EQUIPMENT_SINGLE, equipmentSet=null)
        └── BundleLine: 지국천왕갑옷  quantity=1
              └── BundleEquipmentDetail (hasRitual=true)
                    └── BundleEquipmentRitual (북두칠성, GREAT_SUCCESS)
```

### 재료 + 세트 같이 판매

```
TradeListing
  ├── ListingBundle (EQUIPMENT_SET, equipmentSet=지국천왕)
  │     ├── BundleLine: 투구 ...
  │     └── BundleLine: 갑옷 ...
  └── ListingBundle (MATERIAL_BUNDLE, equipmentSet=null)
        └── BundleLine: 뇌정석  quantity=50
```

---

## 7. 크롤링 방향

### 7.1 데이터 소스

모든 크롤링 대상 사이트는 **거상짱**으로 통일한다.

```
gerniverse.app  → Cloudflare 차단 ❌
geota.co.kr     → x-signature HMAC 차단 ❌
거상짱          → 전통 SSR, 시맨틱 HTML ✅
```

### 7.2 크롤러 분리 방향

현재 크롤러가 모든 아이템을 `MATERIAL` 타입으로 저장하는 문제가 있다.

```
기존 크롤러 (수정 필요)
  → 장비 아이템 → EquipmentItem으로 저장
  → stat 저장 시 stat_unit 함께 파싱/저장

신규 크롤러 (추가 필요)
  → 재료 아이템     → MaterialItem으로 저장
  → 세트 효과      → EquipmentSetEffect로 저장
  → 주술 세트 효과 → RitualSetEffect로 저장
```

### 7.3 stat_unit 파싱 규칙

크롤링 시 능력치 문자열에서 stat_unit을 결정하는 규칙:

```
"힘300"      → statUnit = FLAT
"타저30%"    → statUnit = PERCENT
"마저55%"    → statUnit = PERCENT
"데미지+2%"  → statUnit = PERCENT
"이속+1"     → statUnit = FLAT
"속성값+5"   → statUnit = FLAT
```

### 7.4 isTradeable 처리

크롤링 시 `EquipmentSet.isTradeable = true`로 전부 저장한다.
메타 판단은 이후 관리자 페이지에서 개별 토글로 관리한다.

---

## 확정 사항 (closed)

| 항목 | 결정 |
|---|---|
| StatType/StatUnit String → Enum | ✅ enum으로 통일 (EquipmentSetEffect, RitualSetEffect 포함) |
| StatType 이름 변경 여부 | ❌ 기존 이름 유지. DAMAGE_PERCENT / SKILL_DAMAGE_PERCENT / FIELD_MOVE_SPEED만 신규 추가 |
| ELEMENT_VALUE 중복 행 | ✅ 중복 아님. element=ADAPTIVE(용병 속성 추종)과 element=EARTH(땅속성 고정)는 별도 효과 |
| RitualSetEffect.equipment_set_id | ✅ NOT NULL. 주술 가능 세트가 고정이므로 세트별 별도 행 |
| "풀" 기준 | ✅ totalPieces=6 세트에서 반지 제외 5피스만. 소규모 세트는 isTradeable=false |

## 오픈 이슈

| 항목 | 내용 | 우선순위 |
|---|---|---|
| 크롤링 코드 수정 범위 | stat_unit 추가로 기존 ItemStat 저장 로직 전체 수정 필요 | 높음 |