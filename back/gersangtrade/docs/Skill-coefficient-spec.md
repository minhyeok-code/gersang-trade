# 스킬 계수 엔티티 스펙

## 0. 이 문서의 목적

Claude Code가 이 문서만 읽고 아래를 구현할 수 있도록 작성되었다.

1. `StatType` enum 확장 — DPS 계산용 스탯 타입 추가
2. `SkillType` enum 신규 생성 — INSTANT / PERSISTENT 구분
3. `MercenarySkill` 엔티티 수정 — `skillKey` 컬럼 추가
4. `Item` 엔티티 수정 — `itemKey` 컬럼 추가
5. `ItemSkill` 엔티티 수정 — `skillKey` 컬럼 추가
6. `SkillCoefficient` 엔티티 신규 생성 — 용병/아이템 스킬 통합 참조
7. 시드 데이터 적재 전략

---

## 1. 배경 및 데이터 소스

### 1.1 데이터 소스 역할 분리

```
거상짱 크롤러   → 용병 기본정보 / 스탯 / 재료 / 장비
거니버스 HTML   → 스킬명 / skill_key / 스킬 계수 (수동 저장)
직접 측정       → casts_per_second (시전속도)
```

거니버스 HTML의 `self.__next_f.push()` 내부 `sim-coefficients` 쿼리에
용병별 스킬 계수가 전부 포함되어 있다. 이를 정답 소스로 사용한다.

### 1.2 DPS 계산 공식

```
원데미지 = coef_str×STR + coef_dex×DEX + coef_vit×VIT
         + coef_int×INT + coef_atk×ATK + coef_lvl×LVL

INSTANT:    DPS = 원데미지 × hit_count × casts_per_second
PERSISTENT: DPS = 원데미지 / (tick_interval_ms / 1000.0)
```

`LVL`은 DB에 저장하지 않는다. 유저가 계산기 입력 시 250 / 260 중 선택하는 요청 파라미터이다.

`casts_per_second` (INSTANT) 또는 `tick_interval_ms` (PERSISTENT)가 null이면 미측정 상태이며 DPS 노출에서 제외한다.

### 1.3 측정 우선순위

직접 측정(`casts_per_second`) 대상:
- 사천왕 일반/각성 (8마리)
- 명왕 일반/각성 (8마리)
- 전설장수 15마리
- 사인검

측정 조건 고정:
- 공격속도 버프 없음
- 자동전투 기준
- 동일 레벨 기준

---

## 2. StatType enum — DPS 계산용 항목 확인

아래 항목은 DPS 계산용 스탯 타입으로, `StatType.java`에 **이미 모두 존재**한다.
추가 작업 불필요. 구현 시 그대로 사용한다.

```java
// 이미 존재 — 추가 불필요
STRENGTH("힘"),
DEXTERITY("민첩"),
VITALITY("생명력"),   // displayName "생명력" — "생명" 아님
INTELLECT("지력"),
ATTACK_POWER("공격력"),
```

> ⚠️ VITALITY의 displayName은 `"생명력"`이다. `"생명"`으로 표기된 곳이 있으면 코드 기준(`StatType.java`)으로 교정한다.
>
> ⚠️ 기존 enum 값을 절대 수정하지 않는다 (DB에 String으로 저장된 기존 값 파괴 금지).

---

## 3. SkillType enum 신규 생성

스킬 시전 방식을 구분하는 enum이다.
DPS 계산 방식이 타입에 따라 달라지므로 반드시 구분해야 한다.

```java
public enum SkillType {
    INSTANT,    // 일반 시전형 — casts_per_second로 DPS 계산
    PERSISTENT, // 지속/장판형 — tick_interval_ms로 DPS 계산
}
```

### 타입별 DPS 계산 방식

```
INSTANT:
  원데미지 = coef_str×STR + ... + coef_atk×ATK + coef_lvl×LVL
  DPS = 원데미지 × hit_count × casts_per_second

PERSISTENT:
  원데미지 = coef_str×STR + ... + coef_atk×ATK + coef_lvl×LVL
  DPS = 원데미지 / (tick_interval_ms / 1000.0)
```

