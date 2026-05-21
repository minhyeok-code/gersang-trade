# 전설장수(LegendGeneral) 엔티티 및 계산 스펙

## 0. 이 문서의 목적

Claude Code가 이 문서만 읽고 아래를 구현할 수 있도록 작성되었다.

1. `LegendGeneral`, `LegendGeneralPassive`, `LegendGeneralCharacteristic`, `CharacteristicEffect` JPA 엔티티
2. `DeckBuffSource`, `DeckBuff` 공통 버프 엔티티 확정
3. 계산 서비스 (`LegendGeneralBuffCalculator`)

---

## 1. 도메인 개요

### 전설장수 타입 분류 (총 15마리)

```
타입 A (5마리): 레벨 기반 패시브 + 특성으로 추가 강화
  - 주몽, 맹획, 노부츠나, 바지라오, 초선

타입 B (10마리): 패시브 없음. 특성 찍어야 버프 발동
  - 보쿠텐, 악바르, 홍길동, 여포, 치요메, 레지나, 화목란, 만선야, 마조, 최무선
```

### 특성 포인트 규칙

```
- 총 17포인트 (레벨 1에 1개, 20레벨마다 1개, 200~260은 10레벨마다 1개)
- 특성별로 포인트를 따로 배분 (예: 특성0에 10, 특성1에 7 → 합계 17)
- 유저 덱에 특성별 배분 포인트 저장 필요
- 포인트 = 5이면 → 해당 특성의 level=5 행의 value만 적용 (누적 합산 아님)
```

### 특성 구조 규칙

```
- CharacteristicType(PASSIVE/ACTIVE) 구분 없음. 모든 특성을 단일 구조로 관리
- 특성 레벨은 항상 1~10 (예외 없음)
- 특성 1개에 여러 효과(CharacteristicEffect)가 묶일 수 있음
- 같은 특성 레벨에 묶인 effect들은 레벨이 같이 오름
- effect별 value는 독립적 (같은 레벨이라도 수치가 다를 수 있음)
- effect별 statType, element, target이 모두 독립적
- SELF 타깃: 해당 전설장수가 장착된 용병 본인에게만 적용 (SKILL_DAMAGE_PERCENT 등)
- ALLY 타깃: 덱 전체 용병에게 적용
- ENEMY 타깃: 몬스터에게 적용 — value는 음수로 저장 (저항/속성 감소)
- 데미지 관련 효과만 수록. 회복 관련(MP_RECOVERY 등)은 계산기 대상 아님
```

---

## 2. StatType 추가 항목

기존 `StatType` enum에 아래 항목을 추가한다.

```java
DAMAGE_PERCENT_GROUND("지상데미지증가"),
DAMAGE_PERCENT_AIR("공중데미지증가"),
```

---

## 3. BuffTarget enum

```java
public enum BuffTarget {
    ALLY,   // 아군 전체
    ENEMY,  // 적 (저항/속성 감소) — value 음수로 저장
    SELF,   // 해당 전설장수가 장착된 용병 본인만
}
```

---

## 4. DeckBuffSource / DeckBuff 확정

전설장수 패시브 버프는 `DeckBuffSource` + `DeckBuff`로 관리한다.

### 4-1. DeckBuffSourceType

```java
public enum DeckBuffSourceType {
    JINBEOP,         // 진법
    CHEUNGJIN,       // 층진
    LEGEND_GENERAL,  // 전설장수 패시브
}
```

### 4-2. DeckBuffSource

```java
@Entity
@Table(name = "deck_buff_source")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeckBuffSource {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeckBuffSourceType sourceType;

    // LEGEND_GENERAL: LegendGeneral.id
    @Column(nullable = false)
    private Long sourceId;

    @Column(nullable = false)
    private String name;                // 표시용. 예: "주몽 패시브"

    @OneToMany(mappedBy = "source", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DeckBuff> buffs = new ArrayList<>();
}
```

### 4-3. DeckBuff

