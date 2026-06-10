package org.example.gersangtrade.admin.dto.response;

import org.example.gersangtrade.domain.catalog.Mercenary;
import org.example.gersangtrade.domain.catalog.enums.MercenaryCategory;
import org.example.gersangtrade.domain.catalog.enums.Nature;

/**
 * 이미지 관리용 용병 응답.
 * 검색·누락 목록 양쪽에서 사용된다.
 */
public record MercenaryImageTargetResponse(
        Long id,
        String name,
        MercenaryCategory category,
        Nature nature,
        String imageUrl
) {
    public static MercenaryImageTargetResponse from(Mercenary m) {
        return new MercenaryImageTargetResponse(
                m.getId(),
                m.getName(),
                m.getCategory(),
                m.getNature(),
                m.getImageUrl()
        );
    }
}
