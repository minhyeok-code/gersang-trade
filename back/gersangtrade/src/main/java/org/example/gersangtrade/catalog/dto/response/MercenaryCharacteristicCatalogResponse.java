package org.example.gersangtrade.catalog.dto.response;

import java.util.List;

/**
 * 용병 특성 카탈로그 — 스냅샷 뷰어 등 읽기 전용 화면에서 이름·레벨 라벨 조회용.
 */
public record MercenaryCharacteristicCatalogResponse(
        Long mercenaryId,
        List<CharacteristicEntry> characteristics
) {
    public record CharacteristicEntry(
            Long characteristicId,
            String name,
            List<LevelEntry> levels
    ) {}

    public record LevelEntry(
            Integer level,
            String label
    ) {}
}
