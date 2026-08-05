package org.example.gersangtrade.guide.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.guide.dto.request.GuideStepAddRequest;
import org.example.gersangtrade.guide.dto.request.GuideStepUpdateRequest;
import org.example.gersangtrade.guide.dto.request.UserGuideRenameRequest;
import org.example.gersangtrade.guide.dto.request.UserGuideStepReorderRequest;
import org.example.gersangtrade.guide.dto.response.UserGuideDetailResponse;
import org.example.gersangtrade.guide.dto.response.UserGuideStepResponse;
import org.example.gersangtrade.guide.dto.response.UserGuideSummaryResponse;
import org.example.gersangtrade.guide.service.UserGuideService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 유저 가이드 사본 API (로그인 필요).
 *
 * <pre>
 * POST   /api/user-guides?guideId=       — 원본 복제해 내 사본 시작 (멱등)
 * GET    /api/user-guides                — 내 사본 목록 (진행도 포함)
 * GET    /api/user-guides/{id}           — 내 사본 상세 (스텝 포함)
 * PATCH  /api/user-guides/{id}/title     — 사본 이름 변경
 * DELETE /api/user-guides/{id}           — 사본 소프트삭제
 * POST   /api/user-guides/{id}/steps     — 커스텀 스텝 추가
 * PATCH  /api/user-guides/{id}/steps/order — 스텝 순서 재정렬
 * PATCH  /api/user-guides/steps/{stepId} — 스텝 내용 수정
 * POST   /api/user-guides/steps/{stepId}/check   — 완료 체크
 * DELETE /api/user-guides/steps/{stepId}/check   — 완료 해제
 * DELETE /api/user-guides/steps/{stepId}         — 스텝 삭제
 * </pre>
 */
@RestController
@RequestMapping("/api/user-guides")
@RequiredArgsConstructor
public class UserGuideController {

    private final UserGuideService userGuideService;

    /** 원본을 복제해 내 사본을 시작한다 (이미 있으면 그 사본 반환). */
    @PostMapping
    public ResponseEntity<UserGuideDetailResponse> start(
            @AuthenticationPrincipal Long userId,
            @RequestParam Long guideId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userGuideService.startGuide(userId, guideId));
    }

    /** 내 사본 목록. */
    @GetMapping
    public ResponseEntity<List<UserGuideSummaryResponse>> getMyGuides(
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(userGuideService.getMyGuides(userId));
    }

    /** 내 사본 상세. */
    @GetMapping("/{userGuideId}")
    public ResponseEntity<UserGuideDetailResponse> getMyGuide(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long userGuideId) {
        return ResponseEntity.ok(userGuideService.getMyGuideDetail(userId, userGuideId));
    }

    /** 사본 이름 변경. */
    @PatchMapping("/{userGuideId}/title")
    public ResponseEntity<Void> rename(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long userGuideId,
            @Valid @RequestBody UserGuideRenameRequest req) {
        userGuideService.renameGuide(userId, userGuideId, req.title());
        return ResponseEntity.noContent().build();
    }

    /** 사본 삭제 (소프트). */
    @DeleteMapping("/{userGuideId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long userGuideId) {
        userGuideService.deleteGuide(userId, userGuideId);
        return ResponseEntity.noContent().build();
    }

    /** 커스텀 스텝 추가. */
    @PostMapping("/{userGuideId}/steps")
    public ResponseEntity<UserGuideStepResponse> addStep(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long userGuideId,
            @Valid @RequestBody GuideStepAddRequest req) {
        UserGuideStepResponse created = userGuideService.addStep(
                userId, userGuideId,
                req.stepType(), req.label(), req.note(), req.iconUrl(),
                req.linkedItemId(), req.linkedSetId(), req.linkedMercenaryId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /** 스텝 순서 재정렬. */
    @PatchMapping("/{userGuideId}/steps/order")
    public ResponseEntity<Void> reorder(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long userGuideId,
            @Valid @RequestBody UserGuideStepReorderRequest req) {
        userGuideService.reorderSteps(userId, userGuideId, req.orderedStepIds());
        return ResponseEntity.noContent().build();
    }

    /** 스텝 내용 수정. */
    @PatchMapping("/steps/{stepId}")
    public ResponseEntity<Void> updateStep(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long stepId,
            @Valid @RequestBody GuideStepUpdateRequest req) {
        userGuideService.updateStepContent(userId, stepId, req.label(), req.note(), req.iconUrl());
        return ResponseEntity.noContent().build();
    }

    /** 스텝 완료 체크. */
    @PostMapping("/steps/{stepId}/check")
    public ResponseEntity<Void> checkStep(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long stepId) {
        userGuideService.checkStep(userId, stepId);
        return ResponseEntity.noContent().build();
    }

    /** 스텝 완료 해제. */
    @DeleteMapping("/steps/{stepId}/check")
    public ResponseEntity<Void> uncheckStep(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long stepId) {
        userGuideService.uncheckStep(userId, stepId);
        return ResponseEntity.noContent().build();
    }

    /** 스텝 삭제 (하드). */
    @DeleteMapping("/steps/{stepId}")
    public ResponseEntity<Void> removeStep(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long stepId) {
        userGuideService.removeStep(userId, stepId);
        return ResponseEntity.noContent().build();
    }
}
