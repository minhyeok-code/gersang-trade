package org.example.gersangtrade.guide.controller;

import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.guide.dto.response.GuideStepResponse;
import org.example.gersangtrade.guide.dto.response.GuideSummaryResponse;
import org.example.gersangtrade.guide.service.GuideService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 가이드 원본 조회 API.
 *
 * <pre>
 * GET /api/guides                  — 공개 가이드 목록 (비로그인 가능)
 * GET /api/guides/recommended      — 내 덱 기반 추천 가이드 (로그인)
 * GET /api/guides/{guideId}        — 가이드 요약
 * GET /api/guides/{guideId}/steps  — 가이드 원본 스텝 미리보기
 * </pre>
 */
@RestController
@RequestMapping("/api/guides")
@RequiredArgsConstructor
public class GuideController {

    private final GuideService guideService;

    /** 공개 가이드 목록. */
    @GetMapping
    public ResponseEntity<List<GuideSummaryResponse>> getGuides() {
        return ResponseEntity.ok(guideService.getPublishedGuides());
    }

    /** 내 활성 덱 기반 추천 가이드 (덱 없으면 빈 목록). */
    @GetMapping("/recommended")
    public ResponseEntity<List<GuideSummaryResponse>> getRecommended(
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(guideService.recommendGuidesForUser(userId));
    }

    /** 가이드 요약. */
    @GetMapping("/{guideId}")
    public ResponseEntity<GuideSummaryResponse> getGuide(@PathVariable Long guideId) {
        return ResponseEntity.ok(guideService.getPublishedGuide(guideId));
    }

    /** 가이드 원본 스텝 미리보기. */
    @GetMapping("/{guideId}/steps")
    public ResponseEntity<List<GuideStepResponse>> getGuideSteps(@PathVariable Long guideId) {
        return ResponseEntity.ok(guideService.getGuideSteps(guideId));
    }
}
