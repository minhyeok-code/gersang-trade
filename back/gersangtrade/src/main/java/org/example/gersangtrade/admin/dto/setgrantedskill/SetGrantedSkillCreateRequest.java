package org.example.gersangtrade.admin.dto.setgrantedskill;

import jakarta.validation.constraints.NotBlank;
import org.example.gersangtrade.domain.catalog.enums.SkillBehaviorType;
import org.example.gersangtrade.domain.catalog.enums.StatSource;
import org.example.gersangtrade.domain.catalog.enums.TriggerSource;

public record SetGrantedSkillCreateRequest(
        @NotBlank String skillKey,
        @NotBlank String skillName,
        SkillBehaviorType skillBehaviorType,
        StatSource statSource,
        TriggerSource triggerSource,
        Integer triggerEveryN,
        String triggerBaseSkillKey,
        String note
) {}
