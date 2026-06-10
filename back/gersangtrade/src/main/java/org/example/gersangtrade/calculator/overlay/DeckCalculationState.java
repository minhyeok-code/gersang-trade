package org.example.gersangtrade.calculator.overlay;

import org.example.gersangtrade.calculator.dto.request.MemberDpsInput;
import org.example.gersangtrade.domain.deck.UserDeck;

import java.util.List;
import java.util.Map;

/**
 * DPS 계산 파이프라인 입력 — overlay merge 후 (또는 loaded와 동일 내용).
 * overlay가 null이면 LoadedDeckState.toCalculationState()로 그대로 변환.
 */
public record DeckCalculationState(
        Long deckId,
        UserDeck deck,
        List<LoadedMember> members,
        Map<Long, MemberDpsInput> memberInputs
) {}
