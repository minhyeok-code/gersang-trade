package org.example.gersangtrade.admin.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.example.gersangtrade.domain.catalog.enums.BuffTarget;
import org.example.gersangtrade.domain.catalog.enums.Element;
import org.example.gersangtrade.domain.catalog.enums.EquipmentKind;
import org.example.gersangtrade.domain.catalog.enums.EquipmentSlot;
import org.example.gersangtrade.domain.catalog.enums.ItemType;
import org.example.gersangtrade.domain.catalog.enums.StatType;

import java.util.List;

/** 아이템 신규 등록 요청 */
public record ItemCreateRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull ItemType type,
        String tradeCategory,
        // EQUIPMENT 타입일 때 필수
        EquipmentSlot slot,
        EquipmentKind equipmentKind,
        Long equipmentSetId,
        boolean ritualApplicable,
        boolean hasSlotOption,
        @Valid List<StatEntry> stats
) {
    public record StatEntry(
            @NotNull StatType statType,
            Element element,     // null이면 NONE으로 처리
            @NotNull Integer value,
            BuffTarget scope     // null이면 SELF로 처리
    ) {}
}
