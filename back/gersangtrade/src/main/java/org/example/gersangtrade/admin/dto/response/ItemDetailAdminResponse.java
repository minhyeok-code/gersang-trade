package org.example.gersangtrade.admin.dto.response;

import org.example.gersangtrade.domain.catalog.EquipmentItem;
import org.example.gersangtrade.domain.catalog.Item;
import org.example.gersangtrade.domain.catalog.ItemSkill;
import org.example.gersangtrade.domain.catalog.ItemStat;
import org.example.gersangtrade.domain.catalog.enums.Element;
import org.example.gersangtrade.domain.catalog.enums.EquipmentKind;
import org.example.gersangtrade.domain.catalog.enums.EquipmentSlot;
import org.example.gersangtrade.domain.catalog.enums.ItemType;
import org.example.gersangtrade.domain.catalog.enums.StatType;

import java.util.List;

/** 관리자 아이템 상세 응답 (기본정보 + 스탯 + 스킬 + 장비 정보) */
public record ItemDetailAdminResponse(
        Long id,
        String name,
        ItemType type,
        String tradeCategory,
        String imageUrl,
        EquipmentInfo equipment,   // type=EQUIPMENT 일 때만 non-null
        List<StatEntry> stats,
        List<String> skills
) {
    public record StatEntry(Long id, StatType statType, Element element, Integer value) {}

    public record EquipmentInfo(
            EquipmentKind equipmentKind,
            EquipmentSlot slot,
            boolean ritualApplicable,
            boolean hasSlotOption
    ) {}

    public static ItemDetailAdminResponse of(Item item,
                                              EquipmentItem equipmentItem,
                                              List<ItemStat> stats,
                                              List<ItemSkill> skills) {
        EquipmentInfo equipmentInfo = (equipmentItem != null)
                ? new EquipmentInfo(
                        equipmentItem.getEquipmentKind(),
                        equipmentItem.getSlot(),
                        equipmentItem.isRitualApplicable(),
                        equipmentItem.isHasSlotOption())
                : null;

        return new ItemDetailAdminResponse(
                item.getId(),
                item.getName(),
                item.getType(),
                item.getTradeCategory(),
                item.getImageUrl(),
                equipmentInfo,
                stats.stream()
                        .map(s -> new StatEntry(s.getId(), s.getStatType(), s.getElement(), s.getValue()))
                        .toList(),
                skills.stream().map(ItemSkill::getSkillName).toList()
        );
    }
}
