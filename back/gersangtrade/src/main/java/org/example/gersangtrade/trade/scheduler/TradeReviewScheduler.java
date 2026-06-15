package org.example.gersangtrade.trade.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gersangtrade.trade.service.TradeReviewService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 거래 평가 일정 스케줄러.
 * 매일 새벽 2시에 (1) 생성 2일 경과 건 revealAt 설정 → (2) revealAt 경과 건 공개 처리.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TradeReviewScheduler {

    private final TradeReviewService tradeReviewService;

    /**
     * 매일 새벽 2:00 — revealAt 설정 후 평가 공개 배치 실행.
     * cron: "초 분 시 일 월 요일"
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void runDailyReviewBatch() {
        log.info("[TradeReviewScheduler] 거래 평가 일정 배치 시작");
        try {
            tradeReviewService.scheduleRevealAt();
            tradeReviewService.publishPendingReviews();
        } catch (Exception e) {
            log.error("[TradeReviewScheduler] 거래 평가 일정 배치 실패", e);
        }
    }
}
