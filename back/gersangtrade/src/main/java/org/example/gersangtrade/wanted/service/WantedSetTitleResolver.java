package org.example.gersangtrade.wanted.service;

import org.example.gersangtrade.domain.catalog.EquipmentItem;
import org.example.gersangtrade.domain.catalog.EquipmentSet;
import org.example.gersangtrade.domain.catalog.Ritual;
import org.example.gersangtrade.domain.listing.enums.RitualOutcome;
import org.example.gersangtrade.domain.wanted.WantedEquipmentCondition;
import org.example.gersangtrade.domain.wanted.WantedItem;
import org.example.gersangtrade.domain.wanted.WantedRitualCondition;
import org.example.gersangtrade.domain.wanted.enums.PreferredOutcome;
import org.example.gersangtrade.listing.service.SetTitleGenerator;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 구매 희망 등록글의 세트 피스·주술 조건으로 거래 표기 제목을 재계산한다.
 * 판매 등록의 {@link org.example.gersangtrade.listing.service.SetTitleResolver}와 동일 규칙을 적용한다.
 */
public final class WantedSetTitleResolver {

    private WantedSetTitleResolver() {}

    /**
     * @param items               sortOrder 순 정렬 전 아이템 목록
     * @param conditionByItemId   wantedItemId → 장비 조건
     * @param ritualsByItemId     wantedItemId → 주술 조건 목록
     * @param equipmentByItemId   itemId → 장비 상세 (equipmentSet fetch 포함)
     * @return 동일 세트 구성으로 판별 가능하면 표시 제목, 아니면 empty
     */
    public static Optional<String> resolve(
            List<WantedItem> items,
            Map<Long, WantedEquipmentCondition> conditionByItemId,
            Map<Long, List<WantedRitualCondition>> ritualsByItemId,
            Map<Long, EquipmentItem> equipmentByItemId) {

        return buildPieceInputs(items, conditionByItemId, ritualsByItemId, equipmentByItemId)
                .map(ctx -> SetTitleGenerator.generate(ctx.setName(), ctx.pieceInputs()));
    }

    /**
     * 구매 희망 피스·주술 조건으로 세트 구성 WatchInfo를 산출한다.
     * displayName·시세 매칭에 사용한다.
     */
    public static Optional<SetTitleGenerator.WatchInfo> resolveWatchInfo(
            List<WantedItem> items,
            Map<Long, WantedEquipmentCondition> conditionByItemId,
            Map<Long, List<WantedRitualCondition>> ritualsByItemId,
            Map<Long, EquipmentItem> equipmentByItemId) {

        return buildPieceInputs(items, conditionByItemId, ritualsByItemId, equipmentByItemId)
                .map(ctx -> SetTitleGenerator.resolveWatchInfo(ctx.pieceInputs()))
                .flatMap(info -> info != null ? Optional.of(info) : Optional.empty());
    }

    private record SetPieceContext(String setName, List<SetTitleGenerator.PieceTitleInput> pieceInputs) {}

    private static Optional<SetPieceContext> buildPieceInputs(
            List<WantedItem> items,
            Map<Long, WantedEquipmentCondition> conditionByItemId,
            Map<Long, List<WantedRitualCondition>> ritualsByItemId,
            Map<Long, EquipmentItem> equipmentByItemId) {

        if (items == null || items.isEmpty()) {
            return Optional.empty();
        }

        List<WantedItem> sorted = items.stream()
                .sorted(Comparator.comparingInt(WantedItem::getSortOrder))
                .toList();

        String setName = null;
        Long setId = null;
        List<SetTitleGenerator.PieceTitleInput> pieceInputs = new java.util.ArrayList<>();

        for (WantedItem wantedItem : sorted) {
            Long catalogItemId = wantedItem.getItem().getId();
            EquipmentItem equipment = equipmentByItemId.get(catalogItemId);
            if (equipment == null) {
                return Optional.empty();
            }

            EquipmentSet equipmentSet = equipment.getEquipmentSet();
            if (equipmentSet == null) {
                return Optional.empty();
            }

            if (setId == null) {
                setId = equipmentSet.getId();
                setName = equipmentSet.getName();
            } else if (!setId.equals(equipmentSet.getId())) {
                return Optional.empty();
            }

            String displayMark = resolveRitualMark(
                    wantedItem.getId(),
                    conditionByItemId,
                    ritualsByItemId);

            pieceInputs.add(new SetTitleGenerator.PieceTitleInput(equipment.getSlot(), displayMark));
        }

        if (setName == null || pieceInputs.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new SetPieceContext(setName, pieceInputs));
    }

    private static String resolveRitualMark(
            Long wantedItemId,
            Map<Long, WantedEquipmentCondition> conditionByItemId,
            Map<Long, List<WantedRitualCondition>> ritualsByItemId) {

        WantedEquipmentCondition condition = conditionByItemId.get(wantedItemId);
        if (condition == null || !condition.isHasRitual()) {
            return null;
        }

        List<WantedRitualCondition> rituals = ritualsByItemId.getOrDefault(wantedItemId, List.of());
        if (rituals.isEmpty()) {
            return null;
        }

        WantedRitualCondition first = rituals.get(0);
        Ritual ritual = first.getRitual();
        RitualOutcome outcome = toRitualOutcome(first.getPreferredOutcome());
        return SetTitleGenerator.buildTitleMark(ritual, outcome);
    }

    private static RitualOutcome toRitualOutcome(PreferredOutcome preferred) {
        if (preferred == PreferredOutcome.GREAT_SUCCESS) {
            return RitualOutcome.GREAT_SUCCESS;
        }
        return RitualOutcome.SUCCESS;
    }
}
