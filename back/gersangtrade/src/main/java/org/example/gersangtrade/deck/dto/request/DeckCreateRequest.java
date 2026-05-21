package org.example.gersangtrade.deck.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeckCreateRequest(
        @NotBlank @Size(max = 50) String name
) {}
