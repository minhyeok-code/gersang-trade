package org.example.gersangtrade.calculator.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.calculator.dto.response.DpsEvaluationDetailResponse;
import org.example.gersangtrade.calculator.dto.response.DpsEvaluationSummary;
import org.example.gersangtrade.calculator.dto.response.DpsValueEvaluationResponse;
import org.example.gersangtrade.calculator.dto.response.EvaluationDeckDiffResponse;
import org.example.gersangtrade.calculator.dto.response.EvaluationDeckStatus;
import org.example.gersangtrade.calculator.repository.DpsValueEvaluationRepository;
import org.example.gersangtrade.domain.calculator.DpsValueEvaluation;
import org.example.gersangtrade.domain.hunt.DeckSnapshot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * DPS 가성비 평가 조회 서비스.
 * 목록·상세 조회는 DB 저장 값에서 복원하며 DPS 재계산을 수행하지 않는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DpsValueEvaluationQueryService {

    private final DpsValueEvaluationRepository evaluationRepository;
    private final EvaluationCandidateLabelResolver candidateLabelResolver;
    private final EvaluationDeckStatusService deckStatusService;
    private final DeckSnapshotDiffService diffService;
    private final ObjectMapper objectMapper;

    /**
     * 내 평가 목록 — 최신순, 페이징.
     * 본인 데이터만 반환한다.
     */
    public Page<DpsEvaluationSummary> getMyEvaluations(Long userId, Pageable pageable) {
        return evaluationRepository.findByUserIdWithMonster(userId, pageable)
                .map(e -> DpsEvaluationSummary.from(
                        e,
                        candidateLabelResolver.resolve(e),
                        deckStatusService.resolve(userId, e)));
    }

    /**
     * 내 평가 기록 삭제 — 본인 소유만 허용.
     */
    @Transactional
    public void deleteMyEvaluation(Long userId, Long evaluationId) {
        DpsValueEvaluation eval = loadOwned(userId, evaluationId);
        evaluationRepository.delete(eval);
    }

    /**
     * 내 평가 상세 — DB 저장 값 복원.
     * 타인 평가 접근 시 404로 처리한다(존재 여부 노출 방지).
     */
    public DpsValueEvaluationResponse getMyEvaluation(Long userId, Long evaluationId) {
        return DpsValueEvaluationResponse.ofStored(loadOwned(userId, evaluationId));
    }

    /**
     * 내 평가 상세 — 덱 상태·재평가용 requestJson 포함.
     */
    public DpsEvaluationDetailResponse getMyEvaluationDetail(Long userId, Long evaluationId) {
        DpsValueEvaluation eval = loadOwnedWithSnapshots(userId, evaluationId);
        EvaluationDeckStatus deckStatus = deckStatusService.resolve(userId, eval);
        return DpsEvaluationDetailResponse.of(
                eval,
                candidateLabelResolver.resolve(eval),
                deckStatus,
                parseRequestJson(eval.getRequestJson()));
    }

    /**
     * 평가 당시 baseline 덱 vs 현재 덱 장비 diff.
     */
    public EvaluationDeckDiffResponse getDeckDiff(Long userId, Long evaluationId) {
        DpsValueEvaluation eval = loadOwnedWithSnapshots(userId, evaluationId);
        EvaluationDeckStatus deckStatus = deckStatusService.resolve(userId, eval);

        DeckSnapshot baseline = eval.getBaselineDeckSnapshot();
        if (baseline == null) {
            return new EvaluationDeckDiffResponse(deckStatus, null, null, List.of(), null, null);
        }

        JsonNode baselineContent = parseRequestJson(baseline.getContentJson());
        var current = deckStatusService.buildCurrentBaselineContent(userId, eval);
        if (current == null) {
            return new EvaluationDeckDiffResponse(
                    deckStatus,
                    baseline.getId(),
                    null,
                    List.of(),
                    baselineContent,
                    null);
        }

        return new EvaluationDeckDiffResponse(
                deckStatus,
                baseline.getId(),
                null,
                diffService.diff(baseline.getContentJson(), current.canonicalJson()),
                baselineContent,
                parseRequestJson(current.canonicalJson()));
    }

    private DpsValueEvaluation loadOwned(Long userId, Long evaluationId) {
        return evaluationRepository.findByIdAndUserId(evaluationId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "평가 결과를 찾을 수 없습니다. id=" + evaluationId));
    }

    private DpsValueEvaluation loadOwnedWithSnapshots(Long userId, Long evaluationId) {
        return evaluationRepository.findByIdAndUserIdWithSnapshots(evaluationId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "평가 결과를 찾을 수 없습니다. id=" + evaluationId));
    }

    private JsonNode parseRequestJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            return null;
        }
    }
}
