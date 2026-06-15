package org.example.gersangtrade.trade.util;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 거래 평가 일정 계산.
 *
 * <ul>
 *   <li>평가 제출 기간: 생성(거래 확정) 후 3일</li>
 *   <li>revealAt 갱신: 생성 후 2일 경과 시 배치가 공개 예정 시각을 설정</li>
 *   <li>공개·EXP 반영: revealAt(3일째 이후 첫 새벽 2시)에 배치가 처리</li>
 * </ul>
 */
public final class TradeReviewTiming {

    public static final int EVALUATION_DAYS = 3;
    public static final int REVEAL_AT_SCHEDULE_AFTER_DAYS = 2;
    private static final LocalTime BATCH_TIME = LocalTime.of(2, 0);

    private TradeReviewTiming() {
    }

    /** 평가 제출 마감 시각 = createdAt + 3일 */
    public static LocalDateTime evaluationEndsAt(LocalDateTime createdAt) {
        return createdAt.plusDays(EVALUATION_DAYS);
    }

    /**
     * 공개·점수 반영 배치 시각.
     * 평가 마감 이후 첫 새벽 2시(마감이 02:00 이전이면 당일 02:00, 이후면 다음날 02:00).
     */
    public static LocalDateTime computeRevealAt(LocalDateTime createdAt) {
        LocalDateTime evalEnd = evaluationEndsAt(createdAt);
        LocalDateTime candidate = evalEnd.toLocalDate().atTime(BATCH_TIME);
        if (candidate.isBefore(evalEnd)) {
            candidate = candidate.plusDays(1);
        }
        return candidate;
    }

    /** 평가 제출 가능 여부 */
    public static boolean isEvaluationOpen(LocalDateTime createdAt, LocalDateTime now) {
        return now.isBefore(evaluationEndsAt(createdAt));
    }

    /** revealAt 갱신 대상 여부 — 생성 후 2일 경과 */
    public static boolean isReadyForRevealAtSchedule(LocalDateTime createdAt, LocalDateTime now) {
        return !createdAt.plusDays(REVEAL_AT_SCHEDULE_AFTER_DAYS).isAfter(now);
    }
}
