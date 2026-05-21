# 용병 스킬(MercenarySkill) 엔티티 설계 문서

## 0. 이 문서의 목적

Claude Code가 이 문서만 읽고 아래를 구현할 수 있도록 작성되었다.

1. `MercenarySkill` 엔티티 수정 (스킬 계수 컬럼 추가)
2. `SkillType` enum 신규 생성
3. DPS 계산기 구조 설계

---

## 1. 스킬 계수 패턴 분석

거상 데미지 공식의 공통 패턴:

```
1타 데미지 = 공격력×a + 힘×b + 민첩×c + 생명력×d + 지력×e + 레벨×f
최종 데미지 = 1타 데미지 × 타격횟수 × 특성배율
```

조건부 발동 스킬 (매 n번째):
```
각성 슈크라 대포격: 포격 3번마다 발동
조선남 폭발: 함정 피해 받는 적에게 발동
```

---

## 2. SkillType enum

```java
package org.example.gersangtrade.domain.catalog.enums;

public enum SkillType {
    ACTIVE,     // 일반 액티브 스킬
    PASSIVE,    // 패시브 스킬 (상시 적용)
    WEAPON,     // 무기 전용 스킬
    TRIGGER,    // 조건부 발동 스킬 (매 n번째 등)
    BUFF,       // 버프/회복 스킬 (데미지 없음)
    SUMMON,     // 소환 스킬
}
```

---

## 3. MercenarySkill 엔티티 수정

기존 컬럼:
```
id, mercenary_id, skill_name
```

추가 컬럼:

```java
@Entity
@Table(name = "mercenary_skills")
public class MercenarySkill {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mercenary_id", nullable = false)
    private Mercenary mercenary;

    @Column(name = "skill_name", nullable = false, length = 100)
    private String skillName;

    // ── 신규 추가 ─────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "skill_type")
    private SkillType skillType;

    // ── 스킬 계수 ─────────────────────────────────────────
    // null = 해당 스탯 계수 없음

    @Column(name = "power_coeff")
    private Float powerCoeff;       // 공격력 계수

    @Column(name = "strength_coeff")
    private Float strengthCoeff;    // 힘 계수

    @Column(name = "dexterity_coeff")
    private Float dexterityCoeff;   // 민첩 계수

    @Column(name = "vitality_coeff")
    private Float vitalityCoeff;    // 생명력 계수

    @Column(name = "intellect_coeff")
    private Float intellectCoeff;   // 지력 계수

    @Column(name = "level_coeff")
    private Float levelCoeff;       // 레벨 계수

    // ── 타격 구조 ─────────────────────────────────────────

    @Column(name = "hit_count")
    private Integer hitCount;       // 타격 횟수. null = 단타

    /**
     * 특성 최대 배율.
     * 예: 2.5 = 특성 최대 투자 시 데미지 2.5배
     * null = 특성 배율 없음
     */
    @Column(name = "max_trait_multiplier")
    private Float maxTraitMultiplier;

    // ── 조건부 발동 (TRIGGER 타입 전용) ──────────────────

    /**
     * 매 n번째 기본 스킬 발동 시 이 스킬이 발동됨.
     * 예: 3 = 기본 스킬 3번마다 발동
     * null = 조건 없음 (일반 스킬)
     */
    @Column(name = "trigger_every_n")
    private Integer triggerEveryN;

    /**
     * 발동 조건이 되는 기본 스킬 key.
     * triggerEveryN이 있을 때만 사용.
     * 예: "포격" 스킬 key → 포격 3번마다 이 스킬 발동
     */
    @Column(name = "trigger_base_skill_key", length = 100)
    private String triggerBaseSkillKey;

    /**
     * 스킬 설명 / 특이사항.
     * 공식으로 표현 불가한 특수 효과 기록용.
     * 예: "첫번째 화검은 최소공격력+힘만 적용"
     */
    @Column(name = "note", columnDefinition = "TEXT")
    private String note;
}
```

---

## 4. DPS 계산기 구조

### 4-1. 1타 데미지 계산