### 타입 분류 예시

| 스킬명 | 용병 | 타입 |
|---|---|---|
| 신토류 | 보쿠텐 | INSTANT |
| 빙화 | 초선 | INSTANT |
| 팔괘진 | 마조 | PERSISTENT |
| 대지포식 (소환물) | 레지나 | PERSISTENT |
| 용오름 소환 | 홍길동 | PERSISTENT |
| 모래병사 소환 | 악바르 | PERSISTENT |

---

## 4. MercenarySkill 엔티티 수정

기존 `MercenarySkill`에 `skill_key` 컬럼을 추가한다.

```java
/**
 * 거니버스 내부 스킬 식별 키 — 예: "tlsxhfb", "qldghk"
 * 거상짱 크롤링 시에는 null. 거니버스 데이터 적재 후 채워진다.
 * SkillCoefficient와의 연결 키로 사용된다.
 */
@Column(name = "skill_key", length = 100)
private String skillKey;
```

### 변경 후 전체 엔티티

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

## 5. Item 엔티티 수정

기존 `Item`에 `item_key` 컬럼을 추가한다.
거니버스 `image_path`의 마지막 세그먼트를 `item_key`로 사용한다.
`mercenary_key`("saingeom", "immortal-weapon" 등)는 사용하지 않는다.

```java
/**
 * 거니버스 image_path 마지막 세그먼트 기반 아이템 식별 키.
 * 예: "tkdlsrja" (사인검), "whdflrnjsdmldlsgud" (종리권의 인형)
 * ItemSkill / SkillCoefficient 적재 시 조회 기준으로 사용된다.
 * 수동 입력 전에는 null.
 */
@Column(name = "item_key", length = 100, unique = true)
private String itemKey;

public void updateItemKey(String itemKey) {
    this.itemKey = itemKey;
}
```

### item_key 추출 규칙

거니버스 `image_path`의 마지막 세그먼트를 **접미사 제거 없이 그대로** 사용한다.
사인검은 속성별로 실제로 다른 아이템이므로 속성별로 별도 Item 행으로 저장한다.

```java
// 모든 아이템 동일 규칙 — 예외 없음
String itemKey = imagePath.substring(imagePath.lastIndexOf('/') + 1);
```

```
"item/weapon/sword/tkdlsrja-tn"      → item_key = "tkdlsrja-tn"
"item/weapon/sword/tkdlsrja-ghk"     → item_key = "tkdlsrja-ghk"
"item/weapon/doll/whdflrnjsdmldlsgud" → item_key = "whdflrnjsdmldlsgud"
```

### 아이템 key 목록

| 아이템명 | item_key | 비고 |
|---|---|---|
| 뇌속성 사인검 | `tkdlsrja-tn` | 뇌속성 용병에게 속성값 +20 |
| 화속성 사인검 | `tkdlsrja-ghk` | 화속성 용병에게 속성값 +20 |
| 수속성 사인검 | `tkdlsrja-xh` | 수속성 용병에게 속성값 +20 |
| 풍속성 사인검 | `tkdlsrja-shl` | 풍속성 용병에게 속성값 +20 |
| 지속성 사인검 | `tkdlsrja-vnd` | 지속성 용병에게 속성값 +20 |
| 종리권의 인형 | `whdflrnjsdmldlsgud` | |
| 조국구의 비검 | `whrnrrndmlqlrja` | |
| 호선의 인형 | `ghtjsdmldlsgud` | |
| 여동빈의 비검 | `duehdqlsdmlqlrja` | |
| 철괴리의 인형 | `cjfrhlfldmldlsgud` | |
| 장과로의 인형 | `wkdrhkfhdmldlsgud` | |
| 한상자의 인형 | `gkstkdwkdmldlsgud` | |
| 람채화의 인형 | `fkacoghkdmldlsgud` | |

