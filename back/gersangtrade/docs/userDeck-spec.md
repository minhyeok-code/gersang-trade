# 유저 덱(UserDeck) 엔티티 설계 문서

## 0. 이 문서의 목적

Claude Code가 이 문서만 읽고 아래를 구현할 수 있도록 작성되었다.

1. `UserDeck` 엔티티 수정
2. `UserDeckMember` 엔티티 수정
3. `UserDeckSpirit`, `UserDeckCheungjin`, `UserDeckJinbeop` 신규 엔티티
4. `UserDeckMemberCharacteristic` 수정 확인
5. `GonmyeongLevelStat`, `GahoLevelStat` 신규 엔티티 및 시더

---

## 1. 설계 원칙

```
용병 레벨: 250 또는 260으로 고정 가정
  → UserDeckMember.mercenaryLevel: Enum(LV_250, LV_260)

데미지 공식/보너스 스탯: 미확정 (테스트서버 대기)
  → 해당 컬럼은 이 문서에서 다루지 않음
  → MVP는 버프 합산까지만 구현
```

---

## 2. UserDeck 수정

기존 엔티티에 아래 항목을 추가한다.

```java
@Entity
@Table(name = "user_decks")
public class UserDeck {

    // ── 기존 컬럼 유지 ────────────────────────────────────
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "is_active", nullable = false)
    private boolean active = false;

    @Column(name = "attr_x_value")
    private Integer attrXValue;

    @Column(name = "total_res_down")
    private Integer totalResDown;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ── 신규 추가 ─────────────────────────────────────────

    /**
     * 덱 이름. 유저가 직접 지정.
     * 예: "화속성 딜덱", "수속성 탱덱"
     */
    @Column(name = "name", length = 50)
    private String name;

    /**
     * 장착된 정령 (최대 2개, 서로 다른 정령)
     * Spirit FK 직접 보유. nullable.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "spirit_slot_1_id")
    private Spirit spiritSlot1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "spirit_slot_2_id")
    private Spirit spiritSlot2;

    /**
     * 층진 강화 레벨 (0~10). null이면 미장착.
     */
    @Column(name = "cheungjin_level")
    private Integer cheungjinLevel;

    /**
     * 진법 속성. null이면 미장착.
     * FIRE / WATER / THUNDER / WIND (EARTH 없음)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "jinbeop_element")
    private Nature jinbeopElement;

    /**
     * 진법 강화 레벨 (0~10). null이면 미장착.
     */
    @Column(name = "jinbeop_level")
    private Integer jinbeopLevel;

    /**
     * 공명 레벨 (1~30). null이면 미장착.
     * 주인공 주스텟 증가 + 전체 용병 데미지 증가.
     */
    @Column(name = "gonmyeong_level")
    private Integer gonmyeongLevel;

    /**
     * 가호 레벨 (1~30). null이면 미장착.
     * 전체 용병 주스텟 증가 + 속성값 증가 + 전체 용병 데미지 증가.
     */
    @Column(name = "gaho_level")
    private Integer gahoLevel;
}
```

**유효성 검증 (서비스 레이어):**
```
- spiritSlot1 == spiritSlot2 불가 (동일 정령 중복 장착 불가)
- jinbeopElement == EARTH 불가 (땅 진법 없음)
- jinbeopElement != null이면 jinbeopLevel도 not null
- cheungjinLevel 범위: 0~10
- jinbeopLevel 범위: 0~10
- gonmyeongLevel 범위: 1~30
- gahoLevel 범위: 1~30
```

---

## 3. UserDeckMember 수정

기존 엔티티에 아래 항목을 추가한다.

```java
@Entity
@Table(name = "user_deck_members")
public class UserDeckMember {

    // ── 기존 컬럼 유지 ────────────────────────────────────
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deck_id", nullable = false)
    private UserDeck deck;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mercenary_id", nullable = false)
    private Mercenary mercenary;

    @Column(name = "slot_index", nullable = false)
    private Integer slotIndex;

    // ── 신규 추가 ─────────────────────────────────────────

    /**
     * 용병 레벨.
     * 250 또는 260으로 고정 가정.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "mercenary_level", nullable = false)
    private MercenaryLevel mercenaryLevel;
}
```

```java
public enum MercenaryLevel {
    LV_250(250),
    LV_260(260);

    private final int value;
    MercenaryLevel(int value) { this.value = value; }
    public int getValue() { return value; }
}
```

---

## 4. UserDeckMemberCharacteristic 수정 확인

기존 엔티티:
```java
private Integer selectedLevel; // 유저가 투자한 레벨
```

이 컬럼이 계산기에서 사용하는 포인트 수와 동일하게 처리된다.

