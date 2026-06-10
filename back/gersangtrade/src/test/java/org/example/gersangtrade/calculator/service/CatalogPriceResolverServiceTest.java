package org.example.gersangtrade.calculator.service;

import org.example.gersangtrade.calculator.dto.response.ItemPriceLine;
import org.example.gersangtrade.calculator.dto.response.PriceResolution;
import org.example.gersangtrade.calculator.dto.response.PriceSource;
import org.example.gersangtrade.domain.catalog.Server;
import org.example.gersangtrade.domain.trade.TradeStatMonthly;
import org.example.gersangtrade.trade.repository.TradeStatMonthlyRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * CatalogPriceResolverService 단위 테스트.
 * §6.2 가격 조회 우선순위를 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class CatalogPriceResolverServiceTest {

    @Mock
    private TradeStatMonthlyRepository tradeStatMonthlyRepository;

    @InjectMocks
    private CatalogPriceResolverService catalogPriceResolverService;

    private static final Integer SERVER_ID = 1;
    private static final String LAST_MONTH = YearMonth.now().minusMonths(1).toString();
    private static final Server SERVER = Server.builder().serverId(SERVER_ID).name("백호").isActive(true).build();

    // ──────────────────────────────────────────────────────────────────────
    // resolveItem 테스트
    // ──────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("resolveItem_override_있으면_USER_INPUT_반환")
    void resolveItem_override있으면_USER_INPUT_반환() {
        PriceResolution result = catalogPriceResolverService.resolveItem(
                101L, SERVER_ID, Map.of("ITEM:101", 500_000_000L));

        assertThat(result.source()).isEqualTo(PriceSource.USER_INPUT);
        assertThat(result.totalPrice()).isEqualTo(500_000_000L);
        assertThat(result.tradeCount()).isNull();
        assertThat(result.serverId()).isEqualTo(SERVER_ID);
    }

    @Test
    @DisplayName("resolveItem_시세있으면_TRADE_STAT_반환")
    void resolveItem_시세있으면_TRADE_STAT_반환() {
        TradeStatMonthly stat = buildMonthlyStat("ITEM:101", 300_000_000L, 12);
        when(tradeStatMonthlyRepository.findByStatKeyAndServerIdAndMonth("ITEM:101", SERVER_ID, LAST_MONTH))
                .thenReturn(Optional.of(stat));

        PriceResolution result = catalogPriceResolverService.resolveItem(101L, SERVER_ID, Map.of());

        assertThat(result.source()).isEqualTo(PriceSource.TRADE_STAT);
        assertThat(result.totalPrice()).isEqualTo(300_000_000L);
        assertThat(result.tradeCount()).isEqualTo(12);
    }

    @Test
    @DisplayName("resolveItem_시세없고_override없으면_MISSING_반환")
    void resolveItem_시세없고_override없으면_MISSING_반환() {
        when(tradeStatMonthlyRepository.findByStatKeyAndServerIdAndMonth(any(), any(), any()))
                .thenReturn(Optional.empty());

        PriceResolution result = catalogPriceResolverService.resolveItem(101L, SERVER_ID, Map.of());

        assertThat(result.source()).isEqualTo(PriceSource.MISSING);
        assertThat(result.totalPrice()).isNull();
    }

    @Test
    @DisplayName("resolveItem_override가_시세보다_우선")
    void resolveItem_override가_시세보다_우선() {
        // 시세가 있어도 override가 먼저
        PriceResolution result = catalogPriceResolverService.resolveItem(
                101L, SERVER_ID, Map.of("ITEM:101", 999_000_000L));

        assertThat(result.source()).isEqualTo(PriceSource.USER_INPUT);
        assertThat(result.totalPrice()).isEqualTo(999_000_000L);
    }

    // ──────────────────────────────────────────────────────────────────────
    // resolveSet 테스트
    // ──────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("resolveSet_SET_override있으면_USER_INPUT_반환")
    void resolveSet_SET_override있으면_USER_INPUT_반환() {
        PriceResolution result = catalogPriceResolverService.resolveSet(
                5L, SERVER_ID, List.of(101L, 102L, 103L),
                Map.of("SET:5", 2_000_000_000L));

        assertThat(result.source()).isEqualTo(PriceSource.USER_INPUT);
        assertThat(result.totalPrice()).isEqualTo(2_000_000_000L);
        assertThat(result.breakdown()).isEmpty();
    }

    @Test
    @DisplayName("resolveSet_SET_시세있으면_TRADE_STAT_반환")
    void resolveSet_SET_시세있으면_TRADE_STAT_반환() {
        TradeStatMonthly stat = buildMonthlyStat("SET:5", 1_800_000_000L, 5);
        when(tradeStatMonthlyRepository.findByStatKeyAndServerIdAndMonth("SET:5", SERVER_ID, LAST_MONTH))
                .thenReturn(Optional.of(stat));

        PriceResolution result = catalogPriceResolverService.resolveSet(
                5L, SERVER_ID, List.of(101L, 102L), Map.of());

        assertThat(result.source()).isEqualTo(PriceSource.TRADE_STAT);
        assertThat(result.totalPrice()).isEqualTo(1_800_000_000L);
        assertThat(result.tradeCount()).isEqualTo(5);
    }

    @Test
    @DisplayName("resolveSet_SET_시세없고_피스_전체_시세있으면_TRADE_STAT_합산")
    void resolveSet_SET_시세없고_피스_전체_시세있으면_TRADE_STAT_합산() {
        when(tradeStatMonthlyRepository.findByStatKeyAndServerIdAndMonth("SET:5", SERVER_ID, LAST_MONTH))
                .thenReturn(Optional.empty());

        TradeStatMonthly piece1 = buildMonthlyStat("ITEM:101", 300_000_000L, 3);
        TradeStatMonthly piece2 = buildMonthlyStat("ITEM:102", 200_000_000L, 2);
        when(tradeStatMonthlyRepository.findByStatKeysAndServerIdAndMonth(
                anyList(), eq(SERVER_ID), eq(LAST_MONTH)))
                .thenReturn(List.of(piece1, piece2));

        PriceResolution result = catalogPriceResolverService.resolveSet(
                5L, SERVER_ID, List.of(101L, 102L), Map.of());

        assertThat(result.source()).isEqualTo(PriceSource.TRADE_STAT);
        assertThat(result.totalPrice()).isEqualTo(500_000_000L);  // 300M + 200M
        assertThat(result.tradeCount()).isNull();  // 합산 시 거래건수 null
        assertThat(result.breakdown()).hasSize(2);
        assertThat(result.breakdown()).allMatch(l -> l.source() == PriceSource.TRADE_STAT);
    }

    @Test
    @DisplayName("resolveSet_일부_피스_시세없고_override있으면_MIXED")
    void resolveSet_일부_피스_시세없고_override있으면_MIXED() {
        when(tradeStatMonthlyRepository.findByStatKeyAndServerIdAndMonth("SET:5", SERVER_ID, LAST_MONTH))
                .thenReturn(Optional.empty());

        // piece1은 시세 있음, piece2는 시세 없음 → override로 보완
        TradeStatMonthly piece1 = buildMonthlyStat("ITEM:101", 300_000_000L, 3);
        when(tradeStatMonthlyRepository.findByStatKeysAndServerIdAndMonth(
                anyList(), eq(SERVER_ID), eq(LAST_MONTH)))
                .thenReturn(List.of(piece1));

        PriceResolution result = catalogPriceResolverService.resolveSet(
                5L, SERVER_ID, List.of(101L, 102L),
                Map.of("ITEM:102", 150_000_000L));

        assertThat(result.source()).isEqualTo(PriceSource.MIXED);
        assertThat(result.totalPrice()).isEqualTo(450_000_000L);  // 300M + 150M
        assertThat(result.breakdown()).hasSize(2);

        ItemPriceLine tradeLine = result.breakdown().stream()
                .filter(l -> l.itemId().equals(101L)).findFirst().orElseThrow();
        assertThat(tradeLine.source()).isEqualTo(PriceSource.TRADE_STAT);

        ItemPriceLine overrideLine = result.breakdown().stream()
                .filter(l -> l.itemId().equals(102L)).findFirst().orElseThrow();
        assertThat(overrideLine.source()).isEqualTo(PriceSource.USER_INPUT);
    }

    @Test
    @DisplayName("resolveSet_피스_시세없고_override도없으면_MISSING")
    void resolveSet_피스_시세없고_override도없으면_MISSING() {
        when(tradeStatMonthlyRepository.findByStatKeyAndServerIdAndMonth("SET:5", SERVER_ID, LAST_MONTH))
                .thenReturn(Optional.empty());
        when(tradeStatMonthlyRepository.findByStatKeysAndServerIdAndMonth(
                anyList(), eq(SERVER_ID), eq(LAST_MONTH)))
                .thenReturn(List.of());

        PriceResolution result = catalogPriceResolverService.resolveSet(
                5L, SERVER_ID, List.of(101L, 102L), Map.of());

        assertThat(result.source()).isEqualTo(PriceSource.MISSING);
        assertThat(result.totalPrice()).isNull();
        assertThat(result.breakdown()).hasSize(2);
        assertThat(result.breakdown()).allMatch(l -> l.source() == PriceSource.MISSING);
    }

    // ──────────────────────────────────────────────────────────────────────
    // 헬퍼
    // ──────────────────────────────────────────────────────────────────────

    private TradeStatMonthly buildMonthlyStat(String statKey, long avgPrice, int tradeCount) {
        return TradeStatMonthly.builder()
                .statMonth(LAST_MONTH)
                .statKey(statKey)
                .avgPrice(avgPrice)
                .tradeCount(tradeCount)
                .quantitySum((long) tradeCount)
                .build();
    }
}
