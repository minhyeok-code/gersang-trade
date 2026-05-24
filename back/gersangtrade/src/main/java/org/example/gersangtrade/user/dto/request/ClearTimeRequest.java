package org.example.gersangtrade.user.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 클리어타임 저장 요청 DTO.
 *
 * @param monsterId         클리어한 몬스터 ID
 * @param deckId            사용한 덱 ID (선택)
 * @param clearTimeSeconds  클리어 소요 시간 (초 단위, 1 이상)
 */
public record ClearTimeRequest(
        @NotNull Long monsterId,
        Long deckId,
        @NotNull @Min(1) Integer clearTimeSeconds
) {
}
