package org.example.gersangtrade.trade.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class TradeReviewTimingTest {

    @Test
    @DisplayName("computeRevealAt_마감15시_다음날02시공개")
    void computeRevealAt_마감15시_다음날02시공개() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 5, 15, 0);

        LocalDateTime revealAt = TradeReviewTiming.computeRevealAt(createdAt);

        assertThat(revealAt).isEqualTo(LocalDateTime.of(2026, 1, 9, 2, 0));
    }

    @Test
    @DisplayName("computeRevealAt_마감02시_당일02시공개")
    void computeRevealAt_마감02시_당일02시공개() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 5, 2, 0);

        LocalDateTime revealAt = TradeReviewTiming.computeRevealAt(createdAt);

        assertThat(revealAt).isEqualTo(LocalDateTime.of(2026, 1, 8, 2, 0));
    }

    @Test
    @DisplayName("isEvaluationOpen_3일미만_true")
    void isEvaluationOpen_3일미만_true() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 5, 10, 0);
        LocalDateTime now = LocalDateTime.of(2026, 1, 7, 23, 59);

        assertThat(TradeReviewTiming.isEvaluationOpen(createdAt, now)).isTrue();
    }

    @Test
    @DisplayName("isEvaluationOpen_3일경과_false")
    void isEvaluationOpen_3일경과_false() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 5, 10, 0);
        LocalDateTime now = LocalDateTime.of(2026, 1, 8, 10, 0);

        assertThat(TradeReviewTiming.isEvaluationOpen(createdAt, now)).isFalse();
    }
}
