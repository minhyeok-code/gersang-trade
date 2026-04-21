package org.example.gersangtrade.trade.repository;

import org.example.gersangtrade.domain.trade.TradeStatMonthly;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 월별 거래 통계 레포지토리.
 * 능력치 가성비 비교(ValueMetricMonthly) 계산 및 월별 시세 조회에 사용된다.
 */
public interface TradeStatMonthlyRepository extends JpaRepository<TradeStatMonthly, Long> {

    /**
     * statKey + statMonth로 기존 통계 레코드 조회.
     * 월별 집계 배치 Job의 upsert 패턴에 사용된다.
     */
    Optional<TradeStatMonthly> findByStatKeyAndStatMonth(String statKey, String statMonth);

    /**
     * 특정 집계키의 최근 N개월 월별 통계 조회 (최신순).
     * 가성비 비교 기능에서 직전 달 avgPrice를 조회할 때 사용된다.
     */
    @Query("SELECT s FROM TradeStatMonthly s " +
           "WHERE s.statKey = :statKey " +
           "ORDER BY s.statMonth DESC")
    List<TradeStatMonthly> findByStatKeyOrderByStatMonthDesc(@Param("statKey") String statKey);

    /**
     * 특정 집계키의 기간 내 월별 통계 조회 (최신순).
     * fromMonth, toMonth는 "YYYY-MM" 형식.
     */
    @Query("SELECT s FROM TradeStatMonthly s " +
           "WHERE s.statKey = :statKey AND s.statMonth BETWEEN :fromMonth AND :toMonth " +
           "ORDER BY s.statMonth DESC")
    List<TradeStatMonthly> findByStatKeyAndMonthRange(
            @Param("statKey") String statKey,
            @Param("fromMonth") String fromMonth,
            @Param("toMonth") String toMonth);
}
