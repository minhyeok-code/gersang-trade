package org.example.gersangtrade.deck.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 덱 멤버 특성 일괄 저장 요청 DTO.
 * 기존 선택을 전부 교체한다 (PUT 시맨틱).
 */
public record MemberCharacteristicSetRequest(
        @NotNull List<Entry> characteristics
) {
    public record Entry(
            @NotNull Long characteristicId,
            @NotNull @Min(1) Integer selectedLevel
    ) {}
}