```java
@Entity
@Table(name = "deck_buff")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeckBuff {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id", nullable = false)
    private DeckBuffSource source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatType statType;

    /**
     * 속성 구분.
     * ADAPTIVE: 착용 용병 속성 추종
     * FIRE 등:  해당 속성 용병에게만 적용
     * NONE:     속성 무관
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Element element;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BuffValueType valueType;

    @Column(nullable = false)
    private float value;                // ENEMY 타깃은 음수로 저장

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BuffTarget target;          // ALLY / ENEMY / SELF
}
```

---

## 5. 전설장수 엔티티

### 5-1. LegendGeneral

```java
@Entity
@Table(name = "legend_general")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LegendGeneral {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mercenary_id", nullable = false)
    private Mercenary mercenary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LegendGeneralType type;     // TYPE_A, TYPE_B

    @OneToMany(mappedBy = "legendGeneral", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LegendGeneralPassive> passives = new ArrayList<>();

    @OneToMany(mappedBy = "legendGeneral", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LegendGeneralCharacteristic> characteristics = new ArrayList<>();
}
```

```java
public enum LegendGeneralType {
    TYPE_A,  // 레벨 기반 패시브 + 특성 강화
    TYPE_B,  // 특성으로만 버프 발동
}
```

### 5-2. LegendGeneralPassive (타입 A 전용)

```java
@Entity
@Table(name = "legend_general_passive")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LegendGeneralPassive {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "legend_general_id", nullable = false)
    private LegendGeneral legendGeneral;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatType statType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Element element;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BuffValueType valueType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BuffTarget target;

    // 계산식: value = startValue + floor((level - startLevel) / incrementPerLevels) * incrementValue
    // level < startLevel 이면 버프 없음
    @Column(nullable = true)
    private Integer startLevel;

    @Column(nullable = true)
    private Float startValue;           // 미확정이면 null

    @Column(nullable = true)
    private Integer incrementPerLevels;

    @Column(nullable = true)
    private Float incrementValue;

    @Column(nullable = true)
    private Float maxValue;
}
```

### 5-3. LegendGeneralCharacteristic

```java
@Entity
@Table(
    name = "legend_general_characteristic",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"legend_general_id", "characteristic_index", "level"}
    )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LegendGeneralCharacteristic {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "legend_general_id", nullable = false)
    private LegendGeneral legendGeneral;

    /**
     * 같은 전설장수의 특성 구분용 인덱스 (0부터 시작)
     */
    @Column(nullable = false)
    private int characteristicIndex;

    @Column(nullable = false)
    private int level;                  // 특성 레벨 (1~10)

    @OneToMany(mappedBy = "characteristic", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CharacteristicEffect> effects = new ArrayList<>();
}
```

### 5-4. CharacteristicEffect

```java
@Entity
@Table(name = "characteristic_effect")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CharacteristicEffect {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "characteristic_id", nullable = false)
    private LegendGeneralCharacteristic characteristic;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatType statType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Element element;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BuffValueType valueType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BuffTarget target;          // ALLY / ENEMY / SELF

    @Column(nullable = false)
    private float value;                // ENEMY 타깃은 음수로 저장
}
```

---

## 6. 전설장수별 전체 데이터

특성 데이터 표기 형식:
```
특성 N (characteristicIndex=N):
  effect1: statType / element / valueType / target
  level:  1/  2/ .../ 10   ← 항상 1~10
  value1: x/  x/ .../  x   ← ENEMY 타깃은 음수
  value2: x/  x/ .../  x   ← effect2 수치 (다를 경우)
```

---

### 6-1. 타입 A

#### 주몽

```
LegendGeneralPassive:
  MAGIC_RESISTANCE / NONE / PERCENT_ADD / ALLY
  startLevel=100, startValue=10.0, incrementPerLevels=2, incrementValue=1.0, maxValue=null

특성 0 (characteristicIndex=0):
  effect1: MAGIC_RESISTANCE / NONE / PERCENT_ADD / ALLY
  level:  1/ 2/ 3/ 4/ 5/ 6/ 7/ 8/ 9/10
  value:  1/ 2/ 3/ 4/ 5/ 6/ 8/10/12/15

특성 1 (characteristicIndex=1):
  effect1: SKILL_DAMAGE_PERCENT / NONE / PERCENT_ADD / SELF
  level:  1/ 2/ 3/ 4/ 5/ 6/ 7/ 8/  9/ 10
  value: 10/20/30/40/50/60/75/90/120/150
```

