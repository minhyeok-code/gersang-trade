package org.example.gersangtrade.deck.dto.response;

import java.util.List;

/**
 * 덱 멤버 특성 조회 응답 DTO.
 * 해당 용병의 전체 특성 정의와 현재 선택된 레벨을 함께 반환한다.
 */
public record MemberCharacteristicResponse(
        Long memberId,
        int level,
        int maxCharacteristicPoints,
        List<CharacteristicEntry> characteristics
) {
    public record CharacteristicEntry(
            Long characteristicId,
            String key,
            String name,
            Integer point,
            String description,
            String requiredCharacteristicKey,
            String applyType,
            Integer selectedLevel,   // null이면 미선택
            List<LevelEntry> levels
    ) {}

    public record LevelEntry(
            String label,
            Integer level,
            String amount,
            Float amountValue,
            String statType          // null이면 미매핑
    ) {}
}
