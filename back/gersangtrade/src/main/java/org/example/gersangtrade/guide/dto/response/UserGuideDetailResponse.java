package org.example.gersangtrade.guide.dto.response;

import org.example.gersangtrade.domain.catalog.Mercenary;
import org.example.gersangtrade.domain.guide.UserGuide;
import org.example.gersangtrade.domain.guide.UserGuideStep;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 유저 가이드 사본 상세 응답 — 스텝 목록 포함.
 */
public record UserGuideDetailResponse(
        Long id,
        String title,
        Long sourceGuideId,
        String sourceVersion,
        Long targetMercenaryId,
        String targetMercenaryName,
        String targetMercenaryImageUrl,
        LocalDateTime createdAt,
        List<UserGuideStepResponse> steps
) {
    public static UserGuideDetailResponse of(UserGuide g, List<UserGuideStep> steps) {
        Mercenary merc = g.getTargetMercenary();
        return new UserGuideDetailResponse(
                g.getId(),
                g.getTitle(),
                g.getSourceGuide() != null ? g.getSourceGuide().getId() : null,
                g.getSourceVersion(),
                merc != null ? merc.getId() : null,
                merc != null ? merc.getName() : null,
                merc != null ? merc.getImageUrl() : null,
                g.getCreatedAt(),
                steps.stream().map(UserGuideStepResponse::of).toList()
        );
    }
}
