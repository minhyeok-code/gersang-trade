package org.example.gersangtrade.catalog.dto.response;

import org.example.gersangtrade.domain.catalog.EquipmentSet;

/** 유저용 장비 세트 응답 DTO */
public record SetResponse(
        Long id,
        String name,
        int totalPieces
) {
    public static SetResponse from(EquipmentSet s) {
        return new SetResponse(s.getId(), s.getName(), s.getTotalPieces());
    }
}
