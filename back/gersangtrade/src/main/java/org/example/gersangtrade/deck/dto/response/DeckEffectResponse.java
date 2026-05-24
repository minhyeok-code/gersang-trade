package org.example.gersangtrade.deck.dto.response;

import org.example.gersangtrade.domain.catalog.DeckBuff;
import org.example.gersangtrade.domain.catalog.DeckBuffSource;
import org.example.gersangtrade.domain.catalog.Spirit;
import org.example.gersangtrade.domain.catalog.SpiritBuff;
import org.example.gersangtrade.domain.catalog.enums.BuffTarget;
import org.example.gersangtrade.domain.catalog.enums.DeckBuffSourceType;
import org.example.gersangtrade.domain.catalog.enums.Element;
import org.example.gersangtrade.domain.catalog.enums.StatType;

import java.util.List;
import java.util.Map;

/**
 * 덱 단위 효과 응답 DTO.
 * 정령/진법/층진 선택 상태와 적용 스탯 요약에 공통으로 사용한다.
 */
public record DeckEffectResponse(
        List<SpiritEntry> spirits,
        DeckBuffSourceEntry jinbeop,
        DeckBuffSourceEntry cheungjin,
        List<StatEntry> stats
) {
    public record SpiritEntry(
            Long id,
            String name,
            String displayLabel,
            String nature,
            String grade,
            String acquireCondition,
            String specialEffectNote,
            List<BuffEntry> buffs
    ) {
        public static SpiritEntry of(Spirit spirit) {
            if (spirit == null) return null;
            return new SpiritEntry(
                    spirit.getId(),
                    spirit.getName(),
                    spirit.getDisplayLabel(),
                    spirit.getNature().name(),
                    spirit.getGrade().name(),
                    spirit.getAcquireCondition(),
                    spirit.getSpecialEffectNote(),
                    spirit.getBuffs().stream().map(BuffEntry::of).toList()
            );
        }
    }

    public record DeckBuffSourceEntry(
            Long id,
            String name,
            DeckBuffSourceType sourceType,
            Long sourceId,
            List<BuffEntry> buffs
    ) {
        public static DeckBuffSourceEntry of(DeckBuffSource source) {
            if (source == null) return null;
            return new DeckBuffSourceEntry(
                    source.getId(),
                    source.getName(),
                    source.getSourceType(),
                    source.getSourceId(),
                    source.getBuffs().stream().map(BuffEntry::of).toList()
            );
        }
    }

    public record BuffEntry(
            StatType statType,
            Element element,
            String valueType,
            float value,
            BuffTarget target
    ) {
        public static BuffEntry of(SpiritBuff buff) {
            return new BuffEntry(
                    buff.getStatType(),
                    buff.getElement(),
                    buff.getStatUnit().name(),
                    buff.getValue(),
                    buff.getTarget()
            );
        }

        public static BuffEntry of(DeckBuff buff) {
            return new BuffEntry(
                    buff.getStatType(),
                    buff.getElement(),
                    buff.getValueType().name(),
                    buff.getValue(),
                    buff.getTarget()
            );
        }
    }

    public record StatEntry(StatType statType, int value) {
        public static List<StatEntry> from(Map<StatType, Integer> stats) {
            return stats.entrySet().stream()
                    .map(entry -> new StatEntry(entry.getKey(), entry.getValue()))
                    .toList();
        }
    }
}
