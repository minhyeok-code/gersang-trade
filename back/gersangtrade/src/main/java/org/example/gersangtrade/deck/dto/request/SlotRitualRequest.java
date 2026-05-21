package org.example.gersangtrade.deck.dto.request;

import jakarta.validation.constraints.NotNull;
import org.example.gersangtrade.domain.listing.enums.RitualOutcome;

public record SlotRitualRequest(
        @NotNull Long ritualId,
        @NotNull RitualOutcome outcome
) {}
