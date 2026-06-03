package org.example.gersangtrade.deck.dto.response;

import org.example.gersangtrade.calculator.dto.request.BonusStatTarget;
import org.example.gersangtrade.domain.catalog.enums.StatType;
import org.example.gersangtrade.domain.catalog.enums.StatUnit;
import org.example.gersangtrade.domain.deck.UserDeckMember;
import org.example.gersangtrade.domain.deck.UserDeckMemberSlot;

import java.util.List;
import java.util.Map;

/**
 * 용병 합산 스탯 응답 DTO.
 * 기본 스탯 + 장비(SELF scope) + 특성 + 주술 스탯 합산 결과와 슬롯·레벨 정보를 반환한다.
 */
public record MemberStatResponse(
        Long memberId,
        Long mercenaryId,
        String mercenaryName,
        String mercenaryImageUrl,
        int level,
        BonusStatTarget bonusTarget,
        int bonusAmount,
        List<StatEntry> baseStats,
        List<StatEntry> equipStats,
        List<StatEntry> setEffectStats,
        List<StatEntry> characteristicStats,
        List<StatEntry> partyCharacteristicStats,
        List<StatEntry> enemyDebuffStats,
        List<StatEntry> ritualStats,
        /** 주술 세트효과 합산 — FLAT·PERCENT 모두 statType별로 집계 */
        List<StatEntry> ritualSetEffectStats,
        List<StatEntry> deckBuffStats,
        List<DeckBuffDetail> deckBuffDetails,
        List<StatEntry> levelBonusStats,
        List<StatEntry> bonusStats,
        /** 주인공 국가 속성 버프 (동일 Nature 아군 ELEMENT_VALUE) */
        List<StatEntry> protagonistBuffStats,
        /** 각성 명왕 편성 수 × 아군 속성값 버프 (+5, 토 +2) */
        List<StatEntry> awakenedMyeongwangBuffStats,
        /** 사인검 등 ALLY scope 장비 버프 (같은 속성 아군 전체) */
        List<StatEntry> partyItemBuffStats,
        StatType resolvedMainStat,
        List<StatEntry> myungwangTransferStats,
        List<MyungwangTransferDetail> myungwangTransferDetails,
        List<ActiveEquipmentSetEffect> equipmentSetEffects,
        List<ActiveSetEffect> ritualSetEffects,
        List<StatEntry> totalStats,
        /** 전설장수 아군 속성 버프 합산 — compound key 예: DAMAGE_PERCENT_FIRE */
        List<CompoundStatEntry> lgAllyElementalStats,
        /** 전설장수 아군 속성 버프 장수별 상세 */
        List<LgAllyDetail> lgAllyDetails,
        List<DeckMemberSlotResponse> slots
) {
    public record StatEntry(StatType statType, int value) {}

    /** 원소 속성이 포함된 복합 스탯 표기용 엔트리 (예: statKey="DAMAGE_PERCENT_FIRE") */
    public record CompoundStatEntry(String statKey, int value) {}

    /** 전설장수 아군 버프 장수별 1건 */
    public record LgAllyDetail(String mercenaryName, String statKey, int value) {}

    /** 덱 효과(정령·진법·층진) 출처별 스탯 기여 1건 */
    public record DeckBuffDetail(String sourceName, String sourceType, StatType statType, int value) {}

    /** 명왕 스탯 이전 1건 — 수신 멤버에게만 내려간다 */
    public record MyungwangTransferDetail(String sourceMercenaryName, StatType statType, int value) {}

    /** 발동된 장비 세트효과 1건 */
    public record ActiveEquipmentSetEffect(
            String setName,
            int appliedPieces,
            int requiredPieces,
            StatType statType,
            int statValue
    ) {}

    /** 발동된 주술 세트효과 1건 — outcome은 <천추>, <북두칠성> 등 표기 마크 */
    public record ActiveSetEffect(
            String ritualName,
            String setName,
            String outcome,
            int appliedPieces,
            int requiredPieces,
            StatType statType,
            int statValue,
            StatUnit statUnit
    ) {}

    public static MemberStatResponse of(
            UserDeckMember member,
            Map<StatType, Integer> baseStatMap,
            Map<StatType, Integer> equipStatMap,
            Map<StatType, Integer> setEffectStatMap,
            Map<StatType, Integer> characteristicStatMap,
            Map<StatType, Integer> partyCharacteristicStatMap,
            Map<StatType, Integer> enemyDebuffStatMap,
            Map<StatType, Integer> ritualStatMap,
            Map<StatType, Integer> ritualSetEffectStatMap,
            Map<StatType, Integer> deckBuffStatMap,
            List<DeckBuffDetail> deckBuffDetails,
            Map<StatType, Integer> levelBonusStatMap,
            Map<StatType, Integer> bonusStatMap,
            Map<StatType, Integer> protagonistBuffStatMap,
            Map<StatType, Integer> awakenedMyeongwangBuffStatMap,
            Map<StatType, Integer> partyItemBuffStatMap,
            StatType resolvedMainStat,
            Map<StatType, Integer> myungwangTransferStatMap,
            List<MyungwangTransferDetail> myungwangTransferDetails,
            List<ActiveEquipmentSetEffect> equipmentSetEffects,
            List<ActiveSetEffect> ritualSetEffects,
            Map<StatType, Integer> totalMap,
            Map<String, Integer> lgAllyElementalDisplayMap,
            List<LgAllyDetail> lgAllyDetails,
            List<UserDeckMemberSlot> slots) {

        return new MemberStatResponse(
                member.getId(),
                member.getMercenary().getId(),
                member.getMercenary().getName(),
                member.getMercenary().getImageUrl(),
                member.getLevel(),
                member.getBonusTarget(),
                member.getBonusAmount(),
                toEntries(baseStatMap),
                toEntries(equipStatMap),
                toEntries(setEffectStatMap),
                toEntries(characteristicStatMap),
                toEntries(partyCharacteristicStatMap),
                toEntries(enemyDebuffStatMap),
                toEntries(ritualStatMap),
                toEntries(ritualSetEffectStatMap),
                toEntries(deckBuffStatMap),
                deckBuffDetails,
                toEntries(levelBonusStatMap),
                toEntries(bonusStatMap),
                toEntries(protagonistBuffStatMap),
                toEntries(awakenedMyeongwangBuffStatMap),
                toEntries(partyItemBuffStatMap),
                resolvedMainStat,
                toEntries(myungwangTransferStatMap),
                myungwangTransferDetails,
                equipmentSetEffects,
                ritualSetEffects,
                toEntries(totalMap),
                lgAllyElementalDisplayMap.entrySet().stream()
                        .map(e -> new CompoundStatEntry(e.getKey(), e.getValue()))
                        .toList(),
                lgAllyDetails,
                slots.stream().map(DeckMemberSlotResponse::of).toList()
        );
    }

    private static List<StatEntry> toEntries(Map<StatType, Integer> map) {
        return map.entrySet().stream()
                .map(e -> new StatEntry(e.getKey(), e.getValue()))
                .toList();
    }
}
