package org.example.gersangtrade.user.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.example.gersangtrade.hunt.service.ClearTimeService;

/**
 * 클리어타임 저장 요청 DTO.
 *
 * @param monsterId         클리어한 몬스터 ID
 * @param deckId            사용한 덱 ID (필수, 본인 소유)
 * @param clearTimeSeconds  클리어 소요 시간 (초 단위, 6~26)
 * @param isPublic          사냥 허브 공개 여부 (null이면 true). 토글 비활성 시 서버가 true로 고정
 */
public record ClearTimeRequest(
        @NotNull Long monsterId,
        @NotNull Long deckId,
        @NotNull
        @Min(ClearTimeService.MIN_CLEAR_TIME_SECONDS)
        @Max(ClearTimeService.MAX_CLEAR_TIME_SECONDS)
        Integer clearTimeSeconds,
        Boolean isPublic
) {
}
