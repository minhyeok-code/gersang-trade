package org.example.gersangtrade.admin.dto.skill;

import org.example.gersangtrade.domain.catalog.enums.SkillBehaviorType;

/** 아이템 스킬 수정 요청 — 모든 필드 nullable, null이 아닌 값만 업데이트 */
public record ItemSkillUpdateRequest(
        String skillName,
        SkillBehaviorType skillBehaviorType,
        Boolean replacesBaseSkill,
        Integer triggerEveryN,
        String triggerBaseSkillKey,
        String note
) {}
