package org.example.gersangtrade.calculator.service;

import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.calculator.dto.response.ItemPriceLine;
import org.example.gersangtrade.calculator.dto.response.PriceResolution;
import org.example.gersangtrade.calculator.dto.response.PriceSource;
import org.example.gersangtrade.domain.trade.TradeStatMonthly;
import org.example.gersangtrade.trade.repository.TradeStatMonthlyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 가성비 평가용 장비 아이템·세트 시세 조회 서비스.
 *
 * <p>조회 우선순위 (§6.2):</p>
 * <ul>
 *   <li>단품: priceOverrides → TradeStatMonthly → MISSING</li>
 *   <li>세트: SET override → SET 통계 → 피스별 합산(통계+override 보완) → MISSING</li>
 * </ul>
 *
 * <p>용병 시나리오는 이 서비스를 거치지 않는다.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CatalogPriceResolverService {

    private final TradeStatMonthlyRepository tradeStatMonthlyRepository;

    // ──────────────────────────────────────────────────────────────────────
    // 단품 조회
    // ──────────────────────────────────────────────────────────────────────

    /**
     * 단품 아이템 가격 조회 (§6.2 resolveItem 우선순위 ①→③).
     *
     * @param itemId    아이템 ID
     * @param serverId  거래 서버 ID
     * @param overrides 유저 입력 override 맵 — 키: "ITEM:{itemId}"
     */
    public PriceResolution resolveItem(Long itemId, Integer serverId, Map<String, Long> overrides) {
        String key = "ITEM:" + itemId;

        // ① 유저 override
        Long overridePrice = overrides != null ? overrides.get(key) : null;
        if (overridePrice != null) {
            return new PriceResolution(overridePrice, PriceSource.USER_INPUT, null, serverId, List.of());
        }

        // ② TradeStatMonthly
        Optional<TradeStatMonthly> stat = tradeStatMonthlyRepository
                .findByStatKeyAndServerIdAndMonth(key, serverId, latestMonth());
        if (stat.isPresent()) {
            TradeStatMonthly s = stat.get();
            return new PriceResolution(s.getAvgPrice(), PriceSource.TRADE_STAT, s.getTradeCount(), serverId, List.of());
        }

        // ③ MISSING
        return new PriceResolution(null, PriceSource.MISSING, null, serverId, List.of());
    }

    // ──────────────────────────────────────────────────────────────────────
    // 세트 조회
    // ──────────────────────────────────────────────────────────────────────

    /**
     * 세트 가격 조회 (§6.2 resolveSet 우선순위 ①→④).
     *
     * @param setId        세트 ID
     * @param serverId     거래 서버 ID
     * @param pieceItemIds 세트 피스 아이템 ID 목록
     * @param overrides    유저 입력 override 맵 — 키: "SET:{setId}" 또는 "ITEM:{itemId}"
     */
    public PriceResolution resolveSet(Long setId, Integer serverId, List<Long> pieceItemIds,
                                      Map<String, Long> overrides) {
        String setKey = "SET:" + setId;
        String month = latestMonth();

        // ① SET override (세트 총액)
        Long setOverride = overrides != null ? overrides.get(setKey) : null;
        if (setOverride != null) {
            return new PriceResolution(setOverride, PriceSource.USER_INPUT, null, serverId, List.of());
        }

        // ② TradeStatMonthly — SET 단위 통계
        Optional<TradeStatMonthly> setStat = tradeStatMonthlyRepository
                .findByStatKeyAndServerIdAndMonth(setKey, serverId, month);
        if (setStat.isPresent()) {
            TradeStatMonthly s = setStat.get();
            return new PriceResolution(s.getAvgPrice(), PriceSource.TRADE_STAT, s.getTradeCount(), serverId, List.of());
        }

        // ③ 피스별 합산 fallback
        return resolvePiecesSum(serverId, pieceItemIds, overrides, month);
    }

    // ──────────────────────────────────────────────────────────────────────
    // 내부 헬퍼
    // ──────────────────────────────────────────────────────────────────────

    /**
     * 피스 목록의 가격을 합산한다.
     * 시세 없는 피스는 priceOverrides["ITEM:{pieceItemId}"] 로 보완한다.
     */
    private PriceResolution resolvePiecesSum(Integer serverId, List<Long> pieceItemIds,
                                             Map<String, Long> overrides, String month) {
        List<String> pieceKeys = pieceItemIds.stream()
                .map(id -> "ITEM:" + id)
                .toList();

        Map<String, TradeStatMonthly> statByKey = tradeStatMonthlyRepository
                .findByStatKeysAndServerIdAndMonth(pieceKeys, serverId, month)
                .stream()
                .collect(Collectors.toMap(TradeStatMonthly::getStatKey, Function.identity()));

        List<ItemPriceLine> breakdown = new ArrayList<>();
        long total = 0;
        boolean hasUserInput = false;
        boolean hasMissing = false;

        for (Long pieceId : pieceItemIds) {
            String pieceKey = "ITEM:" + pieceId;
            TradeStatMonthly stat = statByKey.get(pieceKey);

            if (stat != null) {
                breakdown.add(new ItemPriceLine(pieceId, stat.getAvgPrice(), PriceSource.TRADE_STAT));
                total += stat.getAvgPrice();
            } else {
                Long pieceOverride = overrides != null ? overrides.get(pieceKey) : null;
                if (pieceOverride != null) {
                    breakdown.add(new ItemPriceLine(pieceId, pieceOverride, PriceSource.USER_INPUT));
                    total += pieceOverride;
                    hasUserInput = true;
                } else {
                    breakdown.add(new ItemPriceLine(pieceId, null, PriceSource.MISSING));
                    hasMissing = true;
                }
            }
        }

        if (hasMissing) {
            // ④ 총액 산출 불가
            return new PriceResolution(null, PriceSource.MISSING, null, serverId, breakdown);
        }

        PriceSource source = hasUserInput ? PriceSource.MIXED : PriceSource.TRADE_STAT;
        return new PriceResolution(total, source, null, serverId, breakdown);
    }

    /** 가격 조회 기준월 — 직전 달 (가장 최근 집계 완료된 월) */
    private static String latestMonth() {
        return YearMonth.now().minusMonths(1).toString();
    }
}
