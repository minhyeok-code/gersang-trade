package org.example.gersangtrade.trade.service;

import org.example.gersangtrade.domain.trade.TradeStatDaily;
import org.example.gersangtrade.trade.dto.response.DailyPriceHistoryResponse;
import org.example.gersangtrade.trade.repository.TradeStatDailyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * TradeStatService 단위 테스트.
 * 일별 통계 upsert·시세 조회 로직을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class TradeStatServiceTest {

    @Mock
    private TradeStatDailyRepository tradeStatDailyRepository;

    @InjectMocks
    private TradeStatService tradeStatService;

    private static final String STAT_KEY = "ITEM:1";
    private static final LocalDate TODAY = LocalDate.of(2026, 4, 20);

    // ── upsertDailyStat() 테스트 ──────────────────────────────────────────────

    @Test
    @DisplayName("upsertDailyStat_nativeUpsert_호출")
    void upsertDailyStat_nativeUpsert_호출() {
        tradeStatService.upsertDailyStat(STAT_KEY, 500_000_000L, 1L, TODAY);

        verify(tradeStatDailyRepository).upsertAccumulate(TODAY, STAT_KEY, 500_000_000L, 1L);
    }

    // ── getDailyHistory() 테스트 ──────────────────────────────────────────────

    @Test
    @DisplayName("getDailyHistory_기간지정_정상조회")
    void getDailyHistory_기간지정_정상조회() {
        LocalDate from = LocalDate.of(2026, 4, 1);
        LocalDate to = LocalDate.of(2026, 4, 20);

        TradeStatDaily stat = TradeStatDaily.builder()
                .statKey(STAT_KEY)
                .statDate(LocalDate.of(2026, 4, 10))
                .tradeCount(3)
                .quantitySum(3L)
                .priceSum(1_500_000_000L)
                .priceMin(400_000_000L)
                .priceMax(600_000_000L)
                .build();
        when(tradeStatDailyRepository.findByStatKeyAndDateRange(STAT_KEY, from, to))
                .thenReturn(List.of(stat));

        List<DailyPriceHistoryResponse> result = tradeStatService.getDailyHistory(1L, from, to, 10);

        assertThat(result).hasSize(1);
        DailyPriceHistoryResponse response = result.get(0);
        assertThat(response.statDate()).isEqualTo(LocalDate.of(2026, 4, 10));
        assertThat(response.tradeCount()).isEqualTo(3);
        assertThat(response.avgPrice()).isEqualTo(500_000_000L);  // 1500만 / 3
        assertThat(response.priceMin()).isEqualTo(400_000_000L);
        assertThat(response.priceMax()).isEqualTo(600_000_000L);
    }

    @Test
    @DisplayName("getDailyHistory_기간미지정_days=10_기본범위사용")
    void getDailyHistory_기간미지정_days_기본범위사용() {
        when(tradeStatDailyRepository.findByStatKeyAndDateRange(any(), any(), any()))
                .thenReturn(List.of());

        List<DailyPriceHistoryResponse> result = tradeStatService.getDailyHistory(1L, null, null, 10);

        assertThat(result).isEmpty();
        // 날짜 범위가 (오늘 - 10일 ~ 오늘)으로 호출됐는지 검증
        ArgumentCaptor<LocalDate> fromCaptor = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> toCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(tradeStatDailyRepository).findByStatKeyAndDateRange(
                eq(STAT_KEY), fromCaptor.capture(), toCaptor.capture());
        assertThat(toCaptor.getValue()).isEqualTo(LocalDate.now());
        assertThat(fromCaptor.getValue()).isEqualTo(LocalDate.now().minusDays(10));
    }

    @Test
    @DisplayName("getDailyHistory_데이터없으면_빈리스트반환")
    void getDailyHistory_데이터없으면_빈리스트반환() {
        when(tradeStatDailyRepository.findByStatKeyAndDateRange(any(), any(), any()))
                .thenReturn(List.of());

        List<DailyPriceHistoryResponse> result =
                tradeStatService.getDailyHistory(999L, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), 10);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getDailyHistory_avgPrice_수량1이상_정확계산")
    void getDailyHistory_avgPrice_수량1이상_정확계산() {
        // 재료 아이템: 수량 100개, 총가 1억 → 개당 100만
        TradeStatDaily stat = TradeStatDaily.builder()
                .statKey("ITEM:2")
                .statDate(TODAY)
                .tradeCount(1)
                .quantitySum(100L)
                .priceSum(100_000_000L)
                .priceMin(100_000_000L)
                .priceMax(100_000_000L)
                .build();
        when(tradeStatDailyRepository.findByStatKeyAndDateRange(eq("ITEM:2"), any(), any()))
                .thenReturn(List.of(stat));

        List<DailyPriceHistoryResponse> result =
                tradeStatService.getDailyHistory(2L, TODAY, TODAY, 10);

        assertThat(result.get(0).avgPrice()).isEqualTo(1_000_000L);  // 1억 / 100
    }
}
