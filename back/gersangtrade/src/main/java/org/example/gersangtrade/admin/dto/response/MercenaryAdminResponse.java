package org.example.gersangtrade.admin.dto.response;

import org.example.gersangtrade.domain.catalog.Mercenary;
import org.example.gersangtrade.domain.catalog.enums.MercenaryCategory;
import org.example.gersangtrade.domain.catalog.enums.Nature;

/**
 * 관리자 용병 목록 항목 응답.
 * 특성 개수를 포함해 관리자가 특성 입력 현황을 한눈에 파악할 수 있도록 한다.
 */
public record MercenaryAdminResponse(
        Long id,
        String name,
        MercenaryCategory category,
        Nature nature,
        Integer natureValue,
        int characteristicCount  // 현재 등록된 특성 수
) {
    public static MercenaryAdminResponse of(Mercenary m, int characteristicCount) {
        return new MercenaryAdminResponse(
                m.getId(),
                m.getName(),
                m.getCategory(),
                m.getNature(),
                m.getNatureValue(),
                characteristicCount
        );
    }
}
