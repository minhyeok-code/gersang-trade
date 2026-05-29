package org.example.gersangtrade.trade.repository;

import org.example.gersangtrade.domain.trade.TradeStatDaily;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 일별 거래 통계 레포지토리.
 * 시세 조회 및 거래 확정 시 upsert 패턴에 사용된다.
 */
public interface TradeStatDailyRepository extends JpaRepository<TradeStatDaily, Long> {

    /**
     * statKey + statDate로 기존 통계 레코드 조회.
     * 거래 확정 시 upsert(있으면 accumulate, 없으면 insert) 패턴에 사용된다.
     */
    Optional<TradeStatDaily> findByStatKeyAndStatDate(String statKey, LocalDate statDate);

    /**
     * 일별 통계 원자적 upsert — 동시 거래 확정 시 duplicate key 경합 방지.
     */
    // clearAutomatically=false — finalizeTrade 직후 User lazy proxy가 detached 되지 않도록 유지
    @Modifying(clearAutomatically = false, flushAutomatically = true)
    @Query(value = """
            INSERT INTO trade_stat_daily
                (stat_date, stat_key, trade_count, quantity_sum, price_sum, price_min, price_max)
            VALUES
                (:statDate, :statKey, 1, :quantity, :price, :price, :price)
            ON DUPLICATE KEY UPDATE
                trade_count = trade_count + 1,
                quantity_sum = quantity_sum + :quantity,
                price_sum = price_sum + :price,
                price_min = LEAST(price_min, :price),
                price_max = GREATEST(price_max, :price)
            """, nativeQuery = true)
    void upsertAccumulate(@Param("statDate") LocalDate statDate,
                          @Param("statKey") String statKey,
                          @Param("price") long price,
                          @Param("quantity") long quantity);

    /**
     * 특정 집계키의 기간별 일별 통계 조회 (최신순).
     * 시세 조회 API에서 사용된다.
     */
    @Query("SELECT s FROM TradeStatDaily s " +
           "WHERE s.statKey = :statKey AND s.statDate BETWEEN :from AND :to " +
           "ORDER BY s.statDate DESC")
    List<TradeStatDaily> findByStatKeyAndDateRange(
            @Param("statKey") String statKey,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);
}
