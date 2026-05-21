# 스킬 계수 엔티티 스펙

## 0. 이 문서의 목적

Claude Code가 이 문서만 읽고 아래를 구현할 수 있도록 작성되었다.

1. `StatType` enum 확장 — DPS 계산용 스탯 타입 추가
2. `SkillType` enum 신규 생성 — INSTANT / PERSISTENT 구분
3. `Confidence` enum 신규 생성
4. `MercenarySkill` 엔티티 수정 — `skillKey` 컬럼 추가
5. `Item` 엔티티 수정 — `itemKey` 컬럼 추가
6. `ItemSkill` 엔티티 수정 — `skillKey` 컬럼 추가
7. `MercenaryStat` 엔티티 수정 — 기본 스탯 컬럼 추가
8. `SkillCoefficient` 엔티티 신규 생성
9. DPS 계산 서비스
10. 시드 데이터 적재 전략
11. TODO — 향후 확장 기능

---

## 1. 배경 및 데이터 소스

### 1.1 데이터 소스 역할 분리

```
거상짱 크롤러   → 용병 기본정보 / 스탯(base_str 등) / 재료 / 장비
거니버스 HTML   → 스킬명 / skill_key / 스킬 계수 (수동 저장)
직접 측정       → casts_per_second / tick_interval_ms (시전속도)
```

거니버스 HTML의 `self.__next_f.push()` 내부 데이터에 용병별 스킬 계수가 포함되어 있다.
계수는 `calc_details` 문자열로 제공되며, 파싱을 통해 추출한다 (3항 참고).

### 1.2 DPS 계산 공식

```
실제 스탯 = base_stat + 레벨 스탯 + 보너스 스탯

원데미지 = coef_str×STR + coef_dex×DEX + coef_vit×VIT
         + coef_int×INT + coef_atk×ATK + coef_lvl×LVL

INSTANT:
  DPS = 원데미지 × hit_count × casts_per_second

PERSISTENT:
  DPS = 원데미지 / (tick_interval_ms / 1000.0)
```

### 1.3 레벨 스탯 상수 (모든 용병 공통)

거상짱 레벨 테이블 기준. DB 저장 없이 코드 상수로 관리한다.

```java
public enum MercenaryLevel {
    LV_250(2256),
    LV_260(2466);

    private final int levelStat;
}
```

### 1.4 측정 우선순위

직접 측정(`casts_per_second`, `tick_interval_ms`) 대상:
- 사천왕 일반/각성 (8마리)
- 명왕 일반/각성 (8마리)
- 전설장수 15마리
- 사인검

측정 조건 고정:
- 공격속도 버프 없음
- 자동전투 기준
- 동일 레벨 기준

---

## 2. StatType enum 확장

기존 `StatType` enum에 아래 항목을 **추가**한다.
기존 항목은 절대 수정하지 않는다 (DB 저장값 파괴적 변경 금지).

```java
// DPS 계산용 — 스킬 계수 곱셈 대상 스탯
STRENGTH("힘"),
DEXTERITY("민첩"),
VITALITY("생명"),
INTELLECT("지력"),
ATTACK_POWER("공격력"),
```

> ⚠️ `legend-general-spec.md`에서 이미 추가된 항목 확인 후 중복 추가하지 않는다.
> 기존에 있는 항목: `DAMAGE_PERCENT_GROUND`, `DAMAGE_PERCENT_AIR`

---

## 3. SkillType enum 신규 생성

```java
public enum SkillType {
    INSTANT,    // 일반 시전형 — casts_per_second로 DPS 계산
    PERSISTENT, // 지속/장판형 — tick_interval_ms로 DPS 계산
}
```

**초기 적재 전략:**
모든 스킬을 일단 `INSTANT`로 저장한다.
PERSISTENT 스킬은 소수이므로 적재 후 수동으로 변경한다.

**PERSISTENT 대상 스킬 목록:**
- 팔괘진 (마조)
- 대지포식 — 소환물/가시덤불 (레지나)
- 용오름 소환 (홍길동)
- 모래병사 소환 (악바르)

### 타입별 DPS 계산 방식

```
INSTANT:
  DPS = 원데미지 × hit_count × casts_per_second

PERSISTENT:
  DPS = 원데미지 / (tick_interval_ms / 1000.0)
```

---

## 4. Confidence enum 신규 생성

거니버스 원본 confidence 값을 enum으로 관리한다.

