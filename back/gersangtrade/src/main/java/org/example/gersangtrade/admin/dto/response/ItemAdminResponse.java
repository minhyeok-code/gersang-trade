package org.example.gersangtrade.admin.dto.response;

import org.example.gersangtrade.domain.catalog.Item;
import org.example.gersangtrade.domain.catalog.enums.ItemType;

/** 관리자 아이템 목록 항목 응답. statCount로 스탯 미입력 아이템을 파악한다. */
public record ItemAdminResponse(
        Long id,
        String name,
        ItemType type,
        String tradeCategory,
        String imageUrl,
        int statCount
) {
    public static ItemAdminResponse of(Item item, int statCount) {
        return new ItemAdminResponse(
                item.getId(),
                item.getName(),
                item.getType(),
                item.getTradeCategory(),
                item.getImageUrl(),
                statCount
        );
    }
}
