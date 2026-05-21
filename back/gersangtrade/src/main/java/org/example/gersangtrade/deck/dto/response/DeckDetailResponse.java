package org.example.gersangtrade.deck.dto.response;

import org.example.gersangtrade.domain.deck.UserDeck;

import java.util.List;

public record DeckDetailResponse(
        Long id,
        String name,
        boolean active,
        Integer attrXValue,
        Integer totalResDown,
        List<DeckMemberResponse> members
) {
    public static DeckDetailResponse of(UserDeck deck, List<DeckMemberResponse> members) {
        return new DeckDetailResponse(
                deck.getId(),
                deck.getName(),
                deck.isActive(),
                deck.getAttrXValue(),
                deck.getTotalResDown(),
                members
        );
    }
}