```java
public enum Confidence {
    VERIFIED,
    HIGH,
    MEDIUM,
    LOW
}
```

파싱 시 대소문자 처리:
```java
Confidence.valueOf(raw.toUpperCase())
```

---

## 5. calc_details 파싱 규칙

거니버스 원본 데이터에는 스킬 계수가 `calc_details` 문자열로 제공된다.
적재 시 이를 파싱하여 각 계수 컬럼에 저장한다.

### 5.1 원본 형식

```
"지(1550×25) + 공(596×25) + 렙(260×30)"
"힘(1600×131) + 공(796×147)"
"체(2100×25) + 공(696×20) + 렙(260×20)"
"힘(1000×45) + 민(1000×65) + 체(1000×40) + 지(1000×70) + 공(556×50) + 렙(260×30)"
```

### 5.2 파싱 규칙

```
형식: 한글약어(기준스탯×계수)

한글약어 → DB 컬럼 매핑:
  "힘"  → coef_str
  "민"  → coef_dex
  "체"  → coef_vit
  "지"  → coef_int
  "공"  → coef_atk
  "렙"  → coef_lvl

괄호 앞 숫자 (기준스탯): 거니버스 계산용 샘플값 — 무시
× 뒤 숫자 (계수): DB에 저장
언급되지 않은 스탯: 0.0f로 저장
```

### 5.3 파싱 예시

```
"지(1550×25) + 공(596×25) + 렙(260×30)"
→ coef_str=0, coef_dex=0, coef_vit=0, coef_int=25, coef_atk=25, coef_lvl=30

"힘(1600×131) + 공(796×147)"
→ coef_str=131, coef_dex=0, coef_vit=0, coef_int=0, coef_atk=147, coef_lvl=0
```

### 5.4 파싱 구현 예시

```java
private Map<String, Float> parseCalcDetails(String calcDetails) {
    Map<String, Float> result = new HashMap<>();
    // "힘(1600×131)" 패턴 추출
    Pattern pattern = Pattern.compile("([힘민체지공렙])\\((\\d+)×([\\d.]+)\\)");
    Matcher matcher = pattern.matcher(calcDetails);
    while (matcher.find()) {
        String statKey = matcher.group(1);
        float coef = Float.parseFloat(matcher.group(3));
        result.put(statKey, coef);
    }
    return result;
}
```

---

## 6. MercenarySkill 엔티티 수정

기존 `MercenarySkill`에 `skill_key` 컬럼을 추가한다.

```java
@Entity
@Table(
        name = "mercenary_skills",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_mercenary_skills_merc_skill",
                columnNames = {"mercenary_id", "skill_name"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MercenarySkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mercenary_id", nullable = false)
    private Mercenary mercenary;

    @Column(name = "skill_name", nullable = false, length = 100)
    private String skillName;

    /**
     * 거니버스 내부 스킬 식별 키 — 예: "tlsxhfb", "qldghk"
     * 거상짱 크롤링 시에는 null. 거니버스 데이터 적재 후 채워진다.
     */
    @Column(name = "skill_key", length = 100)
    private String skillKey;

    @Builder
    public MercenarySkill(Mercenary mercenary, String skillName, String skillKey) {
        this.mercenary = mercenary;
        this.skillName = skillName;
        this.skillKey = skillKey;
    }

    public void updateSkillKey(String skillKey) {
        this.skillKey = skillKey;
    }
}
```

---

## 7. Item 엔티티 수정

기존 `Item`에 `item_key` 컬럼을 추가한다.
거니버스 `image_path`의 마지막 세그먼트를 `item_key`로 사용한다.

```java
/**
 * 거니버스 image_path 마지막 세그먼트 기반 아이템 식별 키.
 * 예: "tkdlsrja-tn" (뇌속성 사인검)
 */
@Column(name = "item_key", length = 100, unique = true)
private String itemKey;

public void updateItemKey(String itemKey) {
    this.itemKey = itemKey;
}
```

### item_key 추출 규칙

```java
// 예외 없이 모든 아이템 동일 규칙
String itemKey = imagePath.substring(imagePath.lastIndexOf('/') + 1);
```

### 아이템 key 목록

