package org.example.gersangtrade.guide.dto.response;

import org.example.gersangtrade.domain.catalog.Mercenary;
import org.example.gersangtrade.domain.guide.UserGuide;

import java.time.LocalDateTime;

/**
 * 유저 가이드 사본 요약 응답 — 내 가이드 목록에 사용 (진행도 포함).
 */
public record UserGuideSummaryResponse(
        Long id,
        String title,
        Long sourceGuideId,
        String sourceVersion,
        Long targetMercenaryId,
        String targetMercenaryName,
        String targetMercenaryImageUrl,
        int totalSteps,
        int completedSteps,
        LocalDateTime createdAt
) {
    public static UserGuideSummaryResponse of(UserGuide g, int totalSteps, int completedSteps) {
        Mercenary merc = g.getTargetMercenary();
        return new UserGuideSummaryResponse(
                g.getId(),
                g.getTitle(),
                g.getSourceGuide() != null ? g.getSourceGuide().getId() : null,
                g.getSourceVersion(),
                merc != null ? merc.getId() : null,
                merc != null ? merc.getName() : null,
                merc != null ? merc.getImageUrl() : null,
                totalSteps,
                completedSteps,
                g.getCreatedAt()
        );
    }
}
