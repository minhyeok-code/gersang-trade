package org.example.gersangtrade.deck.dto.request;

import jakarta.validation.constraints.Size;

public record DeckUpdateRequest(
        @Size(max = 50) String name,   // null이면 이름 미변경
        Boolean active                  // null이면 활성 여부 미변경
) {}