| 아이템명 | item_key |
|---|---|
| 뇌속성 사인검 | `tkdlsrja-tn` |
| 화속성 사인검 | `tkdlsrja-ghk` |
| 수속성 사인검 | `tkdlsrja-xh` |
| 풍속성 사인검 | `tkdlsrja-shl` |
| 지속성 사인검 | `tkdlsrja-vnd` |
| 종리권의 인형 | `whdflrnjsdmldlsgud` |
| 조국구의 비검 | `whrnrrndmlqlrja` |
| 호선의 인형 | `ghtjsdmldlsgud` |
| 여동빈의 비검 | `duehdqlsdmlqlrja` |
| 철괴리의 인형 | `cjfrhlfldmldlsgud` |
| 장과로의 인형 | `wkdrhkfhdmldlsgud` |
| 한상자의 인형 | `gkstkdwkdmldlsgud` |
| 람채화의 인형 | `fkacoghkdmldlsgud` |

> ⚠️ 사인검 5종은 거상짱 크롤러가 속성별로 5행으로 저장한다는 전제.
> ItemSkill 5행 + SkillCoefficient 5행 생성.
> casts_per_second는 속성 무관하게 동일하므로 1회 측정 후 5행 모두 동일값 입력.

---

## 8. ItemSkill 엔티티 수정

```java
@Entity
@Table(
        name = "item_skills",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_item_skills_item_skill",
                columnNames = {"item_id", "skill_name"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ItemSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(name = "skill_name", nullable = false, length = 100)
    private String skillName;

    /**
     * 거니버스 내부 스킬 식별 키 — 예: "dmsgktnrkd"
     * 거상짱 크롤링 시에는 null. 거니버스 데이터 적재 후 채워진다.
     */
    @Column(name = "skill_key", length = 100)
    private String skillKey;

    @Builder
    public ItemSkill(Item item, String skillName, String skillKey) {
        this.item = item;
        this.skillName = skillName;
        this.skillKey = skillKey;
    }

    public void updateSkillKey(String skillKey) {
        this.skillKey = skillKey;
    }
}
```

---

## 9. MercenaryStat 엔티티 수정

기존 `MercenaryStat`에 레벨 1 기본 스탯 컬럼을 추가한다.
거상짱 크롤러가 아래 형식으로 제공한다:

```html
힘: 100 민첩: 55 생명: 120 지력: 225
타저: 30% 마저: 60%
```

```java
/** 레벨 1 기본 힘 */
@Column(name = "base_str")
private Integer baseStr;

/** 레벨 1 기본 민첩 */
@Column(name = "base_dex")
private Integer baseDex;

/** 레벨 1 기본 생명 */
@Column(name = "base_vit")
private Integer baseVit;

/** 레벨 1 기본 지력 */
@Column(name = "base_int")
private Integer baseInt;
```

> ⚠️ 레벨 스탯(2,256 / 2,466)은 모든 용병 공통 상수이므로 DB 저장 없이
> `MercenaryLevel` enum으로 관리한다 (1.3항 참고).
> 보너스 스탯은 유저 입력값이므로 DB 저장 대상 아님.

---

## 10. SkillCoefficient 엔티티 신규 생성

### 10.1 FK 설계

용병 스킬과 아이템 스킬을 하나의 테이블에서 관리한다.
둘 중 하나만 not null인 구조.

```sql
CONSTRAINT chk_skill_coefficient_fk
CHECK (
    (mercenary_skill_id IS NOT NULL AND item_skill_id IS NULL) OR
    (mercenary_skill_id IS NULL AND item_skill_id IS NOT NULL)
)
```

### 10.2 SkillCoefficientCommand record

정적 팩토리 메서드에 전달할 공통 파라미터를 담는 record.

```java
public record SkillCoefficientCommand(
    String gerniverseRowId,
    float coefStr,
    float coefDex,
    float coefVit,
    float coefInt,
    float coefAtk,
    float coefLvl,
    int hitCount,
    float damageRangeFactor,
    SkillType skillType,
    Confidence confidence,
    String measurementNote
) {}
```

### 10.3 엔티티 코드

