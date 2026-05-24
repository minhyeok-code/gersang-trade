package org.example.gersangtrade.deck.dto.response;

import java.util.List;

/**
 * 덱 효과 선택지 응답 DTO.
 * 프론트에서 정령/진법/층진 선택 박스를 구성할 때 사용한다.
 */
public record DeckEffectCatalogResponse(
        List<DeckEffectResponse.SpiritEntry> spirits,
        List<DeckEffectResponse.DeckBuffSourceEntry> jinbeops,
        List<DeckEffectResponse.DeckBuffSourceEntry> cheungjins
) {
}
