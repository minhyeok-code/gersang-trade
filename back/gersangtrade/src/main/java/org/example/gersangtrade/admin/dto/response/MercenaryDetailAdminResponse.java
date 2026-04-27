package org.example.gersangtrade.admin.dto.response;

import org.example.gersangtrade.domain.catalog.Mercenary;
import org.example.gersangtrade.domain.catalog.MercenarySkill;
import org.example.gersangtrade.domain.catalog.MercenaryStat;
import org.example.gersangtrade.domain.catalog.enums.MercenaryCategory;
import org.example.gersangtrade.domain.catalog.enums.Nation;
import org.example.gersangtrade.domain.catalog.enums.Nature;
import org.example.gersangtrade.domain.catalog.enums.StatType;

import java.util.List;

/** 관리자 용병 상세 응답 (기본정보 + 스탯 + 스킬) */
public record MercenaryDetailAdminResponse(
        Long id,
        String name,
        MercenaryCategory category,
        Nation nation,
        Nature nature,
        Integer natureValue,
        boolean comingSoon,
        List<StatEntry> stats,
        List<String> skills
) {
    public record StatEntry(Long id, StatType statType, Integer value) {}

    public static MercenaryDetailAdminResponse of(Mercenary m,
                                                   List<MercenaryStat> stats,
                                                   List<MercenarySkill> skills) {
        return new MercenaryDetailAdminResponse(
                m.getId(),
                m.getName(),
                m.getCategory(),
                m.getNation(),
                m.getNature(),
                m.getNatureValue(),
                m.isComingSoon(),
                stats.stream().map(s -> new StatEntry(s.getId(), s.getStatKey(), s.getStatValue())).toList(),
                skills.stream().map(MercenarySkill::getSkillName).toList()
        );
    }
}
