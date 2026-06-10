package org.example.gersangtrade.calculator.overlay;

import org.example.gersangtrade.calculator.dto.request.MemberDpsInput;
import org.example.gersangtrade.domain.deck.UserDeck;

import java.util.List;
import java.util.Map;

/**
 * DB 배치 로드 결과 — overlay 적용 전 불변 스냅샷.
 * memberInputs: memberId → MemberDpsInput (요청값. 미지정 멤버는 빈 맵 항목 없음).
 */
public record LoadedDeckState(
        Long deckId,
        UserDeck deck,
        List<LoadedMember> members,
        Map<Long, MemberDpsInput> memberInputs
) {
    public DeckCalculationState toCalculationState() {
        return new DeckCalculationState(deckId, deck, members, memberInputs);
    }
}