```java
@Entity
@Table(
    name = "skill_coefficients",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_skill_coefficients_gerniverse_row_id",
        columnNames = {"gerniverse_row_id"}
    )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SkillCoefficient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mercenary_skill_id")
    private MercenarySkill mercenarySkill;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_skill_id")
    private ItemSkill itemSkill;

    /** 거니버스 원본 row_id — upsert 기준 키 */
    @Column(name = "gerniverse_row_id", length = 100)
    private String gerniverseRowId;

    @Column(name = "coef_str", nullable = false)
    private float coefStr;

    @Column(name = "coef_dex", nullable = false)
    private float coefDex;

    @Column(name = "coef_vit", nullable = false)
    private float coefVit;

    @Column(name = "coef_int", nullable = false)
    private float coefInt;

    @Column(name = "coef_atk", nullable = false)
    private float coefAtk;

    @Column(name = "coef_lvl", nullable = false)
    private float coefLvl;

    @Column(name = "hit_count", nullable = false)
    private int hitCount;

    /**
     * 거니버스 damage_range_factor 원본값.
     * 정확한 의미 미확인 — 데미지 분산 범위로 추정.
     * 0.1이 기본값으로 보이며, 사천왕 계열 스킬은 0.43, 풍뢰탄은 1.0.
     * DPS 계산에서는 현재 사용하지 않음.
     */
    @Column(name = "damage_range_factor", nullable = false)
    private float damageRangeFactor;

    @Enumerated(EnumType.STRING)
    @Column(name = "skill_type", length = 20, nullable = false)
    private SkillType skillType;

    /**
     * 초당 시전 횟수 — 직접 측정값. INSTANT 전용.
     * null이면 미측정. DPS 계산 및 UI 노출에서 제외.
     */
    @Column(name = "casts_per_second")
    private Float castsPerSecond;

    /**
     * 타격 간격 (밀리초) — 직접 측정값. PERSISTENT 전용.
     * null이면 미측정. DPS 계산 및 UI 노출에서 제외.
     */
    @Column(name = "tick_interval_ms")
    private Integer tickIntervalMs;

    @Enumerated(EnumType.STRING)
    @Column(name = "confidence", length = 20)
    private Confidence confidence;

    @Column(name = "measurement_note", columnDefinition = "TEXT")
    private String measurementNote;

    /** 용병 스킬용 정적 팩토리 */
    public static SkillCoefficient forMercenary(
            MercenarySkill mercenarySkill,
            SkillCoefficientCommand cmd) {
        SkillCoefficient sc = new SkillCoefficient();
        sc.mercenarySkill = mercenarySkill;
        sc.itemSkill = null;
        sc.applyCommand(cmd);
        return sc;
    }

    /** 아이템 스킬용 정적 팩토리 */
    public static SkillCoefficient forItem(
            ItemSkill itemSkill,
            SkillCoefficientCommand cmd) {
        SkillCoefficient sc = new SkillCoefficient();
        sc.itemSkill = itemSkill;
        sc.mercenarySkill = null;
        sc.applyCommand(cmd);
        return sc;
    }

    private void applyCommand(SkillCoefficientCommand cmd) {
        this.gerniverseRowId = cmd.gerniverseRowId();
        this.coefStr = cmd.coefStr();
        this.coefDex = cmd.coefDex();
        this.coefVit = cmd.coefVit();
        this.coefInt = cmd.coefInt();
        this.coefAtk = cmd.coefAtk();
        this.coefLvl = cmd.coefLvl();
        this.hitCount = cmd.hitCount();
        this.damageRangeFactor = cmd.damageRangeFactor();
        this.skillType = cmd.skillType();
        this.confidence = cmd.confidence();
        this.measurementNote = cmd.measurementNote();
    }

    /** INSTANT 스킬 — 시전속도 직접 측정 후 업데이트 */
    public void updateCastsPerSecond(Float castsPerSecond, String measurementNote) {
        this.castsPerSecond = castsPerSecond;
        this.measurementNote = measurementNote;
    }

    /** PERSISTENT 스킬 — 타격 간격 직접 측정 후 업데이트 */
    public void updateTickIntervalMs(Integer tickIntervalMs, String measurementNote) {
        this.tickIntervalMs = tickIntervalMs;
        this.measurementNote = measurementNote;
    }

    public boolean isMercenarySkill() {
        return mercenarySkill != null;
    }

    public boolean isItemSkill() {
        return itemSkill != null;
    }
}
```

---

## 11. DPS 계산 서비스

### 11.1 UX 선택지

```
1. 레벨: 250 / 260
2. 보너스 스탯 투자: 주스탯 / 체력
3. 보너스 스탯 총량: 300 / 500 / 700 / 900 / 1000 / 직접입력
```

### 11.2 주스탯 자동 판별

`SkillCoefficient` 계수 비교로 주력 스탯을 자동 판별한다.
DB 컬럼 저장 불필요.

