package org.example.gersangtrade.catalog.dto.response;

import java.util.List;

/**
 * 용병 특성 배분 UI용 응답 — 덱 멤버 없이 가성비 시뮬레이션·후보 용병 설정에 사용한다.
 */
public record MercenaryCharacteristicSetupResponse(
        Long mercenaryId,
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
            List<LevelEntry> levels
    ) {}

    public record LevelEntry(
            String label,
            Integer level,
            String amount,
            Float amountValue,
            String statType,
            String element
    ) {}
}
