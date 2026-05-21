package org.example.gersangtrade.deck.dto.request;

import jakarta.validation.constraints.NotNull;

public record SlotEquipRequest(
        @NotNull Long itemId
) {}
