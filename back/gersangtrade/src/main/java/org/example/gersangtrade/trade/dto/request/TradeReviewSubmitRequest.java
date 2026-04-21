package org.example.gersangtrade.trade.dto.request;

import jakarta.validation.constraints.NotNull;
import org.example.gersangtrade.domain.trade.enums.TradeRating;

/**
 * 거래 평가 제출 요청 DTO.
 * 평가 기간(revealAt 이전) 내에만 제출 가능하다.
 */
public record TradeReviewSubmitRequest(
        @NotNull(message = "평가 선택은 필수입니다.")
        TradeRating rating
) {}
