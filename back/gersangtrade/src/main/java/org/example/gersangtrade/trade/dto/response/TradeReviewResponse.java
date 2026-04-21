package org.example.gersangtrade.trade.dto.response;

import org.example.gersangtrade.domain.trade.TradeReview;
import org.example.gersangtrade.domain.trade.enums.TradeRating;

import java.time.LocalDateTime;

/**
 * 거래 평가 응답 DTO.
 * published=true인 평가만 외부에 노출된다.
 */
public record TradeReviewResponse(
        Long id,
        /** 평가 선택 — null이면 상대방이 평가를 제출하지 않음 */
        TradeRating rating,
        /** 평가를 남긴 상대방 닉네임 (공개 후 노출) */
        String reviewerNickname,
        LocalDateTime revealAt,
        LocalDateTime createdAt
) {
    public static TradeReviewResponse of(TradeReview review) {
        return new TradeReviewResponse(
                review.getId(),
                review.getRating(),
                review.getReviewer().getNickname(),
                review.getRevealAt(),
                review.getCreatedAt()
        );
    }
}
