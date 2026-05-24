package org.example.gersangtrade.admin.dto.request;

import org.example.gersangtrade.domain.catalog.enums.MercenaryCategory;

/**
 * 아이템 착용 제한 추가 요청.
 * mercenaryId와 category 중 하나만 설정해야 한다.
 *
 * @param mercenaryId 특정 용병 ID (null이면 category로 설정)
 * @param category    카테고리 전체 제한 (null이면 mercenaryId로 설정)
 */
public record ItemRestrictionAddRequest(
        Long mercenaryId,
        MercenaryCategory category
) {}
