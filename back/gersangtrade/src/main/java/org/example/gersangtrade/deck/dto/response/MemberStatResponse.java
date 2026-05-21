package org.example.gersangtrade.deck.dto.response;

import org.example.gersangtrade.domain.catalog.enums.StatType;
import org.example.gersangtrade.domain.deck.UserDeckMember;
import org.example.gersangtrade.domain.deck.UserDeckMemberSlot;

import java.util.List;
import java.util.Map;

/**
 * 용병 합산 스탯 응답 DTO.
 * 용병 기본 스탯 + 착용 장비(SELF scope) 스탯 합산 결과와 슬롯 착용 목록을 반환한다.
 */
public record MemberStatResponse(
        Long memberId,
        Long mercenaryId,
        String mercenaryName,
        String mercenaryImageUrl,
        List<StatEntry> baseStats,
        List<StatEntry> equipStats,
        List<StatEntry> totalStats,
        List<DeckMemberSlotResponse> slots
) {
    public record StatEntry(StatType statType, int value) {}

    public static MemberStatResponse of(
            UserDeckMember member,
            Map<StatType, Integer> baseStatMap,
            Map<StatType, Integer> equipStatMap,
            Map<StatType, Integer> totalMap,
            List<UserDeckMemberSlot> slots) {

        return new MemberStatResponse(
                member.getId(),
                member.getMercenary().getId(),
                member.getMercenary().getName(),
                member.getMercenary().getImageUrl(),
                toEntries(baseStatMap),
                toEntries(equipStatMap),
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