> ℹ️ 사인검 5종은 스킬(`은하수(强)`)이 동일하므로 `ItemSkill` 5행 + `SkillCoefficient` 5행이 생성된다.
> `casts_per_second`는 속성 무관하게 동일하므로 1회 측정 후 5행 모두 동일값을 입력한다.

---

## 6. ItemSkill 엔티티 수정

기존 `ItemSkill`에 `skill_key` 컬럼을 추가한다. `MercenarySkill`과 동일한 방식.

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

## 7. SkillCoefficient 엔티티 신규 생성

### 7.1 FK 설계 결정

용병 스킬과 아이템 스킬을 **하나의 테이블**에서 관리한다.
FK를 두 개 두고 둘 중 하나만 not null인 구조를 사용한다.

```
skill_coefficients
  - mercenary_skill_id (nullable) ─┐ 둘 중
  - item_skill_id      (nullable) ─┘ 하나만 not null
```

DB CHECK 제약:
```sql
CONSTRAINT chk_skill_coefficient_fk
CHECK (
    (mercenary_skill_id IS NOT NULL AND item_skill_id IS NULL) OR
    (mercenary_skill_id IS NULL AND item_skill_id IS NOT NULL)
)
```

### 7.2 엔티티 코드

```java
package org.example.gersangtrade.domain.catalog;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 스킬 데미지 계수 엔티티.
 * 거니버스 sim-coefficients 데이터를 기반으로 적재된다.
 *
 * <p>용병 스킬(MercenarySkill)과 아이템 스킬(ItemSkill)을 통합 관리한다.
 * mercenarySkill / itemSkill 중 하나만 not null이어야 한다.
 * DB CHECK 제약으로 강제한다.
 *
 * <p>동일 스킬에 계수 세트가 여러 개일 수 있다 (예: 대지포식 — 소환물/가시덤불).
 *
 * <p>DPS 계산:
 * 원데미지 = coef_str×STR + coef_dex×DEX + coef_vit×VIT
 *           + coef_int×INT + coef_atk×ATK + coef_lvl×LVL
 * DPS = 원데미지 × hitCount × castsPerSecond
 *
 * <p>castsPerSecond가 null이면 DPS 계산 불가 — UI 노출 제외.
 */
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

    /**
     * 용병 스킬 FK.
     * itemSkill이 not null이면 이 필드는 null.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mercenary_skill_id")
    private MercenarySkill mercenarySkill;

    /**
     * 아이템 스킬 FK.
     * mercenarySkill이 not null이면 이 필드는 null.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_skill_id")
    private ItemSkill itemSkill;

    /**
     * 거니버스 원본 row_id — upsert 기준 키.
     * 거니버스 데이터 재적재 시 중복 방지에 사용한다.
     */
    @Column(name = "gerniverse_row_id", length = 100)
    private String gerniverseRowId;

    /** 힘 계수 */
    @Column(name = "coef_str", nullable = false)
    private float coefStr;

    /** 민첩 계수 */
    @Column(name = "coef_dex", nullable = false)
    private float coefDex;

    /** 생명 계수 */
    @Column(name = "coef_vit", nullable = false)
    private float coefVit;

    /** 지력 계수 */
    @Column(name = "coef_int", nullable = false)
    private float coefInt;

    /** 공격력 계수 */
    @Column(name = "coef_atk", nullable = false)
    private float coefAtk;

    /** 레벨 계수 */
    @Column(name = "coef_lvl", nullable = false)
    private float coefLvl;

    /** 스킬 1회 시전 시 타격 수 */
    @Column(name = "hit_count", nullable = false)
    private int hitCount;

    /**
     * 데미지 범위 계수.
     * 0이면 범위 없음 (고정 데미지).
     * 거니버스 damage_range_factor 값을 그대로 저장.
     */
    @Column(name = "damage_range_factor", nullable = false)
    private float damageRangeFactor;

    /**
     * 스킬 시전 방식.
     * INSTANT: 일반 시전형 → casts_per_second로 DPS 계산.
     * PERSISTENT: 지속/장판형 → tick_interval_ms로 DPS 계산.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "skill_type", length = 20)
    private SkillType skillType;

    /**
     * 초당 시전 횟수 — 직접 측정값. INSTANT 전용.
     * null이면 미측정. DPS 계산 및 UI 노출에서 제외.
     * PERSISTENT이면 null.
     * 측정 조건: 공격속도 버프 없음, 자동전투 기준.
     */
    @Column(name = "casts_per_second")
    private Float castsPerSecond;

    /**
     * 타격 간격 (밀리초) — 직접 측정값. PERSISTENT 전용.
     * null이면 미측정. DPS 계산 및 UI 노출에서 제외.
     * INSTANT이면 null.
     * 예: 팔괘진이 0.5초마다 1타 → tickIntervalMs = 500
     */
    @Column(name = "tick_interval_ms")
    private Integer tickIntervalMs;

    /**
     * 데이터 신뢰도.
     * 거니버스 confidence 값을 그대로 사용.
     * verified / high / medium / low
     */
    @Column(name = "confidence", length = 20)
    private String confidence;

    /**
     * 측정/검증 메모.
     * 거니버스 note + 직접 측정 조건을 함께 기록.
     */
    @Column(name = "measurement_note", columnDefinition = "TEXT")
    private String measurementNote;

    /** 용병 스킬용 생성자 */
    @Builder(builderMethodName = "ofMercenary")
    public SkillCoefficient(MercenarySkill mercenarySkill, String gerniverseRowId,
                            float coefStr, float coefDex, float coefVit, float coefInt,
                            float coefAtk, float coefLvl, int hitCount,
                            float damageRangeFactor, SkillType skillType,
                            Float castsPerSecond, Integer tickIntervalMs,
                            String confidence, String measurementNote) {
        this.mercenarySkill = mercenarySkill;
        this.itemSkill = null;
        this.gerniverseRowId = gerniverseRowId;
        this.coefStr = coefStr;
        this.coefDex = coefDex;
        this.coefVit = coefVit;
        this.coefInt = coefInt;
        this.coefAtk = coefAtk;
        this.coefLvl = coefLvl;
        this.hitCount = hitCount;
        this.damageRangeFactor = damageRangeFactor;
        this.skillType = skillType;
        this.castsPerSecond = castsPerSecond;
        this.tickIntervalMs = tickIntervalMs;
        this.confidence = confidence;
        this.measurementNote = measurementNote;
    }

    /** 아이템 스킬용 생성자 */
    @Builder(builderMethodName = "ofItem")
    public SkillCoefficient(ItemSkill itemSkill, String gerniverseRowId,
                            float coefStr, float coefDex, float coefVit, float coefInt,
                            float coefAtk, float coefLvl, int hitCount,
                            float damageRangeFactor, SkillType skillType,
                            Float castsPerSecond, Integer tickIntervalMs,
                            String confidence, String measurementNote) {
        this.mercenarySkill = null;
        this.itemSkill = itemSkill;
        this.gerniverseRowId = gerniverseRowId;
        this.coefStr = coefStr;
        this.coefDex = coefDex;
        this.coefVit = coefVit;
        this.coefInt = coefInt;
        this.coefAtk = coefAtk;
        this.coefLvl = coefLvl;
        this.hitCount = hitCount;
        this.damageRangeFactor = damageRangeFactor;
        this.skillType = skillType;
        this.castsPerSecond = castsPerSecond;
        this.tickIntervalMs = tickIntervalMs;
        this.confidence = confidence;
        this.measurementNote = measurementNote;
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

    /** 용병 스킬 계수인지 여부 */
    public boolean isMercenarySkill() {
        return mercenarySkill != null;
    }

    /** 아이템 스킬 계수인지 여부 */
    public boolean isItemSkill() {
        return itemSkill != null;
    }
}
```

