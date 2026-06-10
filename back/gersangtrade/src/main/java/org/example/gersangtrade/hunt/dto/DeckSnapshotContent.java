package org.example.gersangtrade.hunt.dto;

import org.example.gersangtrade.calculator.dto.request.MemberDpsInput;
import org.example.gersangtrade.calculator.dto.request.ResistanceType;
import org.example.gersangtrade.deck.dto.response.DeckEffectResponse;
import org.example.gersangtrade.deck.dto.response.DeckMemberResponse;

import java.util.List;

/**
 * 클리어타임 저장 시 고정하는 덱 스냅샷 JSON 구조.
 */
public record DeckSnapshotContent(
        Long deckId,
        String deckName,
        Integer attrXValue,
        Integer totalResDown,
        DeckEffectResponse effects,
        List<SnapshotMember> members,
        DpsContext dpsContext
) {
    public record SnapshotMember(
            DeckMemberResponse member,
            List<CharacteristicSelection> characteristics
    ) {}

    /** 스냅샷·해시 판별에 필요한 선택 특성만 저장 */
    public record CharacteristicSelection(
            Long characteristicId,
            Integer selectedLevel
    ) {}

    public record DpsContext(
            ResistanceType resistanceType,
            List<MemberDpsInput> memberInputs,
            List<MemberElementValue> memberElementValues
    ) {
        /** 멤버별 DPS 계산 시점 유효 속성값 */
        public record MemberElementValue(Long memberId, Integer elementValue) {}
    }
}
