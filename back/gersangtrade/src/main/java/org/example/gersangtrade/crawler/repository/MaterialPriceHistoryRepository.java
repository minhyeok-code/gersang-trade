package org.example.gersangtrade.crawler.repository;

import org.example.gersangtrade.domain.crawler.MaterialPriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 재료 아이템 월간 가격 집계 JPA 레포지토리.
 * 가성비 계산기 및 크롤러 Batch Job(Job 2)에서 사용된다.
 */
public interface MaterialPriceHistoryRepository extends JpaRepository<MaterialPriceHistory, Long> {

    /**
     * 아이템·서버·연월 조합으로 집계 레코드 조회 — UPSERT 패턴에서 기존 레코드 확인에 사용된다.
     * UNIQUE 제약 (item_id, server_id, year_month)에 대응한다.
     */
    @Query("""
            SELECT mph FROM MaterialPriceHistory mph
            WHERE mph.item.id = :itemId
              AND mph.server.serverId = :serverId
              AND mph.yearMonth = :yearMonth
            """)
    Optional<MaterialPriceHistory> findByItemIdAndServerIdAndYearMonth(
            @Param("itemId") Long itemId,
            @Param("serverId") Integer serverId,
            @Param("yearMonth") String yearMonth);

    /**
     * 아이템·서버 기준으로 최근 N개월 가격 이력 조회 (yearMonth 내림차순).
     * 가성비 계산기에서 최근 시세 조회에 사용된다.
     */
    @Query("""
            SELECT mph FROM MaterialPriceHistory mph
            WHERE mph.item.id = :itemId
              AND mph.server.serverId = :serverId
            ORDER BY mph.yearMonth DESC
            """)
    List<MaterialPriceHistory> findByItemIdAndServerIdOrderByYearMonthDesc(
            @Param("itemId") Long itemId,
            @Param("serverId") Integer serverId);

    /**
     * 서버 ID·연월 기준 전체 가격 집계 조회 (Item fetch join 포함).
     * 가성비 계산기에서 한 번에 전체 아이템 가격 맵을 로드할 때 사용된다.
     */
    @Query("""
            SELECT mph FROM MaterialPriceHistory mph
            JOIN FETCH mph.item
            WHERE mph.server.serverId = :serverId
              AND mph.yearMonth = :yearMonth
            """)
    List<MaterialPriceHistory> findAllByServerIdAndYearMonth(
            @Param("serverId") Integer serverId,
            @Param("yearMonth") String yearMonth);

    /**
     * 특정 연월의 전체 서버 가격 집계 조회 — 관리자 대시보드 통계 확인에 사용된다.
     */
    @Query("""
            SELECT mph FROM MaterialPriceHistory mph
            JOIN FETCH mph.item
            JOIN FETCH mph.server
            WHERE mph.yearMonth = :yearMonth
            """)
    List<MaterialPriceHistory> findAllWithItemAndServerByYearMonth(@Param("yearMonth") String yearMonth);
}
