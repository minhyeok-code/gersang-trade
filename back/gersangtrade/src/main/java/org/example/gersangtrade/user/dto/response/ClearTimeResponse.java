package org.example.gersangtrade.user.dto.response;

import org.example.gersangtrade.domain.user.UserClearTime;

import java.time.LocalDateTime;

/**
 * 클리어타임 저장 응답 DTO.
 */
public record ClearTimeResponse(
        Long id,
        Long monsterId,
        String monsterName,
        Long deckId,
        Long deckSnapshotId,
        Integer clearTimeSeconds,
        long rawDps,
        long adjustDps,
        long finalDps,
        boolean isPublic,
        long expEarned,
        LocalDateTime recordedAt
) {
    public static ClearTimeResponse of(UserClearTime clearTime, long expEarned) {
        return new ClearTimeResponse(
                clearTime.getId(),
                clearTime.getMonster().getId(),
                clearTime.getMonster().getName(),
                clearTime.getDeckId(),
                clearTime.getDeckSnapshot().getId(),
                clearTime.getClearTimeSeconds(),
                clearTime.getRawDps() != null ? clearTime.getRawDps() : 0L,
                clearTime.getAdjustDps() != null ? clearTime.getAdjustDps() : 0L,
                clearTime.getFinalDps(),
                clearTime.isPublic(),
                expEarned,
                clearTime.getRecordedAt()
        );
    }
}
