package org.example.gersangtrade.catalog.dto;

import org.example.gersangtrade.domain.catalog.EquipmentItem;
import org.example.gersangtrade.domain.catalog.ItemStat;
import org.example.gersangtrade.domain.catalog.enums.Element;
import org.example.gersangtrade.domain.catalog.enums.StatType;

import java.util.List;

/**
 * 덱 슬롯별 착용 가능 장비 목록 응답 DTO.
 * 슬롯 선택 시 해당 슬롯에 착용 가능한 아이템 목록과 스탯을 반환한다.
 */
public record EquipmentSlotItemResponse(
        Long itemId,
        String name,
        String equipmentKind,
        String imageUrl,
        boolean ritualApplicable,
        boolean hasSlotOption,
        Long setId,
        String setName,
        String slot,
        String equipSlot,
        List<StatEntry> stats
) {
    public record StatEntry(StatType statType, Element element, int value, String scope) {}

    public static EquipmentSlotItemResponse of(EquipmentItem item, List<ItemStat> stats) {
        return new EquipmentSlotItemResponse(
                item.getItemId(),
                item.getItem().getName(),
                item.getEquipmentKind().name(),
                item.getItem().getImageUrl(),
                item.isRitualApplicable(),
                item.isHasSlotOption(),
                item.getEquipmentSet() != null ? item.getEquipmentSet().getId() : null,
                item.getEquipmentSet() != null ? item.getEquipmentSet().getName() : null,
                item.getSlot().name(),
                item.getEquipSlot() != null ? item.getEquipSlot().name() : null,
                stats.stream()
                        .map(s -> new StatEntry(s.getStatType(), s.getElement(), s.getValue(), s.getScope().name()))
                        .toList()
        );
    }
}
