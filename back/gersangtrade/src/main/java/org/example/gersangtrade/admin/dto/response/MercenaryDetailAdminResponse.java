package org.example.gersangtrade.admin.dto.response;

import org.example.gersangtrade.domain.catalog.Mercenary;
import org.example.gersangtrade.domain.catalog.MercenarySkill;
import org.example.gersangtrade.domain.catalog.MercenarySkillEffect;
import org.example.gersangtrade.domain.catalog.MercenaryStat;
import org.example.gersangtrade.domain.catalog.enums.MercenaryCategory;
import org.example.gersangtrade.domain.catalog.enums.Nation;
import org.example.gersangtrade.domain.catalog.enums.Nature;
import org.example.gersangtrade.domain.catalog.enums.StatType;

import java.util.List;
import java.util.Map;

/** 관리자 용병 상세 응답 (기본정보 + 스탯 + 스킬 + 스킬 효과) */
public record MercenaryDetailAdminResponse(
        Long id,
        String name,
        MercenaryCategory category,
        Nation nation,
        Nature nature,
        Integer natureValue,
        boolean comingSoon,
        List<StatEntry> stats,
        List<SkillEntry> skills
) {
    public record StatEntry(Long id, StatType statType, Integer value) {}

    public record SkillEntry(Long id, String skillName, List<EffectEntry> effects) {}

    public record EffectEntry(StatType statKey, Integer statValue) {}

    public static MercenaryDetailAdminResponse of(Mercenary m,
                                                   List<MercenaryStat> stats,
                                                   List<MercenarySkill> skills,
                                                   Map<Long, List<MercenarySkillEffect>> effectsBySkillId) {
        return new MercenaryDetailAdminResponse(
                m.getId(),
                m.getName(),
                m.getCategory(),
                m.getNation(),
                m.getNature(),
                m.getNatureValue(),
                m.isComingSoon(),
                stats.stream().map(s -> new StatEntry(s.getId(), s.getStatKey(), s.getStatValue())).toList(),
                skills.stream().map(s -> {
                    List<EffectEntry> fx = effectsBySkillId.getOrDefault(s.getId(), List.of()).stream()
                            .map(e -> new EffectEntry(e.getStatKey(), e.getStatValue())).toList();
                    return new SkillEntry(s.getId(), s.getSkillName(), fx);
                }).toList()
        );
    }
}
