package org.example.gersangtrade.deck.dto.response;

import org.example.gersangtrade.domain.deck.UserDeckMemberSlot;
import org.example.gersangtrade.domain.deck.enums.EquipSlot;

public record DeckMemberSlotResponse(
        EquipSlot slot,
        Long itemId,
        String itemName,
        SlotRitualResponse ritual   // null이면 주술 없음
) {
    public static DeckMemberSlotResponse of(UserDeckMemberSlot slot) {
        SlotRitualResponse ritual = slot.getRitual() != null
                ? SlotRitualResponse.of(slot.getRitual())
                : null;
        return new DeckMemberSlotResponse(
                slot.getSlot(),
                slot.getEquipmentItem().getItemId(),
                slot.getEquipmentItem().getItem().getName(),
                ritual
        );
    }
}
