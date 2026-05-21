package org.example.gersangtrade.deck.dto.response;

import org.example.gersangtrade.domain.deck.UserDeckMember;
import org.example.gersangtrade.domain.deck.UserDeckMemberSlot;

import java.util.List;

public record DeckMemberResponse(
        Long id,
        Long mercenaryId,
        String mercenaryName,
        String mercenaryImageUrl,
        List<DeckMemberSlotResponse> slots
) {
    public static DeckMemberResponse of(UserDeckMember member, List<UserDeckMemberSlot> slots) {
        return new DeckMemberResponse(
                member.getId(),
                member.getMercenary().getId(),
                member.getMercenary().getName(),
                member.getMercenary().getImageUrl(),
                slots.stream().map(DeckMemberSlotResponse::of).toList()
        );
    }
}
