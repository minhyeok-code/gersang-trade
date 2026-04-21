package org.example.gersangtrade.trade.repository;

import org.example.gersangtrade.domain.trade.TradeStatDaily;
import org.springframework.data.jpa.repository.JpaRepository;
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
