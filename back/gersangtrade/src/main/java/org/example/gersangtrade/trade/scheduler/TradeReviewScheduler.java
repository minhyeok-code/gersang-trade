package org.example.gersangtrade.trade.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gersangtrade.trade.service.TradeReviewService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 거래 평가 공개 스케줄러.
 * 매일 새벽 2시에 revealAt이 경과한 평가를 일괄 공개 처리한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TradeReviewScheduler {

    private final TradeReviewService tradeReviewService;

    /**
     * 매일 새벽 2:00 — 평가 공개 배치 실행.
     * cron: "초 분 시 일 월 요일"
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void publishPendingReviews() {
        log.info("[TradeReviewScheduler] 평가 공개 배치 시작");
        try {
            tradeReviewService.publishPendingReviews();
        } catch (Exception e) {
            log.error("[TradeReviewScheduler] 평가 공개 배치 실패", e);
        }
    }
}
