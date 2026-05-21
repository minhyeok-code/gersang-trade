package org.example.gersangtrade.admin.dto.request;

import org.example.gersangtrade.domain.catalog.enums.EquipmentKind;
import org.example.gersangtrade.domain.catalog.enums.EquipmentSlot;
import org.example.gersangtrade.domain.deck.enums.EquipSlot;

public record EquipmentDetailUpdateRequest(
        EquipmentSlot slot,
        EquipmentKind equipmentKind,
        Long setId,               // null이면 세트 미소속
        boolean ritualApplicable,
        boolean hasSlotOption,
        EquipSlot equipSlot       // null이면 미설정 (반지 등)
) {}