---

## 8. 시드 데이터 적재 전략

### 8.1 적재 순서

```
1. Mercenary / Item 적재 (거상짱 크롤러 — 기존)
2. MercenarySkill 적재 (거니버스 데이터 기반)
   - mercenary.key로 Mercenary 조회 후 연결
   - skill_name + skill_key 함께 저장
3. ItemSkill 적재 (거니버스 is_item=true 데이터)
   - item 조회 후 연결
   - skill_name + skill_key 함께 저장
4. SkillCoefficient 적재
   - 용병 스킬: mercenarySkill FK, itemSkill = null
   - 아이템 스킬: itemSkill FK, mercenarySkill = null
   - gerniverse_row_id로 upsert
```

### 8.2 Upsert 기준 키

| 엔티티 | Upsert 기준 |
|---|---|
| `MercenarySkill` | `mercenary_id` + `skill_name` |
| `ItemSkill` | `item_id` + `skill_name` |
| `SkillCoefficient` | `gerniverse_row_id` |

### 8.3 거니버스 → DB 컬럼 매핑

| 거니버스 필드 | DB 컬럼 | 비고 |
|---|---|---|
| `row_id` | `gerniverse_row_id` | upsert 기준 키 |
| `mercenary_key` | `Mercenary.key` | 조회용 |
| `is_item` | FK 분기 | false → mercenarySkill, true → itemSkill |
| `skill_key` | `MercenarySkill.skillKey` or `ItemSkill.skillKey` | |
| `skill_name` | `MercenarySkill.skillName` or `ItemSkill.skillName` | |
| `coef_str` | `coef_str` | |
| `coef_dex` | `coef_dex` | |
| `coef_vit` | `coef_vit` | |
| `coef_int` | `coef_int` | |
| `coef_atk` | `coef_atk` | |
| `coef_lvl` | `coef_lvl` | |
| `hit_count` | `hit_count` | |
| `damage_range_factor` | `damage_range_factor` | |
| `confidence` | `confidence` | |
| `note` | `measurement_note` | |
| _(미존재)_ | `skill_type` | INSTANT / PERSISTENT 직접 분류 |
| _(미존재)_ | `casts_per_second` | INSTANT 전용, 직접 측정 후 업데이트 |
| _(미존재)_ | `tick_interval_ms` | PERSISTENT 전용, 직접 측정 후 업데이트 |

