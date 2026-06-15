package org.example.gersangtrade.calculator.dto.response;

import org.example.gersangtrade.domain.calculator.DpsValueEvaluation;

/**
 * DPS 가성비 평가 실행 응답.
 * §9.2 응답 형식에 대응한다.
 */
public record DpsValueEvaluationResponse(

        boolean persisted,

        /** persist=false이면 null */
        Long evaluationId,

        /** persist=false이면 null */
        Long baselineDeckSnapshotId,

        /** persist=false이면 null */
        Long scenarioDeckSnapshotId,

        DpsTriple before,
        DpsTriple after,
        DpsTriple delta,
        DpsRateTriple increaseRate,
        EfficiencyTriple efficiencyPerEok,

        Long price,
        String formattedPrice,
        PriceSource priceSource,

        /** 시세 기반 가격일 때의 거래 건수 — USER_INPUT·MIXED·MISSING이면 null */
        Integer tradeCount

) {
    /** raw / adjust / final 세 가지 DPS 지표 묶음 (정수) */
    public record DpsTriple(Long raw, Long adjust, Long finalDps) {}

    /** raw / adjust / final DPS 증가율 묶음 (소수점 %) */
    public record DpsRateTriple(Double raw, Double adjust, Double finalDps) {}

    /** raw / adjust / final 가성비 */
    public record EfficiencyTriple(Double raw, Double adjust, Double finalDps) {}

    /** 계산 결과만 반환 (persist=false) */
    public static DpsValueEvaluationResponse ofTransient(
            DpsResponse before, DpsResponse after,
            Long price, PriceSource priceSource, Integer tradeCount) {
        return build(false, null, null, null, before, after, price, priceSource, tradeCount);
    }

    /** 저장 후 반환 (persist=true) */
    public static DpsValueEvaluationResponse ofPersisted(
            DpsValueEvaluation eval, DpsResponse before, DpsResponse after) {
        return build(true,
                eval.getId(),
                eval.getBaselineDeckSnapshot() != null ? eval.getBaselineDeckSnapshot().getId() : null,
                eval.getScenarioDeckSnapshot() != null ? eval.getScenarioDeckSnapshot().getId() : null,
                before, after,
                eval.getPrice(), eval.getPriceSource(), null);
    }

    /** DB 저장 값에서 복원 — GET 상세 조회용 */
    public static DpsValueEvaluationResponse ofStored(DpsValueEvaluation eval) {
        Long baselineId = eval.getBaselineDeckSnapshot() != null
                ? eval.getBaselineDeckSnapshot().getId() : null;
        Long scenarioId = eval.getScenarioDeckSnapshot() != null
                ? eval.getScenarioDeckSnapshot().getId() : null;
        return new DpsValueEvaluationResponse(
                true,
                eval.getId(),
                baselineId,
                scenarioId,
                new DpsTriple(eval.getRawDpsBefore(), eval.getAdjustDpsBefore(), eval.getFinalDpsBefore()),
                new DpsTriple(eval.getRawDpsAfter(),  eval.getAdjustDpsAfter(),  eval.getFinalDpsAfter()),
                new DpsTriple(eval.getRawDpsDelta(),  eval.getAdjustDpsDelta(),  eval.getFinalDpsDelta()),
                new DpsRateTriple(
                        round2(eval.getRawDpsIncreaseRate()),
                        round2(eval.getAdjustDpsIncreaseRate()),
                        round2(eval.getFinalDpsIncreaseRate())),
                new EfficiencyTriple(
                        eval.getEfficiencyPerEokRaw(),
                        eval.getEfficiencyPerEokAdjust(),
                        eval.getEfficiencyPerEokFinal()),
                eval.getPrice(), formatPrice(eval.getPrice()), eval.getPriceSource(), null
        );
    }

    private static double round2(double v) {
        return Math.round(v * 100) / 100.0;
    }

    private static DpsValueEvaluationResponse build(
            boolean persisted,
            Long evaluationId,
            Long baselineDeckSnapshotId,
            Long scenarioDeckSnapshotId,
            DpsResponse before,
            DpsResponse after,
            Long price,
            PriceSource priceSource,
            Integer tradeCount) {

        long rawBefore    = before.rawTotalDps();
        long adjustBefore = before.adjustTotalDps();
        long finalBefore  = before.totalDps();
        long rawAfter     = after.rawTotalDps();
        long adjustAfter  = after.adjustTotalDps();
        long finalAfter   = after.totalDps();

        long rawDelta    = rawAfter    - rawBefore;
        long adjustDelta = adjustAfter - adjustBefore;
        long finalDelta  = finalAfter  - finalBefore;

        double rawRate    = increaseRate(rawBefore,    rawAfter);
        double adjustRate = increaseRate(adjustBefore, adjustAfter);
        double finalRate  = increaseRate(finalBefore,  finalAfter);

        Double effRaw    = efficiency(rawRate,    price);
        Double effAdjust = efficiency(adjustRate, price);
        Double effFinal  = efficiency(finalRate,  price);

        return new DpsValueEvaluationResponse(
                persisted, evaluationId, baselineDeckSnapshotId, scenarioDeckSnapshotId,
                new DpsTriple(rawBefore,  adjustBefore, finalBefore),
                new DpsTriple(rawAfter,   adjustAfter,  finalAfter),
                new DpsTriple(rawDelta,   adjustDelta,  finalDelta),
                new DpsRateTriple(Math.round(rawRate * 100) / 100.0,
                                  Math.round(adjustRate * 100) / 100.0,
                                  Math.round(finalRate * 100) / 100.0),
                new EfficiencyTriple(effRaw, effAdjust, effFinal),
                price, formatPrice(price), priceSource, tradeCount
        );
    }

    /** (after / before - 1) × 100, before ≤ 0이면 0 */
    private static double increaseRate(long before, long after) {
        if (before <= 0) return 0.0;
        return (after / (double) before - 1.0) * 100.0;
    }

    /** 증가율 / 가격(억), 가격 null·0이면 null */
    private static Double efficiency(double increaseRate, Long price) {
        if (price == null || price == 0) return null;
        return increaseRate / (price / 100_000_000.0);
    }

    /** 골드(전 단위)를 "N억 N천만" 형식으로 변환 */
    private static String formatPrice(Long price) {
        if (price == null) return null;
        long eok  = price / 100_000_000L;
        long rest = price % 100_000_000L;
        long cheon = rest / 10_000_000L;
        if (eok > 0 && cheon > 0) return eok + "억 " + cheon + "천만";
        if (eok > 0)               return eok + "억";
        if (cheon > 0)             return cheon + "천만";
        return price + "전";
    }
}
