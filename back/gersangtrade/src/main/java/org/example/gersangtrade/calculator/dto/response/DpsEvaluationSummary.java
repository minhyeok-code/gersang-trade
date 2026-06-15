package org.example.gersangtrade.calculator.dto.response;

import org.example.gersangtrade.calculator.overlay.MercenaryMode;
import org.example.gersangtrade.calculator.overlay.ScenarioItemType;
import org.example.gersangtrade.domain.calculator.DpsValueEvaluation;

import java.time.LocalDateTime;

/**
 * DPS 가성비 평가 목록 응답 — 핵심 요약 필드만 포함.
 * GET /api/calculator/dps/evaluations
 */
public record DpsEvaluationSummary(

        Long evaluationId,

        Long deckId,

        EvaluationDeckStatus deckStatus,

        ScenarioItemType candidateType,

        /** 후보 표시명 — 아이템명·세트명·용병명 */
        String candidateLabel,

        /** itemId / setId / mercenaryId */
        Long candidateRef,

        MercenaryMode mercenaryMode,

        Long monsterId,
        String monsterName,

        /** final DPS 증가율 (%) */
        double finalDpsIncreaseRate,

        /** final 가성비 — price null이면 null */
        Double efficiencyPerEokFinal,

        Long price,
        String formattedPrice,
        PriceSource priceSource,

        LocalDateTime createdAt

) {
    public static DpsEvaluationSummary from(
            DpsValueEvaluation e,
            String candidateLabel,
            EvaluationDeckStatus deckStatus) {
        return new DpsEvaluationSummary(
                e.getId(),
                e.getDeckId(),
                deckStatus,
                e.getCandidateType(),
                candidateLabel,
                e.getCandidateRef(),
                e.getMercenaryMode(),
                e.getMonster().getId(),
                e.getMonster().getName(),
                e.getFinalDpsIncreaseRate(),
                e.getEfficiencyPerEokFinal(),
                e.getPrice(),
                formatPrice(e.getPrice()),
                e.getPriceSource(),
                e.getCreatedAt()
        );
    }

    private static String formatPrice(Long price) {
        if (price == null) return null;
        long eok   = price / 100_000_000L;
        long cheon = (price % 100_000_000L) / 10_000_000L;
        if (eok > 0 && cheon > 0) return eok + "억 " + cheon + "천만";
        if (eok > 0)               return eok + "억";
        if (cheon > 0)             return cheon + "천만";
        return price + "전";
    }
}
