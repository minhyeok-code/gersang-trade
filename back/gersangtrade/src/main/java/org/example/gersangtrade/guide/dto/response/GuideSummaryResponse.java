package org.example.gersangtrade.guide.dto.response;

import org.example.gersangtrade.domain.catalog.Mercenary;
import org.example.gersangtrade.domain.guide.Guide;
import org.example.gersangtrade.domain.guide.enums.GuidePhase;

/**
 * 가이드 원본 요약 응답 — 목록·추천에 사용.
 */
public record GuideSummaryResponse(
        Long id,
        String title,
        GuidePhase phase,
        String version,
        String author,
        Long targetMercenaryId,
        String targetMercenaryName,
        String targetMercenaryImageUrl
) {
    public static GuideSummaryResponse of(Guide guide) {
        Mercenary merc = guide.getTargetMercenary();
        return new GuideSummaryResponse(
                guide.getId(),
                guide.getTitle(),
                guide.getPhase(),
                guide.getVersion(),
                guide.getAuthor(),
                merc != null ? merc.getId() : null,
                merc != null ? merc.getName() : null,
                merc != null ? merc.getImageUrl() : null
        );
    }
}
