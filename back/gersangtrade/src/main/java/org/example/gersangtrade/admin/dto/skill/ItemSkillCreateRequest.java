package org.example.gersangtrade.admin.dto.skill;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.example.gersangtrade.domain.catalog.enums.SkillBehaviorType;

/** 아이템 스킬 신규 등록 요청 */
public record ItemSkillCreateRequest(
        @NotBlank @Size(max = 100) String skillName,
        SkillBehaviorType skillBehaviorType,
        boolean replacesBaseSkill,
        Integer triggerEveryN,           // TRIGGER 타입 전용
        String triggerBaseSkillKey,
        String note
) {}
