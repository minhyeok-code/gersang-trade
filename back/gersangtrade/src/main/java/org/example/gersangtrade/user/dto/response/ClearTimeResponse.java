package org.example.gersangtrade.user.dto.response;

import org.example.gersangtrade.domain.user.UserClearTime;

import java.time.LocalDateTime;

/**
 * 클리어타임 저장 응답 DTO.
 *
 * @param id                저장된 클리어타임 ID
 * @param monsterId         몬스터 ID
 * @param monsterName       몬스터명
 * @param deckId            사용한 덱 ID (null: 미지정)
 * @param clearTimeSeconds  클리어 소요 시간 (초)
 * @param expEarned         지급된 EXP
 * @param recordedAt        기록 일시
 */
public record ClearTimeResponse(
        Long id,
        Long monsterId,
        String monsterName,
        Long deckId,
        Integer clearTimeSeconds,
        long expEarned,
        LocalDateTime recordedAt
) {
    public static ClearTimeResponse of(UserClearTime clearTime, long expEarned) {
        return new ClearTimeResponse(
                clearTime.getId(),
                clearTime.getMonster().getId(),
                clearTime.getMonster().getName(),
                clearTime.getDeckId(),
                clearTime.getClearTimeSeconds(),
                expEarned,
                clearTime.getRecordedAt()
        );
    }
}
