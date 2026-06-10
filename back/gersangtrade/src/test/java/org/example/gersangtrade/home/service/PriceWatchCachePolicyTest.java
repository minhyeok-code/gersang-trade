package org.example.gersangtrade.home.service;

import org.example.gersangtrade.domain.user.enums.WatchTargetType;
import org.example.gersangtrade.home.dto.BuySummary;
import org.example.gersangtrade.home.dto.CompletedSummary;
import org.example.gersangtrade.home.dto.PriceWatchResponse;
import org.example.gersangtrade.home.dto.PriceWatchTargetResponse;
import org.example.gersangtrade.home.dto.SellSummary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PriceWatchCachePolicyTest {

    private final PriceWatchCachePolicy policy = new PriceWatchCachePolicy();

    @Test
    @DisplayName("shouldNotCache_targets비어있으면_true")
    void shouldNotCache_targets비어있으면_true() {
        assertThat(policy.shouldNotCache(new PriceWatchResponse(1, "테스트", List.of())))
                .isTrue();
    }

    @Test
    @DisplayName("shouldNotCache_가격데이터없으면_true")
    void shouldNotCache_가격데이터없으면_true() {
        PriceWatchTargetResponse target = new PriceWatchTargetResponse(
                1L, WatchTargetType.ITEM, "ITEM:1", "아이템",
                emptySell(), emptyBuy(), emptyCompleted());
        assertThat(policy.shouldNotCache(new PriceWatchResponse(1, "테스트", List.of(target))))
                .isTrue();
    }

    @Test
    @DisplayName("shouldNotCache_판매데이터있으면_false")
    void shouldNotCache_판매데이터있으면_false() {
        SellSummary sell = new SellSummary(100L, 1, List.of());
        PriceWatchTargetResponse target = new PriceWatchTargetResponse(
                1L, WatchTargetType.ITEM, "ITEM:1", "아이템",
                sell, emptyBuy(), emptyCompleted());
        assertThat(policy.shouldNotCache(new PriceWatchResponse(1, "테스트", List.of(target))))
                .isFalse();
    }

    private static SellSummary emptySell() {
        return new SellSummary(null, 0, List.of());
    }

    private static BuySummary emptyBuy() {
        return new BuySummary(null, 0, List.of());
    }

    private static CompletedSummary emptyCompleted() {
        return new CompletedSummary(0, "OK", List.of());
    }
}
