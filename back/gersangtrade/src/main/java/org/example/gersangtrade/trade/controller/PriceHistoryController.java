package org.example.gersangtrade.trade.controller;

import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.trade.dto.response.DailyPriceHistoryResponse;
import org.example.gersangtrade.trade.service.TradeStatService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 시세 조회 컨트롤러.
 * 아이템 ID 기준 일별 거래 가격 내역을 제공한다.
 */
@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class PriceHistoryController {

    private final TradeStatService tradeStatService;

    /**
     * 아이템 일별 시세 조회.
     * from~to 기간의 거래 통계를 최신순으로 반환한다.
     * from/to 미입력 시 최근 30일 기준으로 조회한다.
     *
     * @param itemId 아이템 ID
     * @param from   조회 시작일 (yyyy-MM-dd, 선택)
     * @param to     조회 종료일 (yyyy-MM-dd, 선택)
     */
    @GetMapping("/{itemId}/price-history")
    public ResponseEntity<List<DailyPriceHistoryResponse>> getPriceHistory(
            @PathVariable Long itemId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        List<DailyPriceHistoryResponse> history = tradeStatService.getDailyHistory(itemId, from, to);
        return ResponseEntity.ok(history);
    }
}
