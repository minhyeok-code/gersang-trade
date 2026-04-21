package org.example.gersangtrade.trade.repository;

import org.example.gersangtrade.domain.trade.TradeConfirmed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 거래 확정 레포지토리.
 * 통계 집계 및 어뷰징 탐지 쿼리에 사용된다.
 */
public interface TradeConfirmedRepository extends JpaRepository<TradeConfirmed, Long> {

    /**
     * 동일 두 사용자 간 특정 기간 내 거래 건수 조회 — 어뷰징 탐지용.
     * 취소된 거래(cancelled=true)는 제외한다.
     */
    @Query("SELECT COUNT(t) FROM TradeConfirmed t " +
           "WHERE t.cancelled = false " +
           "AND t.confirmedAt >= :since " +
           "AND ((t.seller.id = :userAId AND t.buyer.id = :userBId) " +
           "  OR (t.seller.id = :userBId AND t.buyer.id = :userAId))")
    long countBetweenUsers(@Param("userAId") Long userAId,
                           @Param("userBId") Long userBId,
                           @Param("since") LocalDateTime since);

    /**
     * 특정 사용자의 거래 확정 목록 조회 (판매자 또는 구매자로 참여).
     * 거래내역 페이지 조회에 사용된다.
     */
    @Query("SELECT t FROM TradeConfirmed t " +
           "WHERE t.cancelled = false " +
           "AND (t.seller.id = :userId OR t.buyer.id = :userId) " +
           "ORDER BY t.confirmedAt DESC")
    List<TradeConfirmed> findByUserId(@Param("userId") Long userId);
}
