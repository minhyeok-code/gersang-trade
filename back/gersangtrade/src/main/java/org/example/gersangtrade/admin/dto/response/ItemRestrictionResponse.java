package org.example.gersangtrade.admin.dto.response;

import org.example.gersangtrade.domain.catalog.ItemMercenaryRestriction;
import org.example.gersangtrade.domain.catalog.enums.MercenaryCategory;

/**
 * 아이템 착용 제한 응답.
 *
 * @param id           제한 ID
 * @param mercenaryId  특정 용병 ID (null이면 category 기반 제한)
 * @param mercenaryName 용병 이름 (mercenaryId가 있을 때만)
 * @param category     카테고리 제한 (null이면 mercenaryId 기반 제한)
 */
public record ItemRestrictionResponse(
        Long id,
        Long mercenaryId,
        String mercenaryName,
        MercenaryCategory category
) {
    public static ItemRestrictionResponse of(ItemMercenaryRestriction r) {
        return new ItemRestrictionResponse(
                r.getId(),
                r.getMercenary() != null ? r.getMercenary().getId() : null,
                r.getMercenary() != null ? r.getMercenary().getName() : null,
                r.getCategory()
        );
    }
}
