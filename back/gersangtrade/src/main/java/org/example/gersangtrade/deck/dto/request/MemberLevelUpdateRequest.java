package org.example.gersangtrade.deck.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * 덱 멤버 레벨 수정 요청 DTO.
 * level: 250 또는 260 (다른 값은 400 반환).
 */
public record MemberLevelUpdateRequest(
        @NotNull Integer level
) {}
