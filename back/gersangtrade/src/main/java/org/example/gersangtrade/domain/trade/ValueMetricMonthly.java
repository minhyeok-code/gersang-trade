package org.example.gersangtrade.domain.trade;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.gersangtrade.domain.catalog.Item;
import org.example.gersangtrade.domain.catalog.enums.Element;
import org.example.gersangtrade.domain.catalog.enums.StatType;

/**
 * 월별 가성비(Value-for-Money) 지표 엔티티 (선택적 사전 계산 캐시).
 * 가성비 = avgPrice ÷ statValue — 값이 낮을수록 가성비가 좋음을 의미한다.
 * TradeStatMonthly와 ItemStat을 조합하여 배치 작업으로 사전 계산·저장한다.
 * element는 NONE 포함하여 모든 경우를 저장하며, UNIQUE 제약으로 중복을 방지한다.
 */
@Entity
@Table(
        name = "value_metric_monthly",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_value_metric_monthly_month_item_stat_element",
                columnNames = {"month", "item_id", "stat_type", "element"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ValueMetricMonthly {

    /** 가성비 지표 고유 식별자 (자동 증가) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 집계 월 — "YYYY-MM" 형식 (예: "2026-03") */
    @Column(name = "month", nullable = false, length = 7)
    private String month;

    /** 가성비 대상 아이템 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    /** 비교 기준 능력치 종류 */
    @Enumerated(EnumType.STRING)
    @Column(name = "stat_type", nullable = false, length = 30)
    private StatType statType;

    /** 비교 기준 속성 — NONE 포함 (속성 구분 없는 경우) */
    @Enumerated(EnumType.STRING)
    @Column(name = "element", nullable = false, length = 20)
    private Element element;

    /** 해당 아이템의 능력치 수치 (ItemStat.value 기준) */
    @Column(name = "stat_value", nullable = false)
    private Integer statValue;

    /** 전월 평균 거래가 (TradeStatMonthly.avgPrice 기준) */
    @Column(name = "avg_price", nullable = false)
    private Long avgPrice;

    /**
     * 사전 계산된 가성비 수치.
     * 계산식: avgPrice / statValue — 낮을수록 가성비가 좋다.
     */
    @Column(name = "value_for_money", nullable = false)
    private Double valueForMoney;

    /** 신뢰도 힌트용 거래 건수 — 건수가 적으면 통계 신뢰도가 낮음 */
    @Column(name = "trade_count", nullable = false)
    private Integer tradeCount;

    @Builder
    public ValueMetricMonthly(String month, Item item, StatType statType,
                               Element element, Integer statValue,
                               Long avgPrice, Double valueForMoney, Integer tradeCount) {
        this.month = month;
        this.item = item;
        this.statType = statType;
        this.element = element;
        this.statValue = statValue;
        this.avgPrice = avgPrice;
        this.valueForMoney = valueForMoney;
        this.tradeCount = tradeCount;
    }
}
