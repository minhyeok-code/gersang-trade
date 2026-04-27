package org.example.gersangtrade.crawler.repository;

import org.example.gersangtrade.domain.crawler.MaterialPriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 재료 아이템 일별 가격 집계 JPA 레포지토리.
 * 가성비 계산기 및 크롤러 Batch Job(Job 2)에서 사용된다.
 */
public interface MaterialPriceHistoryRepository extends JpaRepository<MaterialPriceHistory, Long> {

    /**
     * 아이템·서버·날짜 조합으로 집계 레코드 조회 — UPSERT 패턴에서 기존 레코드 확인에 사용된다.
     * UNIQUE 제약 (item_id, server_id, trade_date)에 대응한다.
     */
    @Query("""
            SELECT mph FROM MaterialPriceHistory mph
            WHERE mph.item.id = :itemId
              AND mph.server.serverId = :serverId
              AND mph.tradeDate = :tradeDate
            """)
    Optional<MaterialPriceHistory> findByItemIdAndServerIdAndTradeDate(
            @Param("itemId") Long itemId,
            @Param("serverId") Integer serverId,
            @Param("tradeDate") LocalDate tradeDate);

    /**
     * 아이템·서버 기준으로 가격 이력 조회 (tradeDate 내림차순).
     * 가성비 계산기에서 최근 시세 조회에 사용된다.
     */
    @Query("""
            SELECT mph FROM MaterialPriceHistory mph
            WHERE mph.item.id = :itemId
              AND mph.server.serverId = :serverId
            ORDER BY mph.tradeDate DESC
            """)
    List<MaterialPriceHistory> findByItemIdAndServerIdOrderByTradeDateDesc(
            @Param("itemId") Long itemId,
            @Param("serverId") Integer serverId);

    /**
     * 서버 ID·날짜 기준 전체 가격 집계 조회 (Item fetch join 포함).
     * 가성비 계산기에서 한 번에 전체 아이템 가격 맵을 로드할 때 사용된다.
     */
    @Query("""
            SELECT mph FROM MaterialPriceHistory mph
            JOIN FETCH mph.item
            WHERE mph.server.serverId = :serverId
              AND mph.tradeDate = :tradeDate
            """)
    List<MaterialPriceHistory> findAllByServerIdAndTradeDate(
            @Param("serverId") Integer serverId,
            @Param("tradeDate") LocalDate tradeDate);

    /**
     * 서버 ID 기준 가장 최근 수집 날짜 조회.
     * 가성비 계산기에서 최신 가격 기준일을 결정할 때 사용된다.
     */
    @Query("""
            SELECT MAX(mph.tradeDate) FROM MaterialPriceHistory mph
            WHERE mph.server.serverId = :serverId
            """)
    Optional<LocalDate> findMostRecentTradeDateByServerId(@Param("serverId") Integer serverId);

    /**
     * 서버 ID·날짜 범위 기준 가격 이력 조회 (Item fetch join 포함).
     * 주간 시세 조회 등 날짜 범위 기반 조회에 사용된다.
     */
    @Query("""
            SELECT mph FROM MaterialPriceHistory mph
            JOIN FETCH mph.item
            WHERE mph.server.serverId = :serverId
              AND mph.tradeDate BETWEEN :from AND :to
            ORDER BY mph.tradeDate DESC
            """)
    List<MaterialPriceHistory> findAllByServerIdAndTradeDateBetween(
            @Param("serverId") Integer serverId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    /**
     * 특정 날짜의 전체 서버 가격 집계 조회 — 관리자 대시보드 통계 확인에 사용된다.
     */
    @Query("""
            SELECT mph FROM MaterialPriceHistory mph
            JOIN FETCH mph.item
            JOIN FETCH mph.server
            WHERE mph.tradeDate = :tradeDate
            """)
    List<MaterialPriceHistory> findAllWithItemAndServerByTradeDate(@Param("tradeDate") LocalDate tradeDate);
}