```java
public StatType resolveMainStat(SkillCoefficient coef) {
    return Map.of(
        StatType.STRENGTH,   coef.getCoefStr(),
        StatType.DEXTERITY,  coef.getCoefDex(),
        StatType.VITALITY,   coef.getCoefVit(),
        StatType.INTELLECT,  coef.getCoefInt()
    ).entrySet().stream()
        .max(Map.Entry.comparingByValue())
        .map(Map.Entry::getKey)
        .orElseThrow();
}
```

### 11.3 DPS 계산 흐름

```java
public double calculateDps(
        SkillCoefficient coef,
        MercenaryStat stat,
        MercenaryLevel level,        // LV_250 / LV_260
        BonusStatTarget bonusTarget, // MAIN_STAT / VITALITY
        int bonusAmount              // 300/500/700/900/1000 또는 직접입력
) {
    // 1. 실제 스탯 계산
    int str = stat.getBaseStr();
    int dex = stat.getBaseDex();
    int vit = stat.getBaseVit();
    int intel = stat.getBaseInt();

    // 레벨 스탯 합산 (주스탯에 부여)
    StatType mainStat = resolveMainStat(coef);
    // 레벨 스탯은 주스탯에만 합산
    switch (mainStat) {
        case STRENGTH  -> str   += level.getLevelStat();
        case DEXTERITY -> dex   += level.getLevelStat();
        case VITALITY  -> vit   += level.getLevelStat();
        case INTELLECT -> intel += level.getLevelStat();
    }

    // 보너스 스탯 합산
    switch (bonusTarget) {
        case MAIN_STAT -> {
            switch (mainStat) {
                case STRENGTH  -> str   += bonusAmount;
                case DEXTERITY -> dex   += bonusAmount;
                case VITALITY  -> vit   += bonusAmount;
                case INTELLECT -> intel += bonusAmount;
            }
        }
        case VITALITY -> vit += bonusAmount;
    }

    // 2. 공격력은 별도 조회 (MercenaryStat.attackPower 등)
    int atk = stat.getAttackPower(); // 기존 컬럼 활용

    // 3. 원데미지 계산
    double rawDamage = coef.getCoefStr()  * str
                     + coef.getCoefDex()  * dex
                     + coef.getCoefVit()  * vit
                     + coef.getCoefInt()  * intel
                     + coef.getCoefAtk()  * atk
                     + coef.getCoefLvl()  * level.getLevel();

    // 4. DPS 계산
    return switch (coef.getSkillType()) {
        case INSTANT    -> rawDamage * coef.getHitCount() * coef.getCastsPerSecond();
        case PERSISTENT -> rawDamage / (coef.getTickIntervalMs() / 1000.0);
    };
}
```

> ⚠️ `castsPerSecond` / `tickIntervalMs`가 null이면 DPS 계산 불가.
> UI에서 해당 스킬의 DPS 노출 제외.

---

## 12. 시드 데이터 적재 전략

### 12.1 적재 순서

```
1. 거상짱 크롤러 실행 (선행 필수)
   → Mercenary 적재 (mercenary_key 포함)
   → MercenaryStat 적재 (base_str/dex/vit/int 포함)
   → Item 적재
     ※ 사인검은 속성별 5행(tkdlsrja-tn/ghk/xh/shl/vnd)으로 저장됨

2. 거니버스 데이터 적재 (별도 CLI 명령어 또는 관리자 API)
   → MercenarySkill 적재 (skill_key 포함)
   → ItemSkill 적재 (skill_key 포함)
   → SkillCoefficient 적재 (calc_details 파싱 후 계수 저장)
     - 전체 skill_type = INSTANT로 초기 저장
     - PERSISTENT 스킬 수동 변경 (3항 목록 참고)

3. casts_per_second / tick_interval_ms 직접 측정 후 수동 업데이트
   - 관리자가 게임 내에서 직접 측정
   - 기존 관리자 페이지에서 스킬 목록을 테이블 형태로 조회
   - casts_per_second / tick_interval_ms 셀 인라인 편집 후 저장
   - API: GET /admin/skill-coefficients (목록 조회)
          PATCH /admin/skill-coefficients/{id} (단건 업데이트)
   - UI 구현 시점은 엔티티/적재 완료 후로 미룬다
```

> ⚠️ 거니버스 데이터 적재는 Flyway 자동 실행 대상이 아님.
> 크롤러 실행 완료 후 별도 명령어로 실행해야 FK 참조 실패 없음.

