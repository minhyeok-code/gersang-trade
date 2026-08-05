package org.example.gersangtrade.admin.dto.set;

import jakarta.validation.constraints.NotNull;
import org.example.gersangtrade.domain.catalog.enums.EquipmentSlot;

/** 세트 피스 추가 요청 — equipmentItemId는 Item.id(= EquipmentItem.itemId). */
public record SetPieceAddRequest(
        @NotNull EquipmentSlot slot,
        @NotNull Long equipmentItemId,
        Integer pieceCount
) {}
