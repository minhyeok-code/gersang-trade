package org.example.gersangtrade.hunt.dto.response;

import org.example.gersangtrade.domain.user.UserClearTime;

import java.time.LocalDateTime;

/**
 * 사냥 허브 공개 클리어타임 기록 응답 DTO.
 */
public record HuntPublicRecordResponse(
        Long id,
        Long monsterId,
        String monsterName,
        Integer clearTimeSeconds,
        Integer totalResistPierce,
        Integer totalElementPierce,
        Long rawDps,
        Long adjustDps,
        long finalDps,
        Integer resistAfterDebuff,
        Integer effectiveMonsterElement,
        Double resistPassRate,
        Long deckSnapshotId,
        String authorNickname,
        LocalDateTime recordedAt
) {
    public static HuntPublicRecordResponse from(UserClearTime record) {
        return new HuntPublicRecordResponse(
                record.getId(),
                record.getMonster().getId(),
                record.getMonster().getName(),
                record.getClearTimeSeconds(),
                record.getTotalResistPierce(),
                record.getTotalElementPierce(),
                record.getRawDps(),
                record.getAdjustDps(),
                record.getFinalDps(),
                record.getResistAfterDebuff(),
                record.getEffectiveMonsterElement(),
                record.getResistPassRate(),
                record.getDeckSnapshot().getId(),
                record.getUser().getNickname(),
                record.getRecordedAt()
        );
    }
}
