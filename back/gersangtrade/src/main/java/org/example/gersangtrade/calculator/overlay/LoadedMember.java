package org.example.gersangtrade.calculator.overlay;

import org.example.gersangtrade.domain.catalog.Mercenary;
import org.example.gersangtrade.domain.deck.UserDeckMemberCharacteristic;
import org.example.gersangtrade.domain.deck.UserDeckMemberSlot;

import java.util.List;

/**
 * DB 배치 로드 직후 멤버 스냅샷 — overlay 적용 전 불변 단위.
 * 용병·슬롯·특성을 명시적 필드로 분리해 overlay 교체 시 엔티티 변이를 피한다.
 * DeckStateMerger가 이 레코드를 조합해 DeckCalculationState를 만든다.
 */
public record LoadedMember(
        Long memberId,
        Mercenary mercenary,
        List<UserDeckMemberSlot> slots,
        List<UserDeckMemberCharacteristic> characteristics
) {}
