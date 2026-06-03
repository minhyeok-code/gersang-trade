package org.example.gersangtrade.deck.dto.request;

/**
 * 덱 단위 효과 수정 요청.
 * 정령은 최대 2개, 진법/층진은 각각 1개만 선택한다.
 * 공명/가호는 레벨(1~30)을 직접 입력. null이면 미적용.
 */
public record DeckEffectUpdateRequest(
        Long spirit1Id,
        Long spirit2Id,
        Long jinbeopSourceId,
        Long cheungjinSourceId,
        Integer gonmyeongLevel,
        Integer gahoLevel
) {
}
