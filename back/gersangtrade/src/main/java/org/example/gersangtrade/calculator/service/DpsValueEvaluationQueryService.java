package org.example.gersangtrade.calculator.service;

import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.calculator.dto.response.DpsEvaluationSummary;
import org.example.gersangtrade.calculator.dto.response.DpsValueEvaluationResponse;
import org.example.gersangtrade.calculator.repository.DpsValueEvaluationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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

    /**
     * 내 평가 목록 — 최신순, 페이징.
     * 본인 데이터만 반환한다.
     */
    public Page<DpsEvaluationSummary> getMyEvaluations(Long userId, Pageable pageable) {
        return evaluationRepository.findByUserIdWithMonster(userId, pageable)
                .map(e -> DpsEvaluationSummary.from(
                        e,
                        candidateLabelResolver.resolve(e.getCandidateType(), e.getCandidateRef())));
    }

    /**
     * 내 평가 기록 삭제 — 본인 소유만 허용.
     */
    @Transactional
    public void deleteMyEvaluation(Long userId, Long evaluationId) {
        var eval = evaluationRepository.findByIdAndUserId(evaluationId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "평가 결과를 찾을 수 없습니다. id=" + evaluationId));
        evaluationRepository.delete(eval);
    }

    /**
     * 내 평가 상세 — DB 저장 값 복원.
     * 타인 평가 접근 시 404로 처리한다(존재 여부 노출 방지).
     */
    public DpsValueEvaluationResponse getMyEvaluation(Long userId, Long evaluationId) {
        var eval = evaluationRepository.findByIdAndUserId(evaluationId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "평가 결과를 찾을 수 없습니다. id=" + evaluationId));
        return DpsValueEvaluationResponse.ofStored(eval);
    }
}
