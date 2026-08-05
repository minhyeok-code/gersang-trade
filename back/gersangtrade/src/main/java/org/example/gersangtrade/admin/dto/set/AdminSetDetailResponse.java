package org.example.gersangtrade.admin.dto.set;

import org.example.gersangtrade.domain.catalog.EquipmentSet;

import java.util.List;

/** 세트 상세 응답 — 기본 정보 + 피스 목록 + 효과 목록. */
public record AdminSetDetailResponse(
        Long id,
        String name,
        int totalPieces,
        boolean isTradeable,
        List<SetPieceResponse> pieces,
        List<SetEffectResponse> effects
) {
    public static AdminSetDetailResponse of(EquipmentSet s,
                                            List<SetPieceResponse> pieces,
                                            List<SetEffectResponse> effects) {
        return new AdminSetDetailResponse(
                s.getId(), s.getName(), s.getTotalPieces(), s.isTradeable(), pieces, effects);
    }
}
