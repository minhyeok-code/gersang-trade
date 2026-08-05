package org.example.gersangtrade.guide.dto.response;

import org.example.gersangtrade.domain.catalog.EquipmentSet;
import org.example.gersangtrade.domain.catalog.Item;
import org.example.gersangtrade.domain.catalog.Mercenary;
import org.example.gersangtrade.domain.guide.UserGuideStep;
import org.example.gersangtrade.domain.guide.enums.GuideStepType;

import java.time.LocalDateTime;

/**
 * 유저 가이드 스텝 응답 — 진행도(checked)와 커스텀 여부를 포함한다.
 */
public record UserGuideStepResponse(
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
        String linkedMercenaryName,
        boolean custom,
        boolean checked,
        LocalDateTime checkedAt
) {
    public static UserGuideStepResponse of(UserGuideStep s) {
        Item item = s.getLinkedItem();
        EquipmentSet set = s.getLinkedSet();
        Mercenary merc = s.getLinkedMercenary();
        return new UserGuideStepResponse(
                s.getId(),
                s.getStepOrder(),
                s.getStepType(),
                s.getLabel(),
                s.getNote(),
                s.getIconUrl(),
                GuideStepResponse.resolveIcon(item, merc, s.getIconUrl()),
                item != null ? item.getId() : null,
                item != null ? item.getName() : null,
                set != null ? set.getId() : null,
                set != null ? set.getName() : null,
                set != null && set.isTradeable(),
                merc != null ? merc.getId() : null,
                merc != null ? merc.getName() : null,
                s.isCustom(),
                s.getCheckedAt() != null,
                s.getCheckedAt()
        );
    }
}
