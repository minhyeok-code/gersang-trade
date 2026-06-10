package org.example.gersangtrade.wanted.service;

import org.example.gersangtrade.domain.catalog.Ritual;
import org.example.gersangtrade.domain.listing.enums.RitualOutcome;
import org.example.gersangtrade.domain.wanted.WantedEquipmentCondition;
import org.example.gersangtrade.domain.wanted.WantedItem;
import org.example.gersangtrade.domain.wanted.WantedRitualCondition;
import org.example.gersangtrade.domain.wanted.enums.PreferredOutcome;
import org.example.gersangtrade.listing.service.SetTitleGenerator;

import java.util.List;
import java.util.Optional;

/**
 * 구매 희망 단품 장비의 표시 제목을 재계산한다.
 * 주술이 있으면 {@code <마크>아이템명} 형식으로 반환한다.
 */
public final class WantedSingleItemTitleResolver {

    private WantedSingleItemTitleResolver() {}

    public static Optional<String> resolve(
            WantedItem item,
            WantedEquipmentCondition condition,
            List<WantedRitualCondition> rituals) {

        if (item == null || item.getItem() == null) {
            return Optional.empty();
        }

        String name = item.getItem().getName();
        if (condition == null || !condition.isHasRitual() || rituals == null || rituals.isEmpty()) {
            return Optional.of(name);
        }

        WantedRitualCondition first = rituals.get(0);
        Ritual ritual = first.getRitual();
        RitualOutcome outcome = first.getPreferredOutcome() == PreferredOutcome.GREAT_SUCCESS
                ? RitualOutcome.GREAT_SUCCESS
                : RitualOutcome.SUCCESS;
        String mark = SetTitleGenerator.buildTitleMark(ritual, outcome);
        return Optional.of(mark + name);
    }
}
