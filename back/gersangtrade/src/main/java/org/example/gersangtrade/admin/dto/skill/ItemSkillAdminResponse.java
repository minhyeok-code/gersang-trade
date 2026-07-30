package org.example.gersangtrade.admin.dto.skill;

import org.example.gersangtrade.domain.catalog.ItemSkill;
import org.example.gersangtrade.domain.catalog.enums.SkillBehaviorType;

/** 아이템 스킬 관리자 응답 DTO */
public record ItemSkillAdminResponse(
        Long id,
        String skillName,
        String skillKey,
        SkillBehaviorType skillBehaviorType,
        boolean replacesBaseSkill,
        Integer triggerEveryN,
        String triggerBaseSkillKey,
        String note
) {
    public static ItemSkillAdminResponse from(ItemSkill skill) {
        return new ItemSkillAdminResponse(
                skill.getId(),
                skill.getSkillName(),
                skill.getSkillKey(),
                skill.getSkillBehaviorType(),
                skill.isReplacesBaseSkill(),
                skill.getTriggerEveryN(),
                skill.getTriggerBaseSkillKey(),
                skill.getNote()
        );
    }
}
