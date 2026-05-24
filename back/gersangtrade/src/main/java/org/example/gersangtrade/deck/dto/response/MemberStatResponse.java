package org.example.gersangtrade.deck.dto.response;

import org.example.gersangtrade.calculator.dto.request.BonusStatTarget;
import org.example.gersangtrade.domain.catalog.enums.StatType;
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
        List<StatEntry> deckBuffStats,
        List<StatEntry> levelBonusStats,
        List<StatEntry> bonusStats,
        StatType resolvedMainStat,
        List<StatEntry> myungwangTransferStats,
        List<MyungwangTransferDetail> myungwangTransferDetails,
        List<ActiveEquipmentSetEffect> equipmentSetEffects,
        List<ActiveSetEffect> ritualSetEffects,
        List<StatEntry> totalStats,
        List<DeckMemberSlotResponse> slots
) {
    public record StatEntry(StatType statType, int value) {}

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

    /** 발동된 주술 세트효과 1건 — 동일 (주술, outcome, 세트, 임계값)이라도 statType별로 행이 분리됨 */
    public record ActiveSetEffect(
            String ritualName,
            String setName,
            String outcome,
            int appliedPieces,
            int requiredPieces,
            StatType statType,
            int statValue
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
            Map<StatType, Integer> deckBuffStatMap,
            Map<StatType, Integer> levelBonusStatMap,
            Map<StatType, Integer> bonusStatMap,
            StatType resolvedMainStat,
            Map<StatType, Integer> myungwangTransferStatMap,
            List<MyungwangTransferDetail> myungwangTransferDetails,
            List<ActiveEquipmentSetEffect> equipmentSetEffects,
            List<ActiveSetEffect> ritualSetEffects,
            Map<StatType, Integer> totalMap,
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
                toEntries(deckBuffStatMap),
                toEntries(levelBonusStatMap),
                toEntries(bonusStatMap),
                resolvedMainStat,
                toEntries(myungwangTransferStatMap),
                myungwangTransferDetails,
                equipmentSetEffects,
                ritualSetEffects,
                toEntries(totalMap),
                slots.stream().map(DeckMemberSlotResponse::of).toList()
        );
    }

    private static List<StatEntry> toEntries(Map<StatType, Integer> map) {
        return map.entrySet().stream()
                .map(e -> new StatEntry(e.getKey(), e.getValue()))
                .toList();
    }
}