```java
@Service
public class SkillDamageCalculator {

    /**
     * 스킬 1타 데미지를 계산한다.
     *
     * @param skill      스킬 엔티티
     * @param mercStats  용병 스탯 Map (계수 합산 후)
     * @param level      용병 레벨 (250 or 260)
     * @param traitLevel 특성 투자 레벨 (0~10)
     * @return 1타 데미지
     */
    public float calcSingleHit(
            MercenarySkill skill,
            Map<StatType, Float> mercStats,
            int level,
            int traitLevel) {

        float damage = 0f;

        if (skill.getPowerCoeff() != null)
            damage += mercStats.getOrDefault(StatType.MIN_POWER, 0f) * skill.getPowerCoeff();
        if (skill.getStrengthCoeff() != null)
            damage += mercStats.getOrDefault(StatType.STRENGTH, 0f) * skill.getStrengthCoeff();
        if (skill.getDexterityCoeff() != null)
            damage += mercStats.getOrDefault(StatType.DEXTERITY, 0f) * skill.getDexterityCoeff();
        if (skill.getVitalityCoeff() != null)
            damage += mercStats.getOrDefault(StatType.VITALITY, 0f) * skill.getVitalityCoeff();
        if (skill.getIntellectCoeff() != null)
            damage += mercStats.getOrDefault(StatType.INTELLECT, 0f) * skill.getIntellectCoeff();
        if (skill.getLevelCoeff() != null)
            damage += level * skill.getLevelCoeff();

        return damage;
    }

    /**
     * 스킬 총 데미지 = 1타 데미지 × 타격횟수 × 특성배율
     */
    public float calcTotalDamage(
            MercenarySkill skill,
            Map<StatType, Float> mercStats,
            int level,
            int traitLevel) {

        float singleHit = calcSingleHit(skill, mercStats, level, traitLevel);
        int hits = skill.getHitCount() != null ? skill.getHitCount() : 1;

        // 특성 배율: traitLevel 비례로 선형 보간
        float traitMult = calcTraitMultiplier(skill, traitLevel);

        return singleHit * hits * traitMult;
    }

    /**
     * 특성 배율 계산.
     * traitLevel 0 → 1.0 (배율 없음)
     * traitLevel 최대 → maxTraitMultiplier
     * 선형 보간으로 중간값 계산
     *
     * ※ 정확한 보간 공식은 테스트서버 확인 후 수정 필요
     */
    private float calcTraitMultiplier(MercenarySkill skill, int traitLevel) {
        if (skill.getMaxTraitMultiplier() == null || traitLevel == 0) return 1.0f;
        int maxLevel = 10; // 전설장수/사천왕 특성 최대 레벨
        return 1.0f + (skill.getMaxTraitMultiplier() - 1.0f) * (traitLevel / (float) maxLevel);
    }
}
```

### 4-2. DPS 계산 (12초 기준)

```java
/**
 * 12초 기준 용병 DPS를 계산한다.
 *
 * TRIGGER 타입 스킬:
 *   triggerEveryN = 3이면 기본 스킬 시전 횟수 / 3 만큼 발동
 *   기본 스킬 시전 횟수는 미확정 → 추후 castPerSecond 컬럼 추가 예정
 *
 * ※ 시전 횟수(castPerSecond)는 테스트서버 확인 후 구현
 */
public float calcDps(
        List<MercenarySkill> skills,
        Map<StatType, Float> mercStats,
        int level,
        Map<String, Integer> traitLevels) {

    float totalDamage = 0f;
    float battleSeconds = 12f;

    for (MercenarySkill skill : skills) {
        if (skill.getSkillType() == SkillType.BUFF
         || skill.getSkillType() == SkillType.SUMMON) continue;

        float skillDamage = calcTotalDamage(
            skill, mercStats, level,
            traitLevels.getOrDefault(skill.getSkillName(), 0)
        );

        if (skill.getSkillType() == SkillType.TRIGGER) {
            // TODO: triggerEveryN 기반 발동 횟수 계산
            // 기본 스킬 시전 횟수 확정 후 구현
            // 임시: 기본 스킬의 1/triggerEveryN 적용
            totalDamage += skillDamage / skill.getTriggerEveryN();
        } else {
            // TODO: 스킬 시전 횟수(castPerSecond) 확정 후 구현
            totalDamage += skillDamage;
        }
    }

    return totalDamage / battleSeconds;
}
```

---

## 5. 오픈 이슈 (테스트서버 확인 필요)

| 항목 | 내용 | 우선순위 |
|------|------|---------|
| 스킬 시전 횟수 | 12초 기준 각 스킬 실제 시전 횟수 측정 필요 | 높음 |
| 특성 배율 보간 | traitLevel 중간값에서 배율이 선형인지 확인 필요 | 높음 |
| 각성 사천왕 스킬 계수 | 데이터 없음, 테스트서버 오픈 후 측정 | 높음 |
| 공격력 계수 기준 | 최소공격력 기준인지 최대공격력 기준인지 스킬마다 다름 | 중간 |
| 조건부 발동 주기 | triggerEveryN 기반 실제 DPS 검증 필요 | 중간 |

---

## 6. 미구현 (테스트서버 대기)

```
castPerSecond: 스킬 시전 횟수 컬럼 → 측정 후 추가
실제 DPS 계산 로직 완성
딜 비중 최종 계산
```