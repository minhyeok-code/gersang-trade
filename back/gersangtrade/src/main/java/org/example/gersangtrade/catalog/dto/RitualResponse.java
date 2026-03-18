package org.example.gersangtrade.catalog.dto;

import org.example.gersangtrade.domain.catalog.Ritual;
import org.example.gersangtrade.domain.catalog.enums.RitualType;

/**
 * 주술 정보 응답 DTO.
 * 아이템 선택 후 적용 가능한 주술 목록 조회에 사용된다.
 */
public record RitualResponse(
        Long id,
        String displayName,
        RitualType ritualType,
        String successMark,
        String greatSuccessMark
) {
    public static RitualResponse from(Ritual ritual) {
        return new RitualResponse(
                ritual.getId(),
                ritual.getDisplayName(),
                ritual.getRitualType(),
                ritual.getSuccessMark(),
                ritual.getGreatSuccessMark()
        );
    }
}