### 12.2 Upsert 기준 키

| 엔티티 | Upsert 기준 |
|---|---|
| `MercenarySkill` | `mercenary_id` + `skill_name` |
| `ItemSkill` | `item_id` + `skill_name` |
| `SkillCoefficient` | `gerniverse_row_id` |

### 12.3 거니버스 → DB 컬럼 매핑

| 거니버스 필드 | DB 컬럼 | 처리 방식 |
|---|---|---|
| `row_id` | `gerniverse_row_id` | 그대로 저장 |
| `mercenary_key` | `Mercenary.key` | 조회용 |
| `is_item` | FK 분기 | false → mercenarySkill, true → itemSkill |
| `skill_key` | `MercenarySkill.skillKey` or `ItemSkill.skillKey` | 그대로 저장 |
| `skill_name` | `skillName` | 그대로 저장 |
| `calc_details` | `coef_str` ~ `coef_lvl` | 5항 파싱 규칙 적용 |
| `hit_count` | `hit_count` | 그대로 저장 |
| `damage_range_factor` | `damage_range_factor` | 그대로 저장 |
| `confidence` | `confidence` | 대문자 변환 후 enum 파싱 |
| `note` | `measurement_note` | 그대로 저장 |
| `base_constant` | 저장 안 함 | 현재 전부 0, 0 아닌 케이스 발생 시 재검토 |
| `formula_version` | 저장 안 함 | 현재 전부 1, 버전 변경 시 재검토 |
| _(미존재)_ | `skill_type` | 전체 INSTANT로 초기 저장, PERSISTENT 수동 변경 |
| _(미존재)_ | `casts_per_second` | 직접 측정 후 업데이트 |
| _(미존재)_ | `tick_interval_ms` | 직접 측정 후 업데이트 |

---

## 13. 주의사항

```
1. mercenarySkill / itemSkill 둘 중 하나만 not null — DB CHECK 제약 필수.

2. skill_type = nullable = false 필수.
   초기값 INSTANT로 저장 후 PERSISTENT만 수동 변경.

3. 동일 skill_key에 계수 세트가 여러 개인 경우 (예: 대지포식)
   skill_name 기준으로 MercenarySkill 분리:
   "대지포식 (소환물)" / "대지포식 (가시덤불)" 각각 별도 행.

4. casts_per_second는 절대 추정값을 넣지 않는다.
   측정 완료된 것만 업데이트한다.
   null이면 UI DPS 노출 제외.

5. StatType 추가 시 기존 enum 값 절대 수정 금지.

6. INSTANT이면 tick_interval_ms = null, PERSISTENT이면 casts_per_second = null.
   DB CHECK 제약:
   CONSTRAINT chk_skill_type_measurement
   CHECK (
       (skill_type = 'INSTANT'    AND casts_per_second IS NOT NULL AND tick_interval_ms IS NULL) OR
       (skill_type = 'PERSISTENT' AND tick_interval_ms IS NOT NULL AND casts_per_second IS NULL) OR
       (casts_per_second IS NULL AND tick_interval_ms IS NULL)  -- 미측정 허용
   )

7. base_constant가 0이 아닌 케이스 발견 시 coef_base 컬럼 추가 필요.
   현재는 전부 0이므로 저장 생략.

8. Mercenary.key 컬럼이 DB에 존재해야 거니버스 데이터 적재 가능.
   없으면 별도 추가 필요.
```

---

## 14. TODO — 향후 확장 기능

> 현재 단계에서는 설계하지 않는다.
> DPS 계산기 데이터가 충분히 쌓이고 실현 가능성 확인 후 구체화한다.

```
[ ] 유저 덱 저장
    - 주캐 + 장수 구성 및 DPS 정량화 수치 저장

[ ] 몬스터 정보 저장
    - 보스 / 일반 몬스터 구분
    - 클리어타임은 보스와 일반 몬스터를 차별화하여 기록

[ ] 클리어타임 기록
    - 유저별 몬스터별 클리어타임 저장
    - 덱 구성 스냅샷 함께 저장

[ ] 추천 로직
    - 배치(batch) 탐색으로 상위 클리어타임과 현재 덱 비교
    - 차이점 추출 (어떤 용병/아이템 유무가 영향을 줬는지)
    - 유저별 개인화 추천 제공
    - 구체적 설계는 데이터 축적 후 별도 문서로 작성
```