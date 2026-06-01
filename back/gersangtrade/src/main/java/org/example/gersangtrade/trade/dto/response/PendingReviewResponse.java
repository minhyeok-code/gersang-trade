package org.example.gersangtrade.trade.dto.response;

import org.example.gersangtrade.domain.trade.TradeReview;

import java.time.LocalDateTime;

/**
 * 내가 제출해야 할 대기 중인 평가 응답 DTO.
 * GET /api/reviews/pending 에서 사용된다.
 */
public record PendingReviewResponse(
        Long reviewId,
        /** 평가 대상자(상대방) 닉네임 */
        String counterpartyNickname,
        /** 평가 마감 시각 (거래 확정 후 3일) */
        LocalDateTime revealAt,
        /** 알림과 매핑하기 위한 채팅방 ID (nullable — 채팅방 삭제 시 null) */
        Long chatRoomId
) {
    public static PendingReviewResponse of(TradeReview review) {
        Long chatRoomId = (review.getTradeConfirmed() != null
                && review.getTradeConfirmed().getChatRoom() != null)
                ? review.getTradeConfirmed().getChatRoom().getId()
                : null;
        return new PendingReviewResponse(
                review.getId(),
                review.getTarget().getNickname(),
                review.getRevealAt(),
                chatRoomId
        );
    }
}
