package org.example.gersangtrade.watchlist.service;

import org.example.gersangtrade.domain.catalog.EquipmentItem;
import org.example.gersangtrade.domain.catalog.Ritual;
import org.example.gersangtrade.domain.listing.BundleEquipmentDetail;
import org.example.gersangtrade.domain.listing.BundleEquipmentRitual;
import org.example.gersangtrade.domain.listing.BundleLine;
import org.example.gersangtrade.domain.listing.enums.RitualOutcome;
import org.example.gersangtrade.domain.wanted.WantedEquipmentCondition;
import org.example.gersangtrade.domain.wanted.WantedItem;
import org.example.gersangtrade.domain.wanted.WantedRitualCondition;
import org.example.gersangtrade.domain.wanted.enums.PreferredOutcome;
import org.example.gersangtrade.listing.service.SetTitleGenerator;
import org.example.gersangtrade.wanted.service.WantedSetTitleResolver;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * ITEM 관심 항목과 판매·구매 등록글의 일치 여부를 판단하는 유틸리티.
 * 세트 구성으로 해석되는 삽니다(displayName 기준)는 단품 관심에서 제외한다.
 */
public final class ItemWatchMatcher {

    private ItemWatchMatcher() {}

    /**
     * 단품·재료 관심 항목과 판매 등록글 일치 여부.
     *
     * @param watchItemId     관심 catalog itemId
     * @param watchRitualMark 관심 주술 마크 (null이면 주술 없는 단품만)
     */
    public static boolean matchesSell(
            Long watchItemId,
            String watchRitualMark,
            List<BundleLine> lines,
            Map<Long, BundleEquipmentDetail> detailByLineId,
            Map<Long, List<BundleEquipmentRitual>> ritualsByLineId) {

        if (watchItemId == null || lines == null || lines.isEmpty()) {
            return false;
        }

        for (BundleLine line : lines) {
            if (!watchItemId.equals(line.getItem().getId())) {
                continue;
            }
            BundleEquipmentDetail detail = detailByLineId.get(line.getId());
            if (matchesEquipmentRitual(
                    watchRitualMark,
                    detail != null && detail.isHasRitual(),
                    ritualsByLineId.getOrDefault(line.getId(), List.of()))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 단품·재료 관심 항목과 구매 희망 등록글 일치 여부.
     *
     * @param watchItemId   관심 catalog itemId
     * @param watchRitualMark 관심 주술 마크 (null이면 주술 없는 단품만)
     */
    public static boolean matchesBuy(
            Long watchItemId,
            String watchRitualMark,
            List<WantedItem> items,
            Map<Long, WantedEquipmentCondition> conditionByWantedItemId,
            Map<Long, List<WantedRitualCondition>> ritualsByWantedItemId,
            Map<Long, EquipmentItem> equipmentByCatalogItemId) {

        if (watchItemId == null || items == null || items.isEmpty()) {
            return false;
        }

        // 세트 표기(displayName)로 해석되면 SET 관심 대상 — 단품 관심에서 제외
        if (WantedSetTitleResolver.resolveWatchInfo(
                items, conditionByWantedItemId, ritualsByWantedItemId, equipmentByCatalogItemId
        ).isPresent()) {
            return false;
        }

        // 단품·재료 삽니다는 라인 1개만 허용
        if (items.size() != 1) {
            return false;
        }

        WantedItem only = items.get(0);
        if (!watchItemId.equals(only.getItem().getId())) {
            return false;
        }

        WantedEquipmentCondition condition = conditionByWantedItemId.get(only.getId());
        boolean hasRitual = condition != null && condition.isHasRitual();
        List<WantedRitualCondition> rituals = ritualsByWantedItemId.getOrDefault(only.getId(), List.of());
        return matchesBuyRitual(watchRitualMark, hasRitual, rituals);
    }

    private static boolean matchesBuyRitual(
            String watchRitualMark,
            boolean hasRitual,
            List<WantedRitualCondition> rituals) {

        if (watchRitualMark == null || watchRitualMark.isBlank()) {
            return !hasRitual || rituals.isEmpty();
        }
        if (!hasRitual || rituals.isEmpty()) {
            return false;
        }
        WantedRitualCondition first = rituals.get(0);
        Ritual ritual = first.getRitual();
        RitualOutcome outcome = first.getPreferredOutcome() == PreferredOutcome.GREAT_SUCCESS
                ? RitualOutcome.GREAT_SUCCESS
                : RitualOutcome.SUCCESS;
        return Objects.equals(watchRitualMark, SetTitleGenerator.buildTitleMark(ritual, outcome));
    }

    private static boolean matchesEquipmentRitual(
            String watchRitualMark,
            boolean hasRitual,
            List<BundleEquipmentRitual> rituals) {

        if (watchRitualMark == null || watchRitualMark.isBlank()) {
            return !hasRitual || rituals.isEmpty();
        }
        if (!hasRitual || rituals.isEmpty()) {
            return false;
        }
        BundleEquipmentRitual first = rituals.get(0);
        return Objects.equals(
                watchRitualMark,
                SetTitleGenerator.buildTitleMark(first.getRitual(), first.getOutcome()));
    }
}
