package org.example.gersangtrade.deck.dto.response;

import org.example.gersangtrade.domain.deck.UserDeck;

public record DeckSummaryResponse(
        Long id,
        String name,
        boolean active,
        int memberCount,
        Integer attrXValue,
        Integer totalResDown
) {
    public static DeckSummaryResponse of(UserDeck deck, int memberCount) {
        return new DeckSummaryResponse(
                deck.getId(),
                deck.getName(),
                deck.isActive(),
                memberCount,
                deck.getAttrXValue(),
                deck.getTotalResDown()
        );
    }
}
