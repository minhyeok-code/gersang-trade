# 덱 · 용병 장비 슬롯 설계

## 개요

유저가 용병 덱을 구성하고, 각 용병의 장비 세팅을 저장하는 기능 설계.

설계 원칙:
- 모든 슬롯(일반 + 외변)의 `ItemStat` → 스탯 계산에 포함
- 모든 슬롯(일반 + 외변)의 `ItemSkill` → DPS 계산에 포함
- 세트효과 / 주술 세트효과 → 런타임 계산 (DB 저장 안 함)
- 슬롯은 `EquipSlot` 통합 enum 하나로 관리

---

## 슬롯 종류

### 일반 장비 슬롯 (8종 9슬롯)

| 슬롯 | 비고 |
|------|------|
| 투구 | |
| 갑옷 | |
| 무기 | |
| 신발 | |
| 장갑 | |
| 요대 | |
| 부적 | |
| 반지 | 2슬롯 (반지1 / 반지2) |

### 외변 슬롯 (9종 9슬롯)

기운 / 투구 / 갑옷 / 무기 / 무신 / 귀걸이 / 목걸이 / 팔찌 / 각반

- 일반 장비와 **별도 슬롯** — 동시 착용 가능
- `EquipmentItem.equipmentKind = APPEARANCE`인 아이템만 착용 가능

---

## Enum 정의

```java
// 일반 + 외변 슬롯 통합. APP_ 접두사로 외변 구분.
enum EquipSlot {
    // 일반 슬롯 (8종 9슬롯)
    HELMET, ARMOR, WEAPON, SHOES, GLOVES,
    BELT, CHARM, RING_1, RING_2,

    // 외변 슬롯 (9종 9슬롯)
    APP_SPIRIT,    // 기운
    APP_HELMET,    // 투구 (외변)
    APP_ARMOR,     // 갑옷 (외변)
    APP_WEAPON,    // 무기 (외변)
    APP_WAR_GOD,   // 무신
    APP_EARRING,   // 귀걸이
    APP_NECKLACE,  // 목걸이
    APP_BRACELET,  // 팔찌
    APP_GREAVES    // 각반
}

enum RitualOutcome {
    SUCCESS,       // 일반 성공
    GREAT_SUCCESS  // 대성공 (북두칠성 등)
}
```

---

## 엔티티 구조

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

### UserDeck
유저가 저장한 덱 구성. 여러 개 저장 가능.

| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long (PK) | |
| user | User (FK) | |
| name | String | 덱 이름 |

### DeckMercenary
덱 안의 용병 목록. 최대 12명.

| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long (PK) | |
| userDeck | UserDeck (FK) | |
| mercenary | Mercenary (FK) | |

UNIQUE `(user_deck_id, mercenary_id)` — 동일 덱 내 같은 용병 중복 불가

### DeckMercenarySlot
각 용병의 슬롯별 착용 아이템.

| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long (PK) | |
| deckMercenary | DeckMercenary (FK) | |
| slot | EquipSlot | |
| equipmentItem | EquipmentItem (FK, nullable) | 미착용 시 null |

UNIQUE `(deck_mercenary_id, slot)`

### DeckMercenarySlotRitual
슬롯에 착용된 아이템의 주술 정보. 주술이 없으면 row 없음.

| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long (PK) | |
| deckMercenarySlot | DeckMercenarySlot (FK, UNIQUE) | 슬롯당 최대 1개 |
| ritual | Ritual (FK) | |
| outcome | RitualOutcome | SUCCESS / GREAT_SUCCESS |

### EquipmentItem (기존 엔티티 변경)

`equipSlot: EquipSlot` 필드 추가 (nullable).
- `equipmentKind = APPEARANCE`이면 `APP_*` 값, `NORMAL`이면 일반 슬롯 값
- 수동 관리

---

## 계산기 연동 흐름

### 스탯 계산

```
DeckMercenarySlot 전체 순회 (일반 + 외변 모두)
  ↓
equipmentItem != null인 슬롯만 처리
  ↓
equipmentItem.getItemStats() → StatType별 합산
  ↓
장비 세트효과 (런타임):
  equipmentItem.setId 기준 동일 DeckMercenary 내 피스 수 집계
  EquipmentSetEffect에서 requiredPieces 이하 효과 전부 적용
  ↓
주술 스탯 계산:
  DeckMercenarySlotRitual != null인 슬롯만
  ritual.getItemStats(outcome) 합산
  ↓
주술 세트효과 (런타임):
  동일 DeckMercenary 내 (ritual_id, outcome)별 피스 수 집계
  RitualSetEffect.requiredRitualPieces 이하 효과 전부 적용
```

### DPS 계산

```
DeckMercenarySlot 전체 순회 (일반 + 외변 모두)
  ↓
equipmentItem != null인 슬롯만 처리
  ↓
equipmentItem.getItemSkills() 조회
  ├── ItemSkill 있음 → ItemSkill 기준으로 DPS 계산 (주인공 기본 스킬 대체)
  └── ItemSkill 없음 → 주인공 기본 스킬로 DPS 계산
  ↓
ItemSkill.skillKey → SkillCoefficient 조회
  (hit_count, casts_per_second, 스탯계수 등)
  ↓
SkillDamageCalculator.calcDps(skillCoefficient, mercStats) 호출
  ↓
용병별 DPS 합산
```

### 세트효과 target 처리

`EquipmentSetEffect.target` 기준:

| target | 적용 대상 | 계산기 처리 |
|--------|----------|------------|
| ALLY | 덱 전체 용병 | DeckBuffCalculator에서 전체 합산 |
| SELF | 해당 장비 착용 용병 본인 | 해당 DeckMercenary 스탯에만 가산 |
| ENEMY | 적 (저항/속성 감소) | 저항깎/속성깎 참고 수치로 표기 (계산식 미반영) |

---

## 세트효과 · 주술 세트효과

저장하지 않음. 런타임에 계산.

### 장비 세트효과
1. `DeckMercenarySlot`에서 착용 아이템 목록 조회
2. `EquipmentItem.setId` 기준 동일 세트 피스 수 집계
3. `EquipmentSet` / `EquipmentSetPiece` 카탈로그에서 세트 보너스 적용

### 주술 세트효과
- 아이템 하나당 주술 1개만 적용
- 한 용병 내에서만 적용 (덱 전체 합산 없음)
- `RitualSetEffect` 엔티티 활용 (크롤링으로 적재)
- `element` 필드로 속성 구분 (NONE=공통, non-NONE=특정 속성)

---

## 제약 조건

### DB 레벨
- `DeckMercenary`: UNIQUE `(user_deck_id, mercenary_id)`
- `DeckMercenarySlot`: UNIQUE `(deck_mercenary_id, slot)`
- `DeckMercenarySlotRitual`: UNIQUE `(deck_mercenary_slot_id)`

### 서비스 레이어 검증
- `DeckMercenary`: 동일 `UserDeck` 내 최대 12개
- `DeckMercenarySlot`: `equipmentItem.equipSlot == slot` 일치 필수
- `DeckMercenarySlot`: `APP_*` 슬롯에는 `equipmentKind=APPEARANCE` 아이템만 착용 가능
- `DeckMercenarySlotRitual`: `equipmentItem != null`인 슬롯에만 등록 가능
- `DeckMercenarySlotRitual`: `RitualApplicability` 기준으로 해당 아이템에 적용 가능한 주술인지 검증
