package org.example.gersangtrade.catalog.dto.response;

import org.example.gersangtrade.domain.catalog.EquipmentSet;
import org.example.gersangtrade.domain.catalog.EquipmentSetPiece;

import java.util.List;

/** 세트 단건 상세 응답 DTO — 피스 목록 포함 */
public record SetDetailResponse(
        Long id,
        String name,
        int totalPieces,
        List<PieceEntry> pieces
) {
    public record PieceEntry(
            Long itemId,
            String itemName,
            String imageUrl,
            String slot,
            String equipSlot,
            boolean ritualApplicable,
            int pieceCount
    ) {}

    public static SetDetailResponse of(EquipmentSet set, List<EquipmentSetPiece> pieces) {
        return new SetDetailResponse(
                set.getId(),
                set.getName(),
                set.getTotalPieces(),
                pieces.stream()
                        .map(p -> new PieceEntry(
                                p.getEquipmentItem().getItemId(),
                                p.getEquipmentItem().getItem().getName(),
                                p.getEquipmentItem().getItem().getImageUrl(),
                                p.getSlot().name(),
                                p.getEquipmentItem().getEquipSlot() != null
                                        ? p.getEquipmentItem().getEquipSlot().name() : null,
                                p.getEquipmentItem().isRitualApplicable(),
                                p.getPieceCount()
                        ))
                        .toList()
        );
    }
}