#### 맹획

```
LegendGeneralPassive:
  HITTING_RESISTANCE / NONE / PERCENT_ADD / ALLY
  startLevel=100, startValue=10.0, incrementPerLevels=2, incrementValue=1.0, maxValue=null

특성 0 (characteristicIndex=0):
  effect1: HITTING_RESISTANCE / NONE / PERCENT_ADD / ALLY
  level:  1/ 2/ 3/ 4/ 5/ 6/ 7/ 8/ 9/10
  value:  1/ 2/ 3/ 4/ 5/ 6/ 8/10/12/15

특성 1 (characteristicIndex=1):
  effect1: SKILL_DAMAGE_PERCENT / NONE / PERCENT_ADD / SELF
  level:  1/ 2/ 3/ 4/ 5/ 6/ 7/ 8/  9/ 10
  value: 10/20/30/40/50/60/75/90/120/150
```

#### 노부츠나

```
LegendGeneralPassive (MIN_POWER):
  MIN_POWER / NONE / FLAT / ALLY
  startLevel=100, startValue=null(미확정), incrementPerLevels=null, incrementValue=null, maxValue=null
  ※ 관리자 수동 입력 필요

LegendGeneralPassive (MAX_POWER):
  MAX_POWER / NONE / FLAT / ALLY — 동일 공식

특성 0 (characteristicIndex=0):
  effect1: MIN_POWER / NONE / FLAT / ALLY
  effect2: MAX_POWER / NONE / FLAT / ALLY
  level:   1/  2/  3/  4/  5/  6/  7/  8/  9/ 10
  value:  50/100/150/200/250/300/350/400/450/500  ← effect1, effect2 동일 수치

특성 1 (characteristicIndex=1):
  effect1: SKILL_DAMAGE_PERCENT / NONE / PERCENT_ADD / SELF
  effect2: CRITICAL_CHANCE / NONE / PERCENT_ADD / SELF
  level:   1/ 2/ 3/ 4/ 5/ 6/ 7/ 8/ 9/10
  value1: 10/20/30/40/50/60/70/80/90/100  ← SKILL_DAMAGE_PERCENT
  value2:  4/ 8/12/16/20/24/28/32/36/40  ← CRITICAL_CHANCE
```

#### 바지라오

```
LegendGeneralPassive:
  DAMAGE_PERCENT / NONE / PERCENT_ADD / ALLY
  startLevel=200, startValue=0.5, incrementPerLevels=10, incrementValue=0.5, maxValue=null

특성 0 (characteristicIndex=0):
  effect1: DAMAGE_PERCENT / NONE / PERCENT_ADD / ALLY
  effect2: DAMAGE_PERCENT / EARTH / PERCENT_ADD / ALLY
  level:   1/   2/   3/   4/   5/   6/   7/   8/   9/  10
  value1: 0.5/ 1.0/ 1.5/ 2.0/ 2.5/ 3.0/ 3.5/ 4.0/ 5.0/ 6.0  ← NONE
  value2:  1/   1/   1/   2/   2/   3/   3/   4/   4/   5     ← EARTH

특성 1 (characteristicIndex=1):
  effect1: SKILL_DAMAGE_PERCENT / NONE / PERCENT_ADD / SELF
  level:  1/ 2/ 3/ 4/ 5/ 6/ 7/ 8/ 9/10
  value: 10/20/30/40/50/60/70/80/90/100
```

#### 초선

