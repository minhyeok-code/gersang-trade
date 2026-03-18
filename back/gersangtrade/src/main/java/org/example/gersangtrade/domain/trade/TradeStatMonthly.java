package org.example.gersangtrade.domain.trade;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 월별 거래 통계 집계 엔티티.
 * TradeStatDaily를 월 단위로 롤업한 결과를 보관한다.
 * 가성비(ValueMetricMonthly) 계산의 기반 데이터로 사용된다.
 * statMonth는 "YYYY-MM" 형식의 문자열로 관리한다.
 */
@Entity
@Table(
        name = "trade_stat_monthly",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_trade_stat_monthly_month_key",
                columnNames = {"stat_month", "stat_key"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TradeStatMonthly {

    /** 월별 통계 고유 식별자 (자동 증가) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 집계 월 — "YYYY-MM" 형식 (예: "2026-03") */
    @Column(name = "stat_month", nullable = false, length = 7)
    private String statMonth;

    /**
     * 집계 키.
     * 예: "ITEM:1", "SET:2" — TradeStatDaily.statKey와 동일한 형식.
     */
    @Column(name = "stat_key", nullable = false, length = 255)
    private String statKey;

    /** 월 평균 거래가 */
    @Column(name = "avg_price", nullable = false)
    private Long avgPrice;

    /** 월 총 거래 건수 */
    @Column(name = "trade_count", nullable = false)
    private Integer tradeCount;

    /** 월 수량 합계 */
    @Column(name = "quantity_sum", nullable = false)
    private Long quantitySum;

    /** 월 최저 거래가 — 거래가 없는 경우 null */
    @Column(name = "price_min")
    private Long priceMin;

    /** 월 최고 거래가 — 거래가 없는 경우 null */
    @Column(name = "price_max")
    private Long priceMax;

    @Builder
    public TradeStatMonthly(String statMonth, String statKey,
                             Long avgPrice, Integer tradeCount, Long quantitySum,
                             Long priceMin, Long priceMax) {
        this.statMonth = statMonth;
        this.statKey = statKey;
        this.avgPrice = avgPrice;
        this.tradeCount = tradeCount;
        this.quantitySum = quantitySum;
        this.priceMin = priceMin;
        this.priceMax = priceMax;
    }
}