```
전설장수: selectedLevel = passivePoints (0~17)
각성 사천왕: selectedLevel = 포인트 수 (상위는 2개당 레벨 1)
주인공: selectedLevel = 포인트 수 (상위는 2개당 레벨 1)
일반 사천왕/명왕: selectedLevel = 레벨 (0~10)
```

**추가 필요 없음. 기존 구조로 충분.**

---

## 5. UserDeckMemberEquip 수정

기존 엔티티에서 전설장수/명왕 장비만 다루는데,
MVP에서 아이템 특수 효과는 미구현이므로 **현재 구조 유지**.

단, 주석 수정:
```
LEGENDARY_GENERAL → LegendGeneral 용병
MYEONG_KING / MYEONG_KING_AWAKENING → Myungwang 용병
```

---

## 6. GonmyeongLevelStat 엔티티 (신규)

### 6-1. 엔티티

```java
@Entity
@Table(
    name = "gonmyeong_level_stat",
    uniqueConstraints = @UniqueConstraint(columnNames = {"level", "stat_type"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GonmyeongLevelStat {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 공명 레벨 (1~30)
     */
    @Column(nullable = false)
    private Integer level;

    /**
     * 스탯 종류.
     * MAIN_STAT_FLAT : 주인공 주스텟 증가 (flat)
     * DAMAGE_PERCENT : 전체 용병 데미지 증가 (%)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "stat_type", nullable = false)
    private StatType statType;

    /**
     * 해당 레벨의 수치.
     * MAIN_STAT_FLAT: 정수 (예: 100, 150)
     * DAMAGE_PERCENT: 정수 % (예: 1, 2, 3)
     */
    @Column(nullable = false)
    private Integer value;
}
```

### 6-2. 계산 로직 — 주인공 주스텟 결정

공명의 MAIN_STAT_FLAT는 런타임에 주인공 슬롯을 보고 StatType을 결정한다.

```
주인공 주스텟 결정 순서:

1. 사인검 착용 여부 확인
   → 착용 시: 무조건 INTELLECT

2. 사인검 미착용 시, 주인공 속성으로 판단
   화(FIRE)    → STRENGTH
   풍(WIND)    → VITALITY
   뇌(THUNDER) → DEXTERITY
   수(WATER)   → INTELLECT
```

이 로직은 `PlayerCharacterStatResolver`로 분리하여 공명 계산기 / DPS 계산기에서 재사용한다.

### 6-3. 시더 데이터

```
레벨별 행 구성:
  lv 1~14  : MAIN_STAT_FLAT 행만 존재
  lv 15~30 : MAIN_STAT_FLAT + DAMAGE_PERCENT 행 모두 존재

level | stat_type       | value
------+-----------------+------
1     | MAIN_STAT_FLAT  | 100
2     | MAIN_STAT_FLAT  | 110
3     | MAIN_STAT_FLAT  | 120
4     | MAIN_STAT_FLAT  | 130
5     | MAIN_STAT_FLAT  | 150
6     | MAIN_STAT_FLAT  | 160
7     | MAIN_STAT_FLAT  | 170
8     | MAIN_STAT_FLAT  | 180
9     | MAIN_STAT_FLAT  | 190
10    | MAIN_STAT_FLAT  | 250
11    | MAIN_STAT_FLAT  | 260
12    | MAIN_STAT_FLAT  | 270
13    | MAIN_STAT_FLAT  | 280
14    | MAIN_STAT_FLAT  | 290
15    | MAIN_STAT_FLAT  | 300
15    | DAMAGE_PERCENT  | 1
16    | MAIN_STAT_FLAT  | 310
16    | DAMAGE_PERCENT  | 1
17    | MAIN_STAT_FLAT  | 320
17    | DAMAGE_PERCENT  | 1
18    | MAIN_STAT_FLAT  | 330
18    | DAMAGE_PERCENT  | 1
19    | MAIN_STAT_FLAT  | 340
19    | DAMAGE_PERCENT  | 1
20    | MAIN_STAT_FLAT  | 400
20    | DAMAGE_PERCENT  | 2
21    | MAIN_STAT_FLAT  | 410
21    | DAMAGE_PERCENT  | 2
22    | MAIN_STAT_FLAT  | 420
22    | DAMAGE_PERCENT  | 2
23    | MAIN_STAT_FLAT  | 430
23    | DAMAGE_PERCENT  | 2
24    | MAIN_STAT_FLAT  | 440
24    | DAMAGE_PERCENT  | 2
25    | MAIN_STAT_FLAT  | 450
25    | DAMAGE_PERCENT  | 3
26    | MAIN_STAT_FLAT  | 460
26    | DAMAGE_PERCENT  | 3
27    | MAIN_STAT_FLAT  | 470
27    | DAMAGE_PERCENT  | 3
28    | MAIN_STAT_FLAT  | 480
28    | DAMAGE_PERCENT  | 3
29    | MAIN_STAT_FLAT  | 490
29    | DAMAGE_PERCENT  | 3
30    | MAIN_STAT_FLAT  | 550
30    | DAMAGE_PERCENT  | 4
```

