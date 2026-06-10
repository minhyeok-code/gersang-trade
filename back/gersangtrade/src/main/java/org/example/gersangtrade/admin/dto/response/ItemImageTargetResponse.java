package org.example.gersangtrade.admin.dto.response;

import org.example.gersangtrade.domain.catalog.Item;
import org.example.gersangtrade.domain.catalog.enums.ItemType;

/**
 * 이미지 관리용 아이템 응답.
 * 검색·누락 목록 양쪽에서 사용된다.
 */
public record ItemImageTargetResponse(
        Long id,
        String name,
        ItemType type,
        String imageUrl
) {
    public static ItemImageTargetResponse from(Item item) {
        return new ItemImageTargetResponse(
                item.getId(),
                item.getName(),
                item.getType(),
                item.getImageUrl()
        );
    }
}