```
LegendGeneralPassive:
  MP_RECOVERY / NONE / FLAT / ALLY
  startLevel=100, startValue=10.0, incrementPerLevels=2, incrementValue=1.0, maxValue=null
  ※ 회복 관련 — 계산기 대상 아님. 저장만 함

특성 0 (characteristicIndex=0):
  ※ 마력회복 관련 — 계산기 대상 아님. 수록하지 않음

특성 1 (characteristicIndex=1):
  effect1: SKILL_DAMAGE_PERCENT / NONE / PERCENT_ADD / SELF
  level:  1/ 2/ 3/ 4/ 5/ 6/ 7/ 8/  9/ 10
  value: 10/20/30/40/50/60/75/90/120/150
```

---

### 6-2. 타입 B

#### 보쿠텐

```
특성 0 (characteristicIndex=0):
  effect1: CRITICAL_CHANCE / NONE / PERCENT_ADD / ALLY
  level:  1/2/3/4/5/6/7/8/9/10
  value:  1/2/2/3/3/4/4/5/6/ 7

특성 1 (characteristicIndex=1):
  effect1: SKILL_DAMAGE_PERCENT / NONE / PERCENT_ADD / SELF
  level:  1/ 2/ 3/ 4/ 5/ 6/ 7/ 8/  9/ 10
  value: 10/20/30/40/50/60/75/90/120/150
```

#### 악바르

```
특성 0 (characteristicIndex=0):
  effect1: ELEMENT_VALUE / ADAPTIVE / FLAT / ENEMY  — 적 속성값 감소 (음수)
  level:   1/ 2/ 3/ 4/ 5/ 6/ 7/ 8/ 9/10
  value:  -1/-1/-2/-2/-2/-3/-3/-4/-4/-5

특성 1 (characteristicIndex=1):
  effect1: ELEMENT_VALUE / ADAPTIVE / FLAT / ALLY   — 파티 속성값 증가
  level:  1/2/3/4/5/6/7/8/9/10
  value:  1/1/2/2/2/3/3/4/4/ 5
```

#### 홍길동

```
특성 0 (characteristicIndex=0):
  effect1: DAMAGE_PERCENT / WIND / PERCENT_ADD / ALLY
  level:  1/2/3/4/5/6/7/8/9/10
  value:  1/1/2/2/3/3/4/5/6/ 7

특성 1 (characteristicIndex=1):
  effect1: SKILL_DAMAGE_PERCENT / NONE / PERCENT_ADD / SELF
  effect2: HITTING_RESISTANCE / NONE / PERCENT_ADD / ENEMY
  level:    1/ 2/ 3/ 4/ 5/ 6/ 7/ 8/ 9/ 10
  value1:  10/20/30/40/50/60/70/80/90/100  ← SKILL_DAMAGE_PERCENT
  value2:  -1/-1/-2/-2/-3/-3/-5/-5/-7/-10  ← HITTING_RESISTANCE (음수)
```

#### 여포

```
특성 0 (characteristicIndex=0):
  effect1: DAMAGE_PERCENT / FIRE / PERCENT_ADD / ALLY
  level:  1/2/3/4/5/6/7/8/9/10
  value:  1/1/2/2/3/3/4/5/6/ 7

특성 1 (characteristicIndex=1):
  effect1: SKILL_DAMAGE_PERCENT / NONE / PERCENT_ADD / SELF
  effect2: MAGIC_RESISTANCE / NONE / PERCENT_ADD / ENEMY
  level:    1/ 2/ 3/ 4/ 5/  6/  7/  8/  9/ 10
  value1:  10/20/30/40/50/ 60/ 70/ 80/ 90/100  ← SKILL_DAMAGE_PERCENT
  value2:  -1/-2/-3/-4/-5/ -6/ -8/-10/-12/-15  ← MAGIC_RESISTANCE (음수)
```

#### 치요메

```
특성 0 (characteristicIndex=0):
  effect1: MAGIC_RESISTANCE / NONE / PERCENT_ADD / ENEMY
  level:   1/ 2/ 3/ 4/ 5/ 6/ 7/  8/  9/ 10
  value:  -1/-2/-3/-4/-5/-6/-8/-10/-12/-15  ← 음수

특성 1 (characteristicIndex=1):
  effect1: DAMAGE_PERCENT / WATER / PERCENT_ADD / ALLY
  level:  1/2/3/4/5/6/7/8/9/10
  value:  1/1/2/2/3/3/4/5/6/ 7
```

