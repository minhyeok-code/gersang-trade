package org.example.gersangtrade.admin.dto.setgrantedskill;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.example.gersangtrade.domain.catalog.enums.Enhancement;

public record SkillEffectCreateRequest(
        @Min(1) @NotNull Integer requiredPieces,
        Enhancement enhancement,
        @NotNull Long setGrantedSkillId
) {}
