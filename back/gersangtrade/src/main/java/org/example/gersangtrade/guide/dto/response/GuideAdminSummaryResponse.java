package org.example.gersangtrade.guide.dto.response;

import org.example.gersangtrade.domain.catalog.Mercenary;
import org.example.gersangtrade.domain.guide.Guide;
import org.example.gersangtrade.domain.guide.enums.GuidePhase;

/**
 * 관리자용 가이드 요약 — 비공개 포함 전체 목록·검수에 사용 (published·스텝 수 포함).
 */
public record GuideAdminSummaryResponse(
        Long id,
        String title,
        GuidePhase phase,
        String version,
        String author,
        boolean published,
        Long targetMercenaryId,
        String targetMercenaryName,
        int stepCount
) {
    public static GuideAdminSummaryResponse of(Guide g, int stepCount) {
        Mercenary merc = g.getTargetMercenary();
        return new GuideAdminSummaryResponse(
                g.getId(),
                g.getTitle(),
                g.getPhase(),
                g.getVersion(),
                g.getAuthor(),
                g.isPublished(),
                merc != null ? merc.getId() : null,
                merc != null ? merc.getName() : null,
                stepCount
        );
    }
}
