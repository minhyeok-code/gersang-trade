# 덱 용병 장비 슬롯 설계 문서

> 거상 거래 플랫폼 — 유저 덱 장비 슬롯 엔티티 설계 확정본

---

## 목차

1. [개요 및 설계 원칙](#1-개요-및-설계-원칙)
2. [Enum 정의](#2-enum-정의)
3. [엔티티 설계](#3-엔티티-설계)
4. [계산기 연동 흐름](#4-계산기-연동-흐름)
5. [제약 조건](#5-제약-조건)
6. [UI/UX 흐름](#6-uiux-흐름)
7. [오픈 이슈](#7-오픈-이슈)

---

## 1. 개요 및 설계 원칙

### 1.1 배경

유저가 덱을 구성할 때 각 용병에 장비 아이템을 착용시킬 수 있다.
장비 아이템은 일반 슬롯(8종 9슬롯)과 외변 슬롯(9종 9슬롯)으로 분리된다.
외변 아이템도 스탯/데미지/속성값 효과가 있으므로 계산기에 포함된다.

### 1.2 설계 원칙

```
- 모든 슬롯(일반 + 외변)의 ItemStat      → 스탯 계산에 포함
- 모든 슬롯(일반 + 외변)의 ItemSkill     → DPS 계산에 포함
- 세트효과 / 주술세트효과                 → 런타임에 계산 (DB 저장 안 함)
- 주인공 기본 스킬                        → MVP 제외. 무기 슬롯 ItemSkill만 계산
- 슬롯은 EquipSlot 통합 enum 하나로 관리  → nullable 컬럼 제거, 제약조건 단순화
```

---

## 2. Enum 정의

### 2.1 EquipSlot

일반 슬롯과 외변 슬롯을 하나의 enum으로 통합한다.

```java
public enum EquipSlot {

    // ── 일반 슬롯 (8종 9슬롯) ─────────────────────────────
    HELMET,     // 투구
    ARMOR,      // 갑옷
    WEAPON,     // 무기
    SHOES,      // 신발
    GLOVES,     // 장갑
    BELT,       // 요대
    CHARM,      // 부적
    RING_1,     // 반지1
    RING_2,     // 반지2

    // ── 외변 슬롯 (9종 9슬롯) ─────────────────────────────
    APP_SPIRIT,     // 기운
    APP_HELMET,     // 투구 (외변)
    APP_ARMOR,      // 갑옷 (외변)
    APP_WEAPON,     // 무기 (외변)
    APP_WAR_GOD,    // 무신
    APP_EARRING,    // 귀걸이
    APP_NECKLACE,   // 목걸이
    APP_BRACELET,   // 팔찌
    APP_GREAVES     // 각반
}
```

> 일반 슬롯과 외변 슬롯은 이름이 겹쳐도 별개의 슬롯이다.
> (예: `HELMET`은 일반 투구, `APP_HELMET`은 외변 투구 — 동시 착용 가능)

### 2.2 RitualOutcome

주술 강화 결과를 나타낸다.

```java
public enum RitualOutcome {
    SUCCESS,        // 일반 성공
    GREAT_SUCCESS   // 대성공 (북두칠성 등)
}
```

---

## 3. 엔티티 설계

### 3.1 전체 구조 요약

```
UserDeck
└── DeckMercenary (최대 12개)
      └── DeckMercenarySlot (슬롯당 1개, 최대 18개)
            ├── slot: EquipSlot
            ├── equipmentItem: EquipmentItem (FK, nullable)
            └── DeckMercenarySlotRitual (0~1개)
                  ├── ritual: Ritual (FK)
                  └── outcome: RitualOutcome
```

### 3.2 DeckMercenary

덱 안의 용병 목록.

```java
@Entity
@Table(
    name = "deck_mercenaries",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_deck_mercenary",
        columnNames = {"user_deck_id", "mercenary_id"}
    )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeckMercenary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_deck_id", nullable = false)
    private UserDeck userDeck;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mercenary_id", nullable = false)
    private Mercenary mercenary;

    @OneToMany(mappedBy = "deckMercenary", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DeckMercenarySlot> slots = new ArrayList<>();
}
```

**제약:**
- 동일 `UserDeck` 내 최대 12개
- 동일 `UserDeck` 내 동일 용병 중복 불가 (UNIQUE 제약)

---

### 3.3 DeckMercenarySlot

각 용병의 슬롯별 착용 아이템.

```java
@Entity
@Table(
    name = "deck_mercenary_slots",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_deck_mercenary_slot",
        columnNames = {"deck_mercenary_id", "slot"}
    )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeckMercenarySlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deck_mercenary_id", nullable = false)
    private DeckMercenary deckMercenary;

    /**
     * 착용 슬롯.
     * 일반 슬롯(HELMET~RING_2) / 외변 슬롯(APP_SPIRIT~APP_GREAVES) 통합 enum.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "slot", nullable = false, length = 30)
    private EquipSlot slot;

    /**
     * 착용 아이템. 미착용 시 null.
     * EquipmentItem.equipSlot과 DeckMercenarySlot.slot이 일치해야 한다.
     * (서비스 레이어에서 검증)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_item_id")
    private EquipmentItem equipmentItem;

    @OneToOne(mappedBy = "deckMercenarySlot", cascade = CascadeType.ALL, orphanRemoval = true)
    private DeckMercenarySlotRitual ritual;

    public static DeckMercenarySlot of(DeckMercenary deckMercenary, EquipSlot slot) {
        DeckMercenarySlot s = new DeckMercenarySlot();
        s.deckMercenary = deckMercenary;
        s.slot = slot;
        return s;
    }

    public void equip(EquipmentItem item) {
        this.equipmentItem = item;
    }

    public void unequip() {
        this.equipmentItem = null;
        this.ritual = null;
    }
}
```

---

### 3.4 DeckMercenarySlotRitual

슬롯에 착용된 아이템의 주술 정보.

```java
@Entity
@Table(name = "deck_mercenary_slot_rituals")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeckMercenarySlotRitual {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 주술이 박힌 슬롯. 1:1 관계.
     * 슬롯당 주술은 최대 1개.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deck_mercenary_slot_id", nullable = false, unique = true)
    private DeckMercenarySlot deckMercenarySlot;

    /**
     * 주술 종류.
     * RitualApplicability 기준으로 해당 아이템에 적용 가능한 주술만 선택 가능.
     * (서비스 레이어에서 검증)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ritual_id", nullable = false)
    private Ritual ritual;

    /**
     * 주술 강화 결과.
     * SUCCESS        → 일반 성공
     * GREAT_SUCCESS  → 대성공 (북두칠성 등)
     *
     * 유저가 UI에서 선택 (카탈로그 기반 선택지 제공 — 직접 입력 아님).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false, length = 20)
    private RitualOutcome outcome;

    public static DeckMercenarySlotRitual of(
            DeckMercenarySlot slot,
            Ritual ritual,
            RitualOutcome outcome) {

        DeckMercenarySlotRitual r = new DeckMercenarySlotRitual();
        r.deckMercenarySlot = slot;
        r.ritual = ritual;
        r.outcome = outcome;
        return r;
    }
}
```

---

## 4. 계산기 연동 흐름

### 4.1 스탯 계산

```
DeckMercenarySlot 전체 순회 (일반 + 외변 슬롯 모두)
  ↓
equipmentItem != null인 슬롯만 처리
  ↓
equipmentItem.getItemStats() → StatType별 합산
  ↓
세트효과 계산 (런타임):
  equipmentItem → equipmentItem.getEquipmentSet() (FK 존재)
  같은 DeckMercenary 내 동일 set_id 피스 수 집계
  EquipmentSetEffect에서 requiredPieces 이하 효과 전부 적용
  ↓
주술 스탯 계산:
  DeckMercenarySlotRitual != null인 슬롯만
  ritual.getItemStats(outcome=해당outcome) 합산
  ↓
주술 세트효과 계산 (런타임):
  같은 DeckMercenary 내 동일 ritual_id + outcome별 피스 수 집계
  RitualSetEffect에서 requiredRitualPieces 이하 효과 전부 적용
```

### 4.2 DPS 계산

```
DeckMercenarySlot 전체 순회 (일반 + 외변 슬롯 모두)
  ↓
equipmentItem != null인 슬롯만 처리
  ↓
equipmentItem.getItemSkills() 조회
  ↓
ItemSkill.skillKey → SkillCoefficient 조회
  (hit_count, casts_per_second, 스탯계수 등 전부 SkillCoefficient에 있음)
  ↓
SkillDamageCalculator.calcDps(skillCoefficient, mercStats) 호출
  ↓
용병별 DPS 합산
```

> **주인공 기본 스킬은 MVP 제외.**
> 무기 슬롯(WEAPON)의 ItemSkill만이 아니라 모든 슬롯의 ItemSkill이 DPS 계산에 포함된다.
> 주인공 고유 스킬(무기 없이 사용하는 스킬)은 추후 별도 스펙으로 추가한다.

### 4.3 EquipmentSetEffect target 처리

```
EquipmentSetEffect.target:
  ALLY  → 덱 전체 용병에게 적용
  SELF  → 해당 장비를 착용한 용병 본인에게만 적용
  ENEMY → 적에게 적용 (저항/속성 감소)

계산기에서:
  ALLY  → DeckBuffCalculator에서 전체 합산
  SELF  → 해당 DeckMercenary의 스탯 계산에만 가산
  ENEMY → 저항깎/속성깎 참고 수치로 표기 (계산식 미반영)
```

---

## 5. 제약 조건

### 5.1 DB 레벨

```sql
-- DeckMercenary: 동일 덱 내 용병 중복 불가
UNIQUE (user_deck_id, mercenary_id)

-- DeckMercenarySlot: 동일 용병 내 슬롯 중복 불가
UNIQUE (deck_mercenary_id, slot)

-- DeckMercenarySlotRitual: 슬롯당 주술 최대 1개
UNIQUE (deck_mercenary_slot_id)
```

### 5.2 서비스 레이어 검증

```
DeckMercenary:
  동일 UserDeck 내 최대 12개

DeckMercenarySlot:
  equipmentItem.equipSlot == DeckMercenarySlot.slot 일치 필수
  외변 슬롯(APP_*)에는 equipmentKind=APPEARANCE 아이템만 착용 가능

DeckMercenarySlotRitual:
  equipmentItem != null인 슬롯에만 주술 등록 가능
  RitualApplicability 기준으로 해당 아이템에 적용 가능한 주술인지 검증
```

---

## 6. UI/UX 흐름

### 6.1 아이템 선택

```
1. 슬롯 클릭
2. 아이템 리스트 조회 (해당 슬롯 타입 기준 필터링)
3. 기본: 주술 없는 아이템 목록 표시
4. "주술 포함" 체크박스 선택 시:
     → 주술 있는 아이템 목록 표시
     → 주술 선택지도 함께 표시
```

### 6.2 주술 선택

```
주술 선택 UI는 카탈로그 기반으로 선택지를 제공한다. 직접 입력 없음.

선택지 생성 규칙:
  RitualApplicability 기준으로 해당 EquipmentItem에 적용 가능한 Ritual 조회
  각 Ritual별로 outcome 선택지 제공:
    "{주술명} (성공)"       → outcome = SUCCESS
    "{주술명} (북두칠성)"   → outcome = GREAT_SUCCESS (대성공 행이 있는 경우만)

예시 (지국천왕 갑옷 선택 시):
  천추 (성공)
  천추 (북두칠성)
  천선 (성공)
  천기 (성공)
  ...
```

---

## 7. 오픈 이슈


| 주인공 기본 스킬 계산 |  장착한 무기에 아이템스킬이 있다면 주인공 스킬 대신 아이템스킬을 사용하여 dps계산
| 동일 용병 중복 허용 여부 | 덱 내 같은 용병 2개 허용할지 여부 — 불가함