#### 레지나

```
특성 0 (characteristicIndex=0):
  effect1: DAMAGE_PERCENT_GROUND / NONE / PERCENT_ADD / ALLY
  effect2: DAMAGE_PERCENT_AIR / NONE / PERCENT_ADD / ALLY
  level:   1/ 2/ 3/ 4/ 5/ 6/ 7/ 8/ 9/10
  value1:  1/ 1/ 2/ 2/ 3/ 3/ 4/ 5/ 6/ 7  ← DAMAGE_PERCENT_GROUND
  value2:  1/ 2/ 3/ 4/ 5/ 6/ 7/ 8/ 9/10  ← DAMAGE_PERCENT_AIR

특성 1 (characteristicIndex=1):
  effect1: SKILL_DAMAGE_PERCENT / NONE / PERCENT_ADD / SELF
  effect2: ATTACK_SPEED / NONE / PERCENT_ADD / ALLY
  level:   1/ 2/ 3/ 4/ 5/ 6/ 7/ 8/ 9/10
  value1: 10/20/30/40/50/60/70/80/90/100  ← SKILL_DAMAGE_PERCENT
  value2:  2/ 3/ 4/ 5/ 7/12/15/30/40/50  ← ATTACK_SPEED
```

#### 화목란

```
특성 0 (characteristicIndex=0):
  effect1: MAGIC_RESISTANCE / NONE / PERCENT_ADD / ENEMY
  level:   1/ 2/ 3/ 4/ 5/ 6/ 7/ 8/ 9/10
  value:  -1/-2/-3/-4/-5/-6/-7/-8/-9/-10  ← 음수

특성 1 (characteristicIndex=1):
  effect1: SKILL_DAMAGE_PERCENT / NONE / PERCENT_ADD / SELF
  level:  1/ 2/ 3/ 4/ 5/ 6/ 7/ 8/  9/ 10
  value: 10/20/30/40/50/60/75/90/120/150
```

#### 만선야

```
특성 0 (characteristicIndex=0):
  effect1: DAMAGE_PERCENT / THUNDER / PERCENT_ADD / ALLY
  level:  1/2/3/4/5/6/7/8/9/10
  value:  1/1/2/2/3/3/4/5/6/ 7

특성 1 (characteristicIndex=1):
  effect1: SKILL_DAMAGE_PERCENT / NONE / PERCENT_ADD / SELF
  level:  1/ 2/ 3/ 4/ 5/ 6/ 7/ 8/  9/ 10
  value: 10/20/30/40/50/60/75/90/120/150
```

#### 마조

```
특성 0 (characteristicIndex=0):
  effect1: MAGIC_RESISTANCE / NONE / PERCENT_ADD / ENEMY
  level:   1/ 2/ 3/ 4/ 5/ 6/ 7/  8/  9/ 10
  value:  -1/-2/-3/-4/-5/-6/-8/-10/-12/-15  ← 음수

특성 1 (characteristicIndex=1):
  effect1: SKILL_DAMAGE_PERCENT / NONE / PERCENT_ADD / SELF
  level:  1/ 2/ 3/ 4/ 5/ 6/ 7/ 8/  9/ 10
  value: 10/20/30/40/50/60/75/90/120/150
```

#### 최무선

```
특성 0 (characteristicIndex=0):
  effect1: MAGIC_RESISTANCE / NONE / PERCENT_ADD / ENEMY
  level:   1/ 2/ 3/ 4/ 5/ 6/ 7/  8/  9/ 10
  value:  -1/-2/-3/-4/-5/-6/-8/-10/-12/-15  ← 음수

특성 1 (characteristicIndex=1):
  effect1: SKILL_DAMAGE_PERCENT / NONE / PERCENT_ADD / SELF
  level:  1/ 2/ 3/ 4/ 5/ 6/ 7/ 8/  9/ 10
  value: 10/20/30/40/50/60/75/95/120/150
```

