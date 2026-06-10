package org.example.gersangtrade.domain.trade;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.gersangtrade.domain.catalog.Server;

import java.time.LocalDate;

/**
 * 일별 거래 통계 집계 엔티티.
 * 확정된 거래(TradeConfirmed) 데이터를 날짜·집계키·서버 기준으로 집계한 결과를 보관한다.
 * 시세 조회 시 TradeConfirmed를 직접 집계하는 대신 이 테이블을 조회한다 (성능 최적화).
 * cancelled=true인 TradeConfirmed는 집계에서 제외된다.
 * 이벤트 방식(거래 확정 시 upsert) 또는 야간 배치로 갱신된다.
 */
@Entity
@Table(
        name = "trade_stat_daily",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_trade_stat_daily_date_key_server",
                columnNames = {"stat_date", "stat_key", "server_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TradeStatDaily {

    /** 일별 통계 고유 식별자 (자동 증가) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 집계 날짜 */
    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;

    /** 집계 서버 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "server_id", nullable = false)
    private Server server;

    /**
     * 집계 키.
     * 예: "ITEM:1" (아이템 ID 1번), "SET:2" (세트 ID 2번).
     * TradeConfirmed.statKeySnapshot과 동일한 형식을 사용한다.
     *
     * <p>TODO: MVP(1단계) 단순 문자열 유지. 2단계 확장(주술 조합 집계) 시
     * stat_item_id / stat_ritual_mark / stat_set_id / stat_ritual_count 복합 컬럼으로 전환 필요.
     */
    @Column(name = "stat_key", nullable = false, length = 255)
    private String statKey;

    /** 해당 날짜의 총 거래 건수 */
    @Column(name = "trade_count", nullable = false)
    private Integer tradeCount;

    /** 수량 합계 — 재료 아이템의 개당 평균가 계산에 사용 */
    @Column(name = "quantity_sum", nullable = false)
    private Long quantitySum;

    /** 가격 합계 — 평균가 계산에 사용 (avgPrice = priceSum / quantitySum) */
    @Column(name = "price_sum", nullable = false)
    private Long priceSum;

    /** 해당 날짜의 최저 거래가 */
    @Column(name = "price_min", nullable = false)
    private Long priceMin;

    /** 해당 날짜의 최고 거래가 */
    @Column(name = "price_max", nullable = false)
    private Long priceMax;

    @Builder
    public TradeStatDaily(LocalDate statDate, Server server, String statKey,
                           Integer tradeCount, Long quantitySum,
                           Long priceSum, Long priceMin, Long priceMax) {
        this.statDate = statDate;
        this.server = server;
        this.statKey = statKey;
        this.tradeCount = tradeCount;
        this.quantitySum = quantitySum;
        this.priceSum = priceSum;
        this.priceMin = priceMin;
        this.priceMax = priceMax;
    }

    /**
     * 새 거래 확정 시 통계 업데이트.
     * upsert 패턴에서 기존 레코드에 수치를 누산할 때 사용한다.
     */
    public void accumulate(Long price, Long quantity) {
        this.tradeCount += 1;
        this.quantitySum += quantity;
        this.priceSum += price;
        if (price < this.priceMin) this.priceMin = price;
        if (price > this.priceMax) this.priceMax = price;
    }
}