---

## 9. 주의사항

```
1. mercenarySkill / itemSkill 둘 중 하나만 not null — DB CHECK 제약 필수.
   둘 다 null이거나 둘 다 not null인 행은 데이터 오류.

2. 동일 skill_key에 계수 세트가 여러 개인 경우 (예: 대지포식)
   거니버스 skill_name 기준으로 MercenarySkill을 분리한다.
   "대지포식 (소환물)" / "대지포식 (가시덤불)" 각각 별도 행.

3. casts_per_second는 절대 추정값을 넣지 않는다.
   측정 완료된 것만 업데이트한다.

4. StatType 추가 시 기존 enum 값 절대 수정 금지.
   DB에 String으로 저장된 기존 값이 깨진다.

5. 아이템 스킬 적재 시 Item 엔티티가 먼저 DB에 존재해야 한다.
   Item이 없으면 ItemSkill FK 연결 실패 → 스킵 + 경고 로그.

6. skill_type은 거니버스 데이터에 없으므로 수동 분류가 필요하다.
   PERSISTENT 스킬: 팔괘진, 대지포식(소환물/가시덤불), 용오름 소환, 모래병사 소환
   나머지는 전부 INSTANT.

7. INSTANT이면 tick_interval_ms = null, PERSISTENT이면 casts_per_second = null.
   둘 다 null이거나 둘 다 not null이면 데이터 오류.
   DB CHECK 제약 추가:
   CONSTRAINT chk_skill_type_measurement
   CHECK (
       (skill_type = 'INSTANT'    AND casts_per_second IS NOT NULL AND tick_interval_ms IS NULL) OR
       (skill_type = 'PERSISTENT' AND tick_interval_ms IS NOT NULL AND casts_per_second IS NULL) OR
       (casts_per_second IS NULL AND tick_interval_ms IS NULL)  -- 미측정 허용
   )
```