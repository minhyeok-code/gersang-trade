package org.example.gersangtrade.admin.dto.response;

import org.example.gersangtrade.domain.catalog.Mercenary;
import org.example.gersangtrade.domain.catalog.MercenaryStat;
import org.example.gersangtrade.domain.catalog.enums.MercenaryCategory;
import org.example.gersangtrade.domain.catalog.enums.Nature;
import org.example.gersangtrade.domain.catalog.enums.StatType;

import java.util.List;

/**
 * 관리자 용병 목록 항목 응답.
 * 특성 개수와 스탯 목록을 포함해 관리자가 미입력 항목을 한눈에 파악할 수 있도록 한다.
 */
public record MercenaryAdminResponse(
        Long id,
        String name,
        MercenaryCategory category,
        Nature nature,
        Integer natureValue,
        int characteristicCount,
        List<StatEntry> stats
) {
    public record StatEntry(StatType statType, Integer value) {}

    public static MercenaryAdminResponse of(Mercenary m, int characteristicCount,
                                            List<MercenaryStat> stats) {
        return new MercenaryAdminResponse(
                m.getId(),
                m.getName(),
                m.getCategory(),
                m.getNature(),
                m.getNatureValue(),
                characteristicCount,
                stats.stream()
                        .map(s -> new StatEntry(s.getStatKey(), s.getStatValue()))
                        .toList()
        );
    }
}
