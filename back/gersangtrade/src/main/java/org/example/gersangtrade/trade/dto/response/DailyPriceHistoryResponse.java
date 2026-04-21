package org.example.gersangtrade.trade.dto.response;

import org.example.gersangtrade.domain.trade.TradeStatDaily;

import java.time.LocalDate;

/**
 * 일별 시세 조회 응답 DTO.
 * TradeStatDaily 엔티티의 통계 수치를 클라이언트에 전달한다.
 */
public record DailyPriceHistoryResponse(
        LocalDate statDate,
        int tradeCount,
        long avgPrice,
        long priceMin,
        long priceMax,
        long quantitySum
) {
    /**
     * TradeStatDaily 엔티티로부터 응답 DTO를 생성한다.
     * avgPrice = priceSum / quantitySum (수량 가중 평균가).
     */
    public static DailyPriceHistoryResponse of(TradeStatDaily stat) {
        // 수량 기반 평균가 계산 (0 나누기 방지)
        long avg = (stat.getQuantitySum() > 0)
                ? stat.getPriceSum() / stat.getQuantitySum()
                : 0L;
        return new DailyPriceHistoryResponse(
                stat.getStatDate(),
                stat.getTradeCount(),
                avg,
                stat.getPriceMin(),
                stat.getPriceMax(),
                stat.getQuantitySum()
        );
    }
}
