package org.example.gersangtrade.trade.repository;

import org.example.gersangtrade.domain.catalog.Server;
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
     * statKey + statMonth + server로 기존 통계 레코드 조회.
     * 월별 집계 배치 Job의 upsert 패턴에 사용된다.
     */
    Optional<TradeStatMonthly> findByStatKeyAndStatMonthAndServer(String statKey, String statMonth, Server server);

    /**
     * 특정 집계키·서버의 월별 통계 조회 (최신순).
     * 가성비 비교 기능에서 직전 달 avgPrice를 조회할 때 사용된다.
     */
    @Query("SELECT s FROM TradeStatMonthly s " +
           "WHERE s.statKey = :statKey AND s.server.serverId = :serverId " +
           "ORDER BY s.statMonth DESC")
    List<TradeStatMonthly> findByStatKeyOrderByStatMonthDesc(
            @Param("statKey") String statKey,
            @Param("serverId") Integer serverId);

    /**
     * 특정 집계키·서버의 기간 내 월별 통계 조회 (최신순).
     * fromMonth, toMonth는 "YYYY-MM" 형식.
     */
    @Query("SELECT s FROM TradeStatMonthly s " +
           "WHERE s.statKey = :statKey AND s.server.serverId = :serverId " +
           "AND s.statMonth BETWEEN :fromMonth AND :toMonth " +
           "ORDER BY s.statMonth DESC")
    List<TradeStatMonthly> findByStatKeyAndMonthRange(
            @Param("statKey") String statKey,
            @Param("serverId") Integer serverId,
            @Param("fromMonth") String fromMonth,
            @Param("toMonth") String toMonth);

    /**
     * 여러 집계키·서버의 최근 월 통계를 한 번에 조회.
     * CatalogPriceResolverService에서 피스별 시세 배치 조회에 사용된다.
     */
    @Query("SELECT s FROM TradeStatMonthly s " +
           "WHERE s.statKey IN :statKeys AND s.server.serverId = :serverId " +
           "AND s.statMonth = :statMonth")
    List<TradeStatMonthly> findByStatKeysAndServerIdAndMonth(
            @Param("statKeys") List<String> statKeys,
            @Param("serverId") Integer serverId,
            @Param("statMonth") String statMonth);

    /**
     * 단일 집계키·서버·월 시세 조회.
     * CatalogPriceResolverService에서 아이템·세트 단위 시세 조회에 사용된다.
     */
    @Query("SELECT s FROM TradeStatMonthly s " +
           "WHERE s.statKey = :statKey AND s.server.serverId = :serverId " +
           "AND s.statMonth = :statMonth")
    Optional<TradeStatMonthly> findByStatKeyAndServerIdAndMonth(
            @Param("statKey") String statKey,
            @Param("serverId") Integer serverId,
            @Param("statMonth") String statMonth);
}
