package org.example.gersangtrade.admin.dto.request;

import jakarta.validation.constraints.NotNull;
import org.example.gersangtrade.domain.catalog.enums.StatType;

import java.util.List;

public record SkillEffectReplaceRequest(
        @NotNull List<EffectEntry> effects
) {
    public record EffectEntry(
            @NotNull StatType statKey,
            @NotNull Integer statValue
    ) {}
}
