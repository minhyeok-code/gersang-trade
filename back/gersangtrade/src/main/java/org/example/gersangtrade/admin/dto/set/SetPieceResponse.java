package org.example.gersangtrade.admin.dto.set;

import org.example.gersangtrade.domain.catalog.EquipmentSetPiece;
import org.example.gersangtrade.domain.catalog.enums.EquipmentSlot;

/** 세트 피스 응답 — 슬롯·아이템·착용 개수. */
public record SetPieceResponse(
        Long id,
        EquipmentSlot slot,
        Long equipmentItemId,
        String itemName,
        int pieceCount
) {
    public static SetPieceResponse from(EquipmentSetPiece p) {
        return new SetPieceResponse(
                p.getId(),
                p.getSlot(),
                p.getEquipmentItem().getItemId(),
                p.getEquipmentItem().getItem().getName(),
                p.getPieceCount()
        );
    }
}
