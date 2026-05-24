package org.example.gersangtrade.domain.catalog;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.gersangtrade.domain.catalog.enums.SkillBehaviorType;

/**
 * 아이템 고유 스킬 엔티티.
 * 거상짱 아이템 페이지의 .w-stat 셀에서 수치 없이 텍스트만 표기되는 스킬명을 저장한다.
 * 예: 도선빙의, 화염방패, 신선도술 등
 *
 * <p>skillKey는 거니버스 내부 식별 키다. 거상짱 크롤링 시에는 null이며,
 * 거니버스 데이터 적재 후 채워진다. SkillCoefficient와의 연결 키로 사용된다.
 */
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

    /** 거니버스 내부 스킬 식별 키 — 예: "dmsgktnrkd". 거상짱 크롤링 시에는 null. */
    @Column(name = "skill_key", length = 100)
    private String skillKey;

    /**
     * 스킬 발동 방식.
     * ACTIVE: 능동 시전. TRIGGER: 조건 충족 시 자동 발동. PASSIVE: 상시 적용.
     * 수동 분류 필드. 거니버스 크롤링 이후 채워진다.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "skill_behavior_type", length = 20)
    private SkillBehaviorType skillBehaviorType;

    /**
     * 기본 공격을 대체하는 스킬 여부.
     * true이면 이 스킬이 발동하는 동안 기본 공격이 나가지 않는다.
     */
    @Column(name = "replaces_base_skill", nullable = false)
    private boolean replacesBaseSkill = false;

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
     * TRIGGER 타입 전용. 거니버스 skill_key 값.
     */
    @Column(name = "trigger_base_skill_key", length = 100)
    private String triggerBaseSkillKey;

    /** 계산 제외 사유 또는 특이사항 메모. */
    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Builder
    public ItemSkill(Item item, String skillName, String skillKey,
                     SkillBehaviorType skillBehaviorType, boolean replacesBaseSkill,
                     Integer triggerEveryN, String triggerBaseSkillKey, String note) {
        this.item = item;
        this.skillName = skillName;
        this.skillKey = skillKey;
        this.skillBehaviorType = skillBehaviorType;
        this.replacesBaseSkill = replacesBaseSkill;
        this.triggerEveryN = triggerEveryN;
        this.triggerBaseSkillKey = triggerBaseSkillKey;
        this.note = note;
    }

    public void updateSkillKey(String skillKey) {
        this.skillKey = skillKey;
    }

    /** 거니버스 데이터 적재 시 스킬 분류 정보 갱신 */
    public void updateBehavior(SkillBehaviorType skillBehaviorType, boolean replacesBaseSkill,
                               Integer triggerEveryN, String triggerBaseSkillKey, String note) {
        this.skillBehaviorType = skillBehaviorType;
        this.replacesBaseSkill = replacesBaseSkill;
        this.triggerEveryN = triggerEveryN;
        this.triggerBaseSkillKey = triggerBaseSkillKey;
        this.note = note;
    }
}