---

## 7. GahoLevelStat 엔티티 (신규)

### 7-1. 엔티티

```java
@Entity
@Table(
    name = "gaho_level_stat",
    uniqueConstraints = @UniqueConstraint(columnNames = {"level", "stat_type"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GahoLevelStat {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 가호 레벨 (1~30)
     */
    @Column(nullable = false)
    private Integer level;

    /**
     * 스탯 종류.
     * MAIN_STAT_FLAT : 전체 용병 주스텟 증가 (flat)
     * DAMAGE_PERCENT : 전체 용병 데미지 증가 (%)
     * ELEMENT_VALUE  : 전체 용병 속성값 증가 (ADAPTIVE). 땅속성은 서비스에서 처리.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "stat_type", nullable = false)
    private StatType statType;

    /**
     * 해당 레벨의 수치.
     * MAIN_STAT_FLAT: 정수 (예: 25, 50)
     * DAMAGE_PERCENT: 정수 % (예: 1, 2, 3)
     * ELEMENT_VALUE : 정수 (예: 1, 2, 3)
     */
    @Column(nullable = false)
    private Integer value;
}
```

### 7-2. 계산 로직 — 용병별 주스텟 결정

가호의 MAIN_STAT_FLAT는 용병별로 스킬 계수를 확인해 주스텟을 개별 결정한다.

```
용병 주스텟 결정 순서:
  1. 해당 용병의 스킬 계수 중 가장 높은 스탯 확인
  2. 계수가 없는 용병(탱커 등)은 VITALITY로 처리
```

### 7-3. 계산 로직 — 가호 ELEMENT_VALUE 땅속성 처리

```
ELEMENT_VALUE(ADAPTIVE) 적용 시:
  일반 속성 용병 (화/수/뇌/풍) → value 그대로 적용
  땅속성 용병                  → floor(value / 2) 적용

예시: lv30, ELEMENT_VALUE value=6
  화속성 용병 → 속성값 +6
  땅속성 용병 → 속성값 +3 (floor(6/2))
```

### 7-4. 시더 데이터

```
레벨별 행 구성:
  lv 1~10  : MAIN_STAT_FLAT 행만 존재
  lv 11~15 : MAIN_STAT_FLAT + DAMAGE_PERCENT
  lv 16~30 : MAIN_STAT_FLAT + DAMAGE_PERCENT + ELEMENT_VALUE

level | stat_type       | value
------+-----------------+------
1     | MAIN_STAT_FLAT  | 25
2     | MAIN_STAT_FLAT  | 50
3     | MAIN_STAT_FLAT  | 75
4     | MAIN_STAT_FLAT  | 100
5     | MAIN_STAT_FLAT  | 125
6     | MAIN_STAT_FLAT  | 150
7     | MAIN_STAT_FLAT  | 175
8     | MAIN_STAT_FLAT  | 200
9     | MAIN_STAT_FLAT  | 225
10    | MAIN_STAT_FLAT  | 250
11    | MAIN_STAT_FLAT  | 275
11    | DAMAGE_PERCENT  | 1
12    | MAIN_STAT_FLAT  | 300
12    | DAMAGE_PERCENT  | 1
13    | MAIN_STAT_FLAT  | 325
13    | DAMAGE_PERCENT  | 2
14    | MAIN_STAT_FLAT  | 350
14    | DAMAGE_PERCENT  | 2
15    | MAIN_STAT_FLAT  | 375
15    | DAMAGE_PERCENT  | 3
16    | MAIN_STAT_FLAT  | 400
16    | DAMAGE_PERCENT  | 3
16    | ELEMENT_VALUE   | 1
17    | MAIN_STAT_FLAT  | 425
17    | DAMAGE_PERCENT  | 3
17    | ELEMENT_VALUE   | 1
18    | MAIN_STAT_FLAT  | 450
18    | DAMAGE_PERCENT  | 3
18    | ELEMENT_VALUE   | 2
19    | MAIN_STAT_FLAT  | 475
19    | DAMAGE_PERCENT  | 3
19    | ELEMENT_VALUE   | 2
20    | MAIN_STAT_FLAT  | 500
20    | DAMAGE_PERCENT  | 3
20    | ELEMENT_VALUE   | 3
21    | MAIN_STAT_FLAT  | 525
21    | DAMAGE_PERCENT  | 4
21    | ELEMENT_VALUE   | 3
22    | MAIN_STAT_FLAT  | 550
22    | DAMAGE_PERCENT  | 4
22    | ELEMENT_VALUE   | 3
23    | MAIN_STAT_FLAT  | 575
23    | DAMAGE_PERCENT  | 5
23    | ELEMENT_VALUE   | 3
24    | MAIN_STAT_FLAT  | 600
24    | DAMAGE_PERCENT  | 5
24    | ELEMENT_VALUE   | 3
25    | MAIN_STAT_FLAT  | 625
25    | DAMAGE_PERCENT  | 6
25    | ELEMENT_VALUE   | 3
26    | MAIN_STAT_FLAT  | 650
26    | DAMAGE_PERCENT  | 6
26    | ELEMENT_VALUE   | 4
27    | MAIN_STAT_FLAT  | 675
27    | DAMAGE_PERCENT  | 6
27    | ELEMENT_VALUE   | 4
28    | MAIN_STAT_FLAT  | 700
28    | DAMAGE_PERCENT  | 6
28    | ELEMENT_VALUE   | 5
29    | MAIN_STAT_FLAT  | 725
29    | DAMAGE_PERCENT  | 6
29    | ELEMENT_VALUE   | 5
30    | MAIN_STAT_FLAT  | 750
30    | DAMAGE_PERCENT  | 6
30    | ELEMENT_VALUE   | 6
```

