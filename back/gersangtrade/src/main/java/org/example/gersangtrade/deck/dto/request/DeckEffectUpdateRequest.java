package org.example.gersangtrade.deck.dto.request;

/**
 * 덱 단위 효과 수정 요청.
 * 정령은 최대 2개, 진법/층진은 각각 1개만 선택한다.
 */
public record DeckEffectUpdateRequest(
        Long spirit1Id,
        Long spirit2Id,
        Long jinbeopSourceId,
        Long cheungjinSourceId
) {
}