---

## 7. 계산 서비스

### LegendGeneralBuffCalculator

```java
@Service
public class LegendGeneralBuffCalculator {

    /**
     * 전설장수 특성 버프를 계산한다.
     * ALLY/ENEMY 타깃 효과만 반환 (덱 전체 버프용).
     * SELF 타깃은 calculateSelfBuffs()로 별도 조회.
     *
     * @param legendGeneral  전설장수
     * @param mercenaryLevel 해당 전설장수의 현재 레벨 (타입 A 패시브 계산용)
     * @param pointsMap      characteristicIndex → 배분 포인트 Map
     * @return BuffKey(statType, element, target)별 합산 버프 Map
     */
    public Map<BuffKey, Float> calculate(
            LegendGeneral legendGeneral,
            int mercenaryLevel,
            Map<Integer, Integer> pointsMap) {

        Map<BuffKey, Float> result = new HashMap<>();

        // 1. 레벨 기반 패시브 (타입 A만 존재)
        for (LegendGeneralPassive passive : legendGeneral.getPassives()) {
            float value = calcLevelBasedValue(passive, mercenaryLevel);
            if (value > 0) {
                BuffKey key = new BuffKey(passive.getStatType(), passive.getElement(), passive.getTarget());
                result.merge(key, value, Float::sum);
            }
        }

        // 2. 특성 효과 — 특성별 배분 포인트에 해당하는 레벨 행의 값만 적용
        legendGeneral.getCharacteristics().stream()
            .filter(c -> {
                Integer points = pointsMap.get(c.getCharacteristicIndex());
                return points != null && points > 0 && c.getLevel() == points;
            })
            .flatMap(c -> c.getEffects().stream())
            .filter(e -> e.getTarget() != BuffTarget.SELF)
            .forEach(e -> {
                BuffKey key = new BuffKey(e.getStatType(), e.getElement(), e.getTarget());
                result.merge(key, e.getValue(), Float::sum);
            });

        return result;
    }

    /**
     * 장착 용병 본인(SELF)에게만 적용되는 효과를 계산한다.
     * DPS 계산기에서 해당 용병 dps 계산 시 사용.
     *
     * @param legendGeneral 전설장수
     * @param pointsMap     characteristicIndex → 배분 포인트 Map
     * @return BuffKey(statType, element, SELF)별 합산 버프 Map
     */
    public Map<BuffKey, Float> calculateSelfBuffs(
            LegendGeneral legendGeneral,
            Map<Integer, Integer> pointsMap) {

        Map<BuffKey, Float> result = new HashMap<>();

        legendGeneral.getCharacteristics().stream()
            .filter(c -> {
                Integer points = pointsMap.get(c.getCharacteristicIndex());
                return points != null && points > 0 && c.getLevel() == points;
            })
            .flatMap(c -> c.getEffects().stream())
            .filter(e -> e.getTarget() == BuffTarget.SELF)
            .forEach(e -> {
                BuffKey key = new BuffKey(e.getStatType(), e.getElement(), e.getTarget());
                result.merge(key, e.getValue(), Float::sum);
            });

        return result;
    }

    private float calcLevelBasedValue(LegendGeneralPassive passive, int level) {
        if (passive.getStartLevel() == null || passive.getStartValue() == null) {
            return 0f;  // 미확정 데이터 — 계산 스킵
        }
        if (level < passive.getStartLevel()) return 0f;

        float base = passive.getStartValue();
        int steps = (level - passive.getStartLevel()) / passive.getIncrementPerLevels();
        float value = base + steps * passive.getIncrementValue();

        if (passive.getMaxValue() != null) {
            value = Math.min(value, passive.getMaxValue());
        }
        return value;
    }

    // StatType + Element + BuffTarget 조합 키
    public record BuffKey(StatType statType, Element element, BuffTarget target) {}
}
```

