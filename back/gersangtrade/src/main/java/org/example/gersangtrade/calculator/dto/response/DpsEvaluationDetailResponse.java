package org.example.gersangtrade.calculator.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import org.example.gersangtrade.calculator.overlay.MercenaryMode;
import org.example.gersangtrade.calculator.overlay.ScenarioItemType;
import org.example.gersangtrade.domain.calculator.DpsValueEvaluation;

import java.time.LocalDateTime;

/**
 * 가성비 평가 상세 — 목록 요약 + 덱 상태 + 재평가용 요청 JSON.
 */
public record DpsEvaluationDetailResponse(
        Long evaluationId,
        Long deckId,
        EvaluationDeckStatus deckStatus,
        ScenarioItemType candidateType,
        String candidateLabel,
        Long candidateRef,
        MercenaryMode mercenaryMode,
        Long monsterId,
        String monsterName,
        Long baselineDeckSnapshotId,
        Long scenarioDeckSnapshotId,
        double finalDpsIncreaseRate,
        Double efficiencyPerEokFinal,
        Long price,
        String formattedPrice,
        PriceSource priceSource,
        LocalDateTime createdAt,
        DpsValueEvaluationResponse metrics,
        /** 재평가 시 POST body로 그대로 사용 가능 — null이면 구 기록 */
        JsonNode requestJson
) {
    public static DpsEvaluationDetailResponse of(
            DpsValueEvaluation eval,
            String candidateLabel,
            EvaluationDeckStatus deckStatus,
            JsonNode requestJson) {
        return new DpsEvaluationDetailResponse(
                eval.getId(),
                eval.getDeckId(),
                deckStatus,
                eval.getCandidateType(),
                candidateLabel,
                eval.getCandidateRef(),
                eval.getMercenaryMode(),
                eval.getMonster().getId(),
                eval.getMonster().getName(),
                eval.getBaselineDeckSnapshot() != null ? eval.getBaselineDeckSnapshot().getId() : null,
                eval.getScenarioDeckSnapshot() != null ? eval.getScenarioDeckSnapshot().getId() : null,
                eval.getFinalDpsIncreaseRate(),
                eval.getEfficiencyPerEokFinal(),
                eval.getPrice(),
                formatPrice(eval.getPrice()),
                eval.getPriceSource(),
                eval.getCreatedAt(),
                DpsValueEvaluationResponse.ofStored(eval),
                requestJson
        );
    }

    private static String formatPrice(Long price) {
        if (price == null) return null;
        long eok = price / 100_000_000L;
        long cheon = (price % 100_000_000L) / 10_000_000L;
        if (eok > 0 && cheon > 0) return eok + "억 " + cheon + "천만";
        if (eok > 0) return eok + "억";
        if (cheon > 0) return cheon + "천만";
        return price + "전";
    }
}
