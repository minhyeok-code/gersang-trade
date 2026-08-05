package org.example.gersangtrade.guide.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.guide.dto.request.GuideImportRequest;
import org.example.gersangtrade.guide.dto.request.GuideStepAdminUpdateRequest;
import org.example.gersangtrade.guide.dto.response.GuideAdminSummaryResponse;
import org.example.gersangtrade.guide.dto.response.GuideImportReport;
import org.example.gersangtrade.guide.service.GuideAdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 가이드 원본 관리 API (관리자).
 *
 * <pre>
 * POST   /admin/guides/import              — 이미지 추출 JSON 일괄 주입 (published=false)
 * PATCH  /admin/guides/{guideId}/published — 공개 여부 토글 (검수 후 공개)
 * DELETE /admin/guides/{guideId}           — 가이드 삭제 (스텝 포함)
 * PATCH  /admin/guides/steps/{stepId}      — 원본 스텝 검수 수정 (내용·연결)
 * DELETE /admin/guides/steps/{stepId}      — 원본 스텝 삭제
 * </pre>
 */
@RestController
@RequestMapping("/admin/guides")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class GuideAdminController {

    private final GuideAdminService guideAdminService;

    /** 전체 가이드 목록 (비공개 포함) — 검수·관리용. */
    @GetMapping
    public ResponseEntity<List<GuideAdminSummaryResponse>> listAll() {
        return ResponseEntity.ok(guideAdminService.getAllGuides());
    }

    /** 이미지 추출 JSON 일괄 주입 → 매칭 리포트 반환. */
    @PostMapping("/import")
    public ResponseEntity<GuideImportReport> importGuides(
            @Valid @RequestBody GuideImportRequest request) {
        return ResponseEntity.ok(guideAdminService.importGuides(request));
    }

    /** 공개 여부 설정. */
    @PatchMapping("/{guideId}/published")
    public ResponseEntity<Void> setPublished(
            @PathVariable Long guideId,
            @RequestParam boolean published) {
        guideAdminService.setPublished(guideId, published);
        return ResponseEntity.noContent().build();
    }

    /** 가이드 삭제. */
    @DeleteMapping("/{guideId}")
    public ResponseEntity<Void> deleteGuide(@PathVariable Long guideId) {
        guideAdminService.deleteGuide(guideId);
        return ResponseEntity.noContent().build();
    }

    /** 원본 스텝 검수 수정. */
    @PatchMapping("/steps/{stepId}")
    public ResponseEntity<Void> updateStep(
            @PathVariable Long stepId,
            @Valid @RequestBody GuideStepAdminUpdateRequest request) {
        guideAdminService.updateStep(stepId, request);
        return ResponseEntity.noContent().build();
    }

    /** 원본 스텝 삭제. */
    @DeleteMapping("/steps/{stepId}")
    public ResponseEntity<Void> deleteStep(@PathVariable Long stepId) {
        guideAdminService.deleteStep(stepId);
        return ResponseEntity.noContent().build();
    }
}
