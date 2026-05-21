package org.example.gersangtrade.deck.dto.response;

import org.example.gersangtrade.domain.deck.UserDeckMemberSlotRitual;
import org.example.gersangtrade.domain.listing.enums.RitualOutcome;

public record SlotRitualResponse(
        Long ritualId,
        String displayName,
        RitualOutcome outcome
) {
    public static SlotRitualResponse of(UserDeckMemberSlotRitual r) {
        return new SlotRitualResponse(
                r.getRitual().getId(),
                r.getRitual().getDisplayName(),
                r.getOutcome()
        );
    }
}
