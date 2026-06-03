package org.example.gersangtrade.domain.catalog;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.gersangtrade.domain.catalog.enums.StatType;

/**
 * 용병 스킬의 대상 디버프 효과 엔티티.
 * 스킬 사용 시 상대에게 적용되는 저항깎, 속성깎 등을 저장한다.
 * 하나의 스킬이 여러 효과를 가질 수 있다 (예: 저항깎 + 속성깎 동시).
 */
@Entity
@Table(
        name = "mercenary_skill_effects",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_skill_effect_skill_stat",
                columnNames = {"skill_id", "stat_key"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MercenarySkillEffect {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id", nullable = false)
    private MercenarySkill skill;

    /** 효과 종류 — RESIST_PIERCE(저항깎), ELEMENT_PIERCE(속성깎) 등 */
    @Enumerated(EnumType.STRING)
    @Column(name = "stat_key", nullable = false, length = 30)
    private StatType statKey;

    /** 효과 수치 */
    @Column(name = "stat_value", nullable = false)
    private Integer statValue;

    @Builder
    public MercenarySkillEffect(MercenarySkill skill, StatType statKey, Integer statValue) {
        this.skill = skill;
        this.statKey = statKey;
        this.statValue = statValue;
    }

    public void updateValue(int value) {
        this.statValue = value;
    }
}
