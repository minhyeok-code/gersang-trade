package org.example.gersangtrade.home.dto;

import org.example.gersangtrade.domain.trade.TradeConfirmed;

import java.time.LocalDateTime;

/**
 * 거래 확정 요약 스냅샷 DTO (관심 아이템 시세 거래완료 탭용).
 */
public record CompletedSnapshot(
        Long confirmedPrice,
        LocalDateTime confirmedAt
) {
    public static CompletedSnapshot from(TradeConfirmed trade) {
        return new CompletedSnapshot(trade.getConfirmedPrice(), trade.getConfirmedAt());
    }
}
