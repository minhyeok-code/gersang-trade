package org.example.gersangtrade.trade.service;

import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.domain.catalog.Server;
import org.example.gersangtrade.domain.trade.TradeStatDaily;
import org.example.gersangtrade.trade.dto.response.DailyPriceHistoryResponse;
import org.example.gersangtrade.trade.repository.TradeStatDailyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 거래 통계 서비스.
 * 거래 확정 시 일별 통계 upsert 및 시세 조회 API를 담당한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TradeStatService {

    private final TradeStatDailyRepository tradeStatDailyRepository;

    // ──────────────────────────────────────────────────────────────────────
    // 일별 통계 upsert
    // ──────────────────────────────────────────────────────────────────────

    /**
     * 거래 확정 시 일별 통계를 upsert한다.
     * 당일 해당 statKey·서버 레코드가 있으면 accumulate, 없으면 신규 insert.
     *
     * @param statKey        집계 키 (예: "ITEM:1", "SET:2")
     * @param confirmedPrice 거래 확정 가격
     * @param quantity       수량 (재료: 실수량, 장비: 1)
     * @param date           거래 확정 날짜
     * @param server         거래가 이루어진 서버
     */
    @Transactional
    public void upsertDailyStat(String statKey, long confirmedPrice, long quantity, LocalDate date, Server server) {
        tradeStatDailyRepository.upsertAccumulate(date, statKey, server.getServerId(), confirmedPrice, quantity);
    }

    // ──────────────────────────────────────────────────────────────────────
    // 시세 조회
    // ──────────────────────────────────────────────────────────────────────

    /**
     * 아이템의 일별 시세 내역을 조회한다.
     * statKey는 "ITEM:{itemId}" 형식을 사용한다.
     *
     * @param itemId   아이템 ID
     * @param from     조회 시작 날짜 (null이면 days 기준으로 계산)
     * @param to       조회 종료 날짜 (null이면 오늘)
     * @param days     조회 일수 (from이 null일 때만 적용, 기본 10)
     * @param serverId 서버 ID
     * @return 일별 시세 목록 (최신순)
     */
    public List<DailyPriceHistoryResponse> getDailyHistory(Long itemId, LocalDate from, LocalDate to, int days, Integer serverId) {
        LocalDate endDate = (to != null) ? to : LocalDate.now();
        LocalDate startDate = (from != null) ? from : endDate.minusDays(days);

        String statKey = "ITEM:" + itemId;
        List<TradeStatDaily> stats =
                tradeStatDailyRepository.findByStatKeyAndDateRange(statKey, startDate, endDate, serverId);
        return stats.stream()
                .map(DailyPriceHistoryResponse::of)
                .toList();
    }
}
