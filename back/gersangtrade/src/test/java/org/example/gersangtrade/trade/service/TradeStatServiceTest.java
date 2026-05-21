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
    @DisplayName("upsertDailyStat_신규레코드_없으면_insert")
    void upsertDailyStat_신규레코드_없으면_insert() {
        // 기존 레코드 없음
        when(tradeStatDailyRepository.findByStatKeyAndStatDate(STAT_KEY, TODAY))
                .thenReturn(Optional.empty());

        tradeStatService.upsertDailyStat(STAT_KEY, 500_000_000L, 1L, TODAY);

        // save() 호출 확인
        ArgumentCaptor<TradeStatDaily> captor = ArgumentCaptor.forClass(TradeStatDaily.class);
        verify(tradeStatDailyRepository).save(captor.capture());
        TradeStatDaily saved = captor.getValue();
        assertThat(saved.getStatKey()).isEqualTo(STAT_KEY);
        assertThat(saved.getStatDate()).isEqualTo(TODAY);
        assertThat(saved.getTradeCount()).isEqualTo(1);
        assertThat(saved.getPriceSum()).isEqualTo(500_000_000L);
        assertThat(saved.getPriceMin()).isEqualTo(500_000_000L);
        assertThat(saved.getPriceMax()).isEqualTo(500_000_000L);
    }

    @Test
    @DisplayName("upsertDailyStat_기존레코드_있으면_accumulate")
    void upsertDailyStat_기존레코드_있으면_accumulate() {
        // 기존 레코드: 1건, 가격 500만
        TradeStatDaily existing = spy(TradeStatDaily.builder()
                .statKey(STAT_KEY)
                .statDate(TODAY)
                .tradeCount(1)
                .quantitySum(1L)
                .priceSum(500_000_000L)
                .priceMin(500_000_000L)
                .priceMax(500_000_000L)
                .build());
        when(tradeStatDailyRepository.findByStatKeyAndStatDate(STAT_KEY, TODAY))
                .thenReturn(Optional.of(existing));

        // 새 거래: 가격 600만
        tradeStatService.upsertDailyStat(STAT_KEY, 600_000_000L, 1L, TODAY);

        // accumulate 호출됐는지 확인 (save는 호출되지 않아야 함)
        verify(existing).accumulate(600_000_000L, 1L);
        verify(tradeStatDailyRepository, never()).save(any());
    }

    @Test
    @DisplayName("upsertDailyStat_accumulate_후_priceMin_갱신")
    void upsertDailyStat_accumulate_후_priceMin_갱신() {
        // 기존 레코드: 최솟값 500만
        TradeStatDaily existing = TradeStatDaily.builder()
                .statKey(STAT_KEY)
                .statDate(TODAY)
                .tradeCount(1)
                .quantitySum(1L)
                .priceSum(500_000_000L)
                .priceMin(500_000_000L)
                .priceMax(500_000_000L)
                .build();
        when(tradeStatDailyRepository.findByStatKeyAndStatDate(STAT_KEY, TODAY))
                .thenReturn(Optional.of(existing));

        // 더 낮은 가격으로 거래
        tradeStatService.upsertDailyStat(STAT_KEY, 300_000_000L, 1L, TODAY);

        assertThat(existing.getPriceMin()).isEqualTo(300_000_000L);
        assertThat(existing.getPriceMax()).isEqualTo(500_000_000L);
        assertThat(existing.getTradeCount()).isEqualTo(2);
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
