package org.example.gersangtrade.trade.repository;

import org.example.gersangtrade.domain.trade.TradeReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 거래 평가 레포지토리.
 * 블라인드 평가 제출·공개 배치 처리에 사용된다.
 */
public interface TradeReviewRepository extends JpaRepository<TradeReview, Long> {

    /**
     * 특정 거래 확정에 대해 reviewer가 제출한 평가 조회.
     * 중복 제출 방지 및 평가 조회에 사용된다.
     */
    Optional<TradeReview> findByTradeConfirmedIdAndReviewerId(Long tradeConfirmedId, Long reviewerId);

    /**
     * 공개 대상 평가 목록 조회 — 배치 Job용.
     * revealAt이 경과하고 아직 published=false인 평가를 반환한다.
     */
    @Query("SELECT r FROM TradeReview r WHERE r.published = false AND r.revealAt <= :now")
    List<TradeReview> findPendingPublish(@Param("now") LocalDateTime now);

    /**
     * 특정 사용자가 받은 공개된 평가 목록 조회.
     * 사용자 프로필의 거래 평가 이력 표시에 사용된다.
     */
    List<TradeReview> findByTargetIdAndPublishedTrue(Long targetId);
}