---

## 8. 유저 덱 특성 포인트 저장

```
legendGeneralId: Long
characteristicPoints: List<{characteristicIndex: Int, points: Int}>
// Σ points <= 17
// points = 0이면 해당 특성 미배분 → 버프 없음
```

---

## 9. DPS 계산 구조

### 용어 정의

```
순수화력 = 몬스터 속성 보정 없이 모든 용병 DPS 합산
몬스터 대비 화력 = 몬스터 지정 시 용병별 속성 보정 개별 적용 후 합산
```

### 용병 1마리 DPS 계산식

```
# 1. 기본 데미지
base_damage = 스탯 × 스킬계수

# 2. 데미지 퍼센트 버프 합산 (가산)
#    - ALLY 타깃이면서 element=NONE 또는 해당 용병 속성인 버프
#    - SELF 타깃 SKILL_DAMAGE_PERCENT (해당 용병에 장착된 전설장수 것만)
damage_percent_sum = Σ DAMAGE_PERCENT 버프 + SKILL_DAMAGE_PERCENT(SELF)

# 3. 버프 적용 데미지 (가산 후 1회 곱)
buffed_damage = base_damage × (1 + damage_percent_sum / 100)

# 4. DPS
dps = buffed_damage × casts_per_second
```

> ※ `casts_per_second`가 null인 용병은 DPS = 0으로 처리하고 UI에 "측정값 없음" 표기

### 순수화력

```
순수화력 = Σ 모든 용병 dps
```

### 몬스터 대비 화력

```
# 용병별 속성 보정률
속성보정률 = (3 × 내속성값 - 몬스터속성값) / 2  (%)

# 용병별 몬스터 대비 DPS
nature_dps_per_merc = dps × (1 + 속성보정률 / 100)

# 전체 합산
몬스터 대비 화력 = Σ 모든 용병 nature_dps_per_merc
```

### 저항깎 / 속성깎 표기 (참고 수치, 계산식에 미반영)

```
저항깎(마법) = Σ MAGIC_RESISTANCE / ENEMY 효과  (음수 합산 → UI에 절댓값 표시)
저항깎(타격) = Σ HITTING_RESISTANCE / ENEMY 효과 (음수 합산 → UI에 절댓값 표시)
속성깎       = Σ ELEMENT_VALUE / ENEMY 효과      (음수 합산 → UI에 절댓값 표시)
```

---

## 10. 주의사항

```
1. 노부츠나 startValue / incrementPerLevels / incrementValue 미확정
   → LegendGeneralPassive 행 생성하되 null 저장
   → 계산기에서 null 감지 시 해당 패시브 스킵
   → 관리자 수동 입력 후 적용

2. 악바르: 특성 0(ENEMY, 음수) / 특성 1(ALLY, 양수) 각각 별도 characteristicIndex

3. ENEMY 타깃 value는 전부 음수로 저장
   해당 statType: MAGIC_RESISTANCE, HITTING_RESISTANCE, ELEMENT_VALUE
   UI 표시 시 절댓값으로 변환

4. SKILL_DAMAGE_PERCENT는 target=SELF
   → 해당 전설장수가 장착된 용병 본인 DPS 계산에만 적용

5. DAMAGE_PERCENT에 element가 있으면 해당 속성 용병에게만 적용
   예: 홍길동 DAMAGE_PERCENT element=WIND → WIND 속성 용병만

6. 특성 포인트 적용 방식: 누적 합산 아님
   배분 포인트 수 = 해당 특성의 현재 레벨
   filter 조건: c.getLevel() == pointsMap.get(c.getCharacteristicIndex())
   points = 0이면 해당 특성 버프 없음

7. 초선 특성 0: 마력회복 관련 → 계산기 완전 제외
   치요메: 특성 0(MAGIC_RESISTANCE/ENEMY) + 특성 1(DAMAGE_PERCENT/WATER/ALLY)

8. casts_per_second null인 용병은 DPS = 0 처리, UI에 "측정값 없음" 표기
``` 