package org.example.gersangtrade.home.service;

import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.catalog.repository.EquipmentItemRepository;
import org.example.gersangtrade.domain.catalog.EquipmentItem;
import org.example.gersangtrade.domain.wanted.WantedEquipmentCondition;
import org.example.gersangtrade.domain.wanted.WantedItem;
import org.example.gersangtrade.domain.wanted.WantedRitualCondition;
import org.example.gersangtrade.wanted.repository.WantedEquipmentConditionRepository;
import org.example.gersangtrade.wanted.repository.WantedItemRepository;
import org.example.gersangtrade.wanted.repository.WantedRitualConditionRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 구매 희망 시세 2차 필터용 — WantedItem·조건·장비 정보 배치 로드 컨텍스트.
 */
@Component
@RequiredArgsConstructor
public class WantedBuyMatchContext {

    private final WantedItemRepository wantedItemRepository;
    private final WantedEquipmentConditionRepository wantedEquipmentConditionRepository;
    private final WantedRitualConditionRepository wantedRitualConditionRepository;
    private final EquipmentItemRepository equipmentItemRepository;

    public Loaded load(List<Long> wantedListingIds) {
        if (wantedListingIds == null || wantedListingIds.isEmpty()) {
            return Loaded.empty();
        }

        List<WantedItem> allItems = wantedItemRepository.findByWantedListingIdIn(wantedListingIds);
        Map<Long, List<WantedItem>> itemsByListingId = allItems.stream()
                .collect(Collectors.groupingBy(wi -> wi.getWantedListing().getId()));

        List<Long> wantedItemIds = allItems.stream().map(WantedItem::getId).toList();
        List<Long> catalogItemIds = allItems.stream()
                .map(wi -> wi.getItem().getId())
                .distinct()
                .toList();

        Map<Long, WantedEquipmentCondition> conditionByWantedItemId =
                wantedItemIds.isEmpty() ? Map.of()
                        : wantedEquipmentConditionRepository.findByWantedItemIdIn(wantedItemIds).stream()
                        .collect(Collectors.toMap(c -> c.getWantedItem().getId(), Function.identity()));

        Map<Long, List<WantedRitualCondition>> ritualsByWantedItemId =
                wantedItemIds.isEmpty() ? Map.of()
                        : wantedRitualConditionRepository.findWithRitualByWantedItemIdIn(wantedItemIds).stream()
                        .collect(Collectors.groupingBy(r -> r.getWantedItem().getId()));

        Map<Long, EquipmentItem> equipmentByCatalogItemId =
                catalogItemIds.isEmpty() ? Map.of()
                        : equipmentItemRepository.findWithItemAndSetByItemIdIn(catalogItemIds).stream()
                        .collect(Collectors.toMap(EquipmentItem::getItemId, Function.identity()));

        return new Loaded(
                itemsByListingId,
                conditionByWantedItemId,
                ritualsByWantedItemId,
                equipmentByCatalogItemId
        );
    }

    public record Loaded(
            Map<Long, List<WantedItem>> itemsByListingId,
            Map<Long, WantedEquipmentCondition> conditionByWantedItemId,
            Map<Long, List<WantedRitualCondition>> ritualsByWantedItemId,
            Map<Long, EquipmentItem> equipmentByCatalogItemId
    ) {
        static Loaded empty() {
            return new Loaded(Map.of(), Map.of(), Map.of(), Map.of());
        }

        List<WantedItem> itemsFor(Long wantedListingId) {
            return itemsByListingId.getOrDefault(wantedListingId, List.of());
        }
    }
}
