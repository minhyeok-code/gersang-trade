package org.example.gersangtrade.admin.dto.response;

import org.example.gersangtrade.domain.catalog.MercenaryCharacteristic;
import org.example.gersangtrade.domain.catalog.MercenaryCharacteristicLevel;
import org.example.gersangtrade.domain.catalog.enums.StatType;

import java.util.List;

/**
 * 용병 특성 상세 응답 (레벨 수치 포함).
 *
 * <p>선행 특성은 requiredCharacteristicId로 참조한다.
 * 프런트에서 트리 구조 렌더링 시 requiredCharacteristicId를 기준으로 부모-자식 연결.
 */
public record CharacteristicAdminResponse(
        Long id,
        String name,
        Integer point,
        String description,
        Long requiredCharacteristicId,   // null이면 루트 특성
        List<LevelEntry> levels
) {
    public record LevelEntry(
            Long id,
            String label,
            Integer level,
            String amount,
            Float amountValue,
            StatType statType
    ) {
        public static LevelEntry from(MercenaryCharacteristicLevel l) {
            return new LevelEntry(l.getId(), l.getLabel(), l.getLevel(),
                    l.getAmount(), l.getAmountValue(), l.getStatType());
        }
    }

    public static CharacteristicAdminResponse from(
            MercenaryCharacteristic c,
            Long requiredCharacteristicId,
            List<MercenaryCharacteristicLevel> levels) {
        return new CharacteristicAdminResponse(
                c.getId(),
                c.getName(),
                c.getPoint(),
                c.getDescription(),
                requiredCharacteristicId,
                levels.stream().map(LevelEntry::from).toList()
        );
    }
}
