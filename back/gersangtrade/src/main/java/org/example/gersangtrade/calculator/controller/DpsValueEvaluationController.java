package org.example.gersangtrade.calculator.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.calculator.dto.request.DpsEvaluationRequest;
import org.example.gersangtrade.calculator.dto.response.DpsEvaluationSummary;
import org.example.gersangtrade.calculator.dto.response.DpsValueEvaluationResponse;
import org.example.gersangtrade.calculator.service.DpsValueEvaluationQueryService;
import org.example.gersangtrade.calculator.service.DpsValueEvaluationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * DPS 가성비 평가 API 컨트롤러.
 *
 * <pre>
 * POST /api/calculator/dps/evaluations       — 평가 실행 (로그인 필수)
 * GET  /api/calculator/dps/evaluations       — 내 평가 목록 (최신순, 페이징)
 * GET  /api/calculator/dps/evaluations/{id}  — 내 평가 상세
 * </pre>
 *
 * 모든 엔드포인트는 로그인 필수이며 본인 데이터만 접근 가능하다.
 */
@RestController
@RequestMapping("/api/calculator/dps/evaluations")
@RequiredArgsConstructor
public class DpsValueEvaluationController {

    private final DpsValueEvaluationService evaluationService;
    private final DpsValueEvaluationQueryService queryService;

    /**
     * DPS 가성비 평가 실행.
     * 결과를 DB에 저장하고 evaluationId를 반환한다.
     * 동일 요청의 재제출은 멱등 처리된다(기존 결과 반환).
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DpsValueEvaluationResponse> evaluate(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid DpsEvaluationRequest request) {
        return ResponseEntity.ok(evaluationService.evaluate(userId, request));
    }

    /**
     * 내 평가 목록 조회 — 최신순, 페이징.
     *
     * @param page 페이지 번호 (0-based, 기본 0)
     * @param size 페이지 크기 (기본 20, 최대 100)
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<DpsEvaluationSummary>> getMyEvaluations(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int clampedSize = Math.min(size, 100);
        PageRequest pageable = PageRequest.of(page, clampedSize, Sort.by("createdAt").descending());
        return ResponseEntity.ok(queryService.getMyEvaluations(userId, pageable));
    }

    /**
     * 내 평가 상세 조회 — DB 저장 값 복원.
     * 타인 평가는 404로 처리한다(존재 여부 비노출).
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DpsValueEvaluationResponse> getMyEvaluation(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id) {
        return ResponseEntity.ok(queryService.getMyEvaluation(userId, id));
    }

    /** 내 평가 기록 삭제 */
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteMyEvaluation(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id) {
        queryService.deleteMyEvaluation(userId, id);
        return ResponseEntity.noContent().build();
    }
}