---

## 8. DeckBuffSourceType 추가

```java
public enum DeckBuffSourceType {
    JINBEOP,         // 진법
    CHEUNGJIN,       // 층진
    LEGEND_GENERAL,  // 전설장수 패시브
    GONMYEONG,       // 공명
    GAHO,            // 가호
}
```

---

## 9. 전체 구조 요약

```
UserDeck
├── spiritSlot1: Spirit (nullable)        ← 신규
├── spiritSlot2: Spirit (nullable)        ← 신규
├── cheungjinLevel: Int (nullable)        ← 신규
├── jinbeopElement: Nature (nullable)     ← 신규
├── jinbeopLevel: Int (nullable)          ← 신규
├── gonmyeongLevel: Int (nullable)        ← 신규
├── gahoLevel: Int (nullable)             ← 신규
├── name: String (nullable)               ← 신규
└── UserDeckMember (12개)
      ├── mercenaryLevel: MercenaryLevel  ← 신규
      ├── UserDeckMemberCharacteristic (N개)
      └── UserDeckMemberEquip (0~1개)

마스터 테이블 (시더로 초기 데이터 적재):
├── GonmyeongLevelStat (44행: lv1~14 각 1행 + lv15~30 각 2행)
└── GahoLevelStat      (65행: lv1~10 각 1행 + lv11~15 각 2행 + lv16~30 각 3행)
```

---

## 10. 계산기 연동 흐름

```
UserDeck 로드
  ↓
1. 정령 버프:
   SpiritBuffCalculator.calculate([spiritSlot1, spiritSlot2])

2. 층진 버프:
   cheungjinLevel이 null이면 스킵
   DeckBuffCalculator.calculateCheungjin(cheungjinLevel)

3. 진법 버프:
   jinbeopElement/jinbeopLevel이 null이면 스킵
   DeckBuffCalculator.calculateJinbeop(jinbeopElement, jinbeopLevel)

4. 공명 버프:
   gonmyeongLevel이 null이면 스킵
   GonmyeongBuffCalculator.calculate(gonmyeongLevel, playerCharacter)
     - MAIN_STAT_FLAT: PlayerCharacterStatResolver로 주스텟 결정 후 주인공에 적용
     - DAMAGE_PERCENT: 전체 용병 데미지에 적용

5. 가호 버프:
   gahoLevel이 null이면 스킵
   GahoBuffCalculator.calculate(gahoLevel, deckMembers)
     - MAIN_STAT_FLAT: 용병별 스킬계수 기준 주스텟 개별 결정 후 적용
     - DAMAGE_PERCENT: 전체 용병 데미지에 적용
     - ELEMENT_VALUE : ADAPTIVE 속성값 적용. 땅속성 용병은 floor(value/2) 적용

6. 각 UserDeckMember 순회:
   mercenary.getMercenaryType() 보고 적절한 계산기 호출
   UserDeckMemberCharacteristic.selectedLevel 참조

7. 버프 전체 합산 → 용병별 딜 비중 계산 (공식 확정 후 구현)
```

---

## 11. 미구현 (테스트서버 대기)

```
- 보너스 스탯 (재료 장수 레벨별)
- 주스탯/체력 선택 컬럼
- 데미지 공식 / DPS 계산
- 딜 비중 계산기
```

이 항목들은 테스트서버 오픈 후 별도 스펙 문서로 추가한다.