package org.example.gersangtrade.admin.dto.set;

import org.example.gersangtrade.domain.catalog.EquipmentSet;

public record AdminSetResponse(
        Long id,
        String name,
        int totalPieces,
        boolean isTradeable
) {
    public static AdminSetResponse from(EquipmentSet s) {
        return new AdminSetResponse(s.getId(), s.getName(), s.getTotalPieces(), s.isTradeable());
    }
}
