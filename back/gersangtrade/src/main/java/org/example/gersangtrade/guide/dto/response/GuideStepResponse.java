package org.example.gersangtrade.guide.dto.response;

import org.example.gersangtrade.domain.catalog.EquipmentSet;
import org.example.gersangtrade.domain.catalog.Item;
import org.example.gersangtrade.domain.catalog.Mercenary;
import org.example.gersangtrade.domain.guide.GuideStep;
import org.example.gersangtrade.domain.guide.enums.GuideStepType;

/**
 * 가이드 원본 스텝 응답 — 시작 전 미리보기에 사용.
 * displayIconUrl은 연결 아이템·용병 이미지를 우선하고, 없으면 스텝 iconUrl로 fallback한다.
 * (세트는 카탈로그 이미지가 없어 iconUrl을 쓴다.)
 */
public record GuideStepResponse(
        Long id,
        int stepOrder,
        GuideStepType stepType,
        String label,
        String note,
        String iconUrl,
        String displayIconUrl,
        Long linkedItemId,
        String linkedItemName,
        Long linkedSetId,
        String linkedSetName,
        boolean linkedSetTradeable,
        Long linkedMercenaryId,
        String linkedMercenaryName
) {
    public static GuideStepResponse of(GuideStep s) {
        Item item = s.getLinkedItem();
        EquipmentSet set = s.getLinkedSet();
        Mercenary merc = s.getLinkedMercenary();
        return new GuideStepResponse(
                s.getId(),
                s.getStepOrder(),
                s.getStepType(),
                s.getLabel(),
                s.getNote(),
                s.getIconUrl(),
                resolveIcon(item, merc, s.getIconUrl()),
                item != null ? item.getId() : null,
                item != null ? item.getName() : null,
                set != null ? set.getId() : null,
                set != null ? set.getName() : null,
                set != null && set.isTradeable(),
                merc != null ? merc.getId() : null,
                merc != null ? merc.getName() : null
        );
    }

    static String resolveIcon(Item item, Mercenary merc, String iconUrl) {
        if (item != null && item.getImageUrl() != null) return item.getImageUrl();
        if (merc != null && merc.getImageUrl() != null) return merc.getImageUrl();
        return iconUrl;
    }
}
