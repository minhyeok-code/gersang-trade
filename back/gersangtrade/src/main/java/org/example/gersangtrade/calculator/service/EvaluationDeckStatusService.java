package org.example.gersangtrade.calculator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.calculator.dto.request.DpsEvaluationRequest;
import org.example.gersangtrade.calculator.dto.request.DpsRequest;
import org.example.gersangtrade.calculator.dto.request.ResistanceType;
import org.example.gersangtrade.calculator.dto.response.DpsResponse;
import org.example.gersangtrade.calculator.dto.response.EvaluationDeckStatus;
import org.example.gersangtrade.deck.repository.UserDeckRepository;
import org.example.gersangtrade.domain.calculator.DpsValueEvaluation;
import org.example.gersangtrade.domain.hunt.DeckSnapshot;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 가성비 평가 기록의 기준 덱 vs 현재 덱 일치 여부를 판별한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EvaluationDeckStatusService {

    private final UserDeckRepository deckRepository;
    private final DpsCalculatorService dpsCalculatorService;
    private final EvaluationSnapshotBuilder snapshotBuilder;
    private final ObjectMapper objectMapper;

    public EvaluationDeckStatus resolve(Long userId, DpsValueEvaluation evaluation) {
        DeckSnapshot baseline = evaluation.getBaselineDeckSnapshot();
        if (baseline == null) {
            return EvaluationDeckStatus.UNKNOWN;
        }

        DpsEvaluationRequest request = parseRequest(evaluation.getRequestJson());
        if (request == null) {
            return EvaluationDeckStatus.UNKNOWN;
        }

        if (!deckRepository.existsByIdAndUser_Id(request.deckId(), userId)) {
            return EvaluationDeckStatus.DECK_DELETED;
        }

        try {
            EvaluationSnapshotBuilder.ContentResult current = buildCurrentContent(request);
            return baseline.getContentHash().equals(current.contentHash())
                    ? EvaluationDeckStatus.CURRENT
                    : EvaluationDeckStatus.STALE;
        } catch (Exception e) {
            return EvaluationDeckStatus.UNKNOWN;
        }
    }

    /**
     * 현재 덱 baseline 스냅샷 JSON (diff·비교용, DB write 없음).
     */
    public EvaluationSnapshotBuilder.ContentResult buildCurrentBaselineContent(
            Long userId, DpsValueEvaluation evaluation) {
        DpsEvaluationRequest request = parseRequest(evaluation.getRequestJson());
        if (request == null || !deckRepository.existsByIdAndUser_Id(request.deckId(), userId)) {
            return null;
        }
        return buildCurrentContent(request);
    }

    private EvaluationSnapshotBuilder.ContentResult buildCurrentContent(DpsEvaluationRequest request) {
        DpsRequest dpsReq = new DpsRequest(
                request.deckId(),
                request.monsterId(),
                request.resistanceType(),
                request.memberInputs());
        DpsResponse before = dpsCalculatorService.calculateWithOverlay(dpsReq, null);
        ResistanceType resistanceType = request.resistanceType() != null
                ? request.resistanceType()
                : DeckResistanceTypeResolver.resolveLoaded(
                        dpsCalculatorService.prepareState(dpsReq, null).members());

        return snapshotBuilder.buildContent(
                dpsCalculatorService.prepareState(dpsReq, null),
                before,
                resistanceType);
    }

    private DpsEvaluationRequest parseRequest(String requestJson) {
        if (requestJson == null || requestJson.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(requestJson, DpsEvaluationRequest.class);
        } catch (Exception e) {
            return null;
        }
    }
}
