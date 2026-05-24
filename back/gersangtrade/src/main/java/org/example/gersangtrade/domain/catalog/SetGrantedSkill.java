package org.example.gersangtrade.domain.catalog;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.gersangtrade.domain.catalog.enums.SkillBehaviorType;
import org.example.gersangtrade.domain.catalog.enums.StatSource;
import org.example.gersangtrade.domain.catalog.enums.TriggerSource;

/**
 * 세트 n종 달성 시 부여되는 스킬 정의.
 * EquipmentSetSkillEffect가 어떤 세트의 몇 종 달성 시 이 스킬이 발동되는지를 연결한다.
 *
 * <p>예: 최무선 10강 7종 → 천자총통:개량
 */
@Entity
@Table(name = "set_granted_skills")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SetGrantedSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 거니버스 내부 스킬 식별 키. SkillCoefficient와의 연결 기준. */
    @Column(name = "skill_key", nullable = false, length = 100)
    private String skillKey;

    /** 화면에 표시할 스킬명 — 예: "천자총통:개량" */
    @Column(name = "skill_name", nullable = false, length = 100)
    private String skillName;

    /**
     * 스킬 발동 방식.
     * ACTIVE: 능동 시전. TRIGGER: 조건 충족 시 자동 발동. PASSIVE: 상시 적용.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "skill_behavior_type", length = 20)
    private SkillBehaviorType skillBehaviorType;

    /**
     * 스탯 참조 대상.
     * SELF: 스킬 보유 전설장수 본인 스탯으로 계산.
     * AFFINITY: 인연에 연결된 사천왕의 스탯으로 계산 (MVP 제외 — note로 표시).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "stat_source", length = 20)
    private StatSource statSource;

    /**
     * 트리거 카운트 발생 주체.
     * SELF: 스킬 보유 용병 본인의 시전 횟수 카운트.
     * MERCENARY: 인연에 연결된 사천왕의 시전 횟수 카운트 (MVP 제외 — note로 표시).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_source", length = 20)
    private TriggerSource triggerSource;

    /**
     * 트리거 스킬의 발동 주기.
     * TRIGGER 타입 전용. N번 스킬 시전마다 1회 발동.
     * ACTIVE·PASSIVE이면 null.
     */
    @Column(name = "trigger_every_n")
    private Integer triggerEveryN;

    /**
     * 트리거 카운트 기준 스킬 키.
     * 어떤 스킬의 시전 횟수를 카운트할지 지정한다.
     * TRIGGER 타입 전용.
     */
    @Column(name = "trigger_base_skill_key", length = 100)
    private String triggerBaseSkillKey;

    /** 계산 제외 사유 또는 특이사항 메모. AFFINITY·MERCENARY 타입은 note로 표시. */
    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Builder
    public SetGrantedSkill(String skillKey, String skillName, SkillBehaviorType skillBehaviorType,
                           StatSource statSource, TriggerSource triggerSource,
                           Integer triggerEveryN, String triggerBaseSkillKey, String note) {
        this.skillKey = skillKey;
        this.skillName = skillName;
        this.skillBehaviorType = skillBehaviorType;
        this.statSource = statSource;
        this.triggerSource = triggerSource;
        this.triggerEveryN = triggerEveryN;
        this.triggerBaseSkillKey = triggerBaseSkillKey;
        this.note = note;
    }

    /** 스킬 분류 정보 갱신 */
    public void updateInfo(String skillName, SkillBehaviorType skillBehaviorType, StatSource statSource,
                           TriggerSource triggerSource, Integer triggerEveryN,
                           String triggerBaseSkillKey, String note) {
        this.skillName = skillName;
        this.skillBehaviorType = skillBehaviorType;
        this.statSource = statSource;
        this.triggerSource = triggerSource;
        this.triggerEveryN = triggerEveryN;
        this.triggerBaseSkillKey = triggerBaseSkillKey;
        this.note = note;
    }
}
