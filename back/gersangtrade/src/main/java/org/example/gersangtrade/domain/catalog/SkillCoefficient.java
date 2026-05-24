package org.example.gersangtrade.domain.catalog;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.gersangtrade.domain.catalog.enums.SkillType;

/**
 * 스킬 데미지 계수 엔티티.
 * 거니버스 sim-coefficients 데이터를 기반으로 적재된다.
 *
 * <p>용병 스킬(MercenarySkill)과 아이템 스킬(ItemSkill)을 통합 관리한다.
 * mercenarySkill / itemSkill 중 하나만 not null이어야 하며, DB CHECK 제약으로 강제한다.
 *
 * <p>동일 스킬에 계수 세트가 여러 개일 수 있다 (예: 대지포식 — 소환물/가시덤불).
 *
 * <p>DPS 계산:
 * 원데미지 = coef_str×STR + coef_dex×DEX + coef_vit×VIT + coef_int×INT + coef_atk×ATK + coef_lvl×LVL
 * INSTANT:    DPS = 원데미지 × hitCount × castsPerSecond
 * PERSISTENT: DPS = 원데미지 / (tickIntervalMs / 1000.0)
 * TRIGGER:    trigger_every_n은 연결된 ItemSkill 또는 SetGrantedSkill에서 조회
 */
@Entity
@Table(
        name = "skill_coefficients",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_skill_coefficients_row_id",
                columnNames = {"row_id"}
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
     * 세트 부여 스킬 FK.
     * mercenarySkill·itemSkill이 not null이면 이 필드는 null.
     * 전설장수 세트 n종 달성 시 발동되는 스킬 (예: 최무선 10강 7종 → 천자총통:개량).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "set_granted_skill_id")
    private SetGrantedSkill setGrantedSkill;

    /** 스킬 계수 원본 row_id — upsert 기준 키. Skill-coeff-entity.json의 row_id와 대응한다. */
    @Column(name = "row_id", length = 100)
    private String rowId;

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
     * 0이면 범위 없음(고정 데미지). 거니버스 damage_range_factor 값을 그대로 저장.
     */
    @Column(name = "damage_range_factor", nullable = false)
    private float damageRangeFactor;

    /**
     * 스킬 DPS 계산 방식.
     * INSTANT:    즉발형 → casts_per_second로 DPS 계산.
     * PERSISTENT: 지속/장판형 → tick_interval_ms로 DPS 계산.
     * TRIGGER:    트리거형 → 연결된 ItemSkill/SetGrantedSkill의 trigger_every_n 참조.
     * 거니버스 데이터에 없으므로 수동 분류가 필요하다.
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
     * 타격 간격(밀리초) — 직접 측정값. PERSISTENT 전용.
     * null이면 미측정. DPS 계산 및 UI 노출에서 제외.
     * INSTANT이면 null.
     * 예: 팔괘진이 0.5초마다 1타 → tickIntervalMs = 500
     */
    @Column(name = "tick_interval_ms")
    private Integer tickIntervalMs;

    /**
     * 데이터 신뢰도 — 거니버스 confidence 값을 그대로 사용.
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
    public SkillCoefficient(MercenarySkill mercenarySkill, String rowId,
                            float coefStr, float coefDex, float coefVit, float coefInt,
                            float coefAtk, float coefLvl, int hitCount,
                            float damageRangeFactor, SkillType skillType,
                            Float castsPerSecond, Integer tickIntervalMs,
                            String confidence, String measurementNote) {
        this.mercenarySkill = mercenarySkill;
        this.itemSkill = null;
        this.rowId = rowId;
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

    /** 세트 부여 스킬용 생성자 */
    @Builder(builderMethodName = "ofSetGrantedSkill")
    public SkillCoefficient(SetGrantedSkill setGrantedSkill, String rowId,
                            float coefStr, float coefDex, float coefVit, float coefInt,
                            float coefAtk, float coefLvl, int hitCount,
                            float damageRangeFactor, SkillType skillType,
                            Float castsPerSecond, Integer tickIntervalMs,
                            String confidence, String measurementNote) {
        this.mercenarySkill = null;
        this.itemSkill = null;
        this.setGrantedSkill = setGrantedSkill;
        this.rowId = rowId;
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
    public SkillCoefficient(ItemSkill itemSkill, String rowId,
                            float coefStr, float coefDex, float coefVit, float coefInt,
                            float coefAtk, float coefLvl, int hitCount,
                            float damageRangeFactor, SkillType skillType,
                            Float castsPerSecond, Integer tickIntervalMs,
                            String confidence, String measurementNote) {
        this.mercenarySkill = null;
        this.itemSkill = itemSkill;
        this.rowId = rowId;
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

    /** 스킬 계수 전체 필드 업데이트. */
    public void updateCoefficients(float coefStr, float coefDex, float coefVit, float coefInt,
                                     float coefAtk, float coefLvl, int hitCount,
                                     float damageRangeFactor, SkillType skillType,
                                     Float castsPerSecond, Integer tickIntervalMs,
                                     String confidence, String note) {
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
        this.measurementNote = note;
    }

    /** rowId 업데이트 — 수동 생성 항목에 나중에 rowId를 매핑할 때 사용. */
    public void updateRowId(String rowId) {
        this.rowId = rowId;
    }

    /** 직접 측정값 업데이트 — castsPerSecond(INSTANT) 또는 tickIntervalMs(PERSISTENT). */
    public void updateMeasurement(Float castsPerSecond, Integer tickIntervalMs, String measurementNote) {
        this.castsPerSecond = castsPerSecond;
        this.tickIntervalMs = tickIntervalMs;
        this.measurementNote = measurementNote;
    }

    public boolean isMercenarySkill() {
        return mercenarySkill != null;
    }

    public boolean isItemSkill() {
        return itemSkill != null;
    }

    public boolean isSetGrantedSkill() {
        return setGrantedSkill != null;
    }
}
