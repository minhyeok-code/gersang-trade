package org.example.gersangtrade.deck.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.example.gersangtrade.calculator.dto.request.BonusStatTarget;

/**
 * 덱 멤버 레벨·보너스 스탯 빌드 설정 수정 요청 DTO.
 */
public record MemberBuildUpdateRequest(
        @NotNull Integer level,
        @NotNull BonusStatTarget bonusTarget,
        @Min(0) int bonusAmount
) {}
