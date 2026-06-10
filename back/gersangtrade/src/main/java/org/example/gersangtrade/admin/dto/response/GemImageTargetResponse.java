package org.example.gersangtrade.admin.dto.response;

import org.example.gersangtrade.domain.catalog.Gem;
import org.example.gersangtrade.domain.catalog.enums.GemGrade;

/**
 * 이미지 관리용 보석 응답.
 * 검색·누락 목록 양쪽에서 사용된다.
 */
public record GemImageTargetResponse(
        Long id,
        String name,
        GemGrade gemGrade,
        Long ritualId,
        String imageUrl
) {
    public static GemImageTargetResponse from(Gem gem) {
        return new GemImageTargetResponse(
                gem.getId(),
                gem.getName(),
                gem.getGemGrade(),
                gem.getRitual() != null ? gem.getRitual().getId() : null,
                gem.getImageUrl()
        );
    }
}
