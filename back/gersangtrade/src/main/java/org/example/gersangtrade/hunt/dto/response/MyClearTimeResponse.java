package org.example.gersangtrade.hunt.dto.response;

import org.example.gersangtrade.domain.user.UserClearTime;

import java.time.LocalDateTime;

/**
 * 내 클리어타임 기록 응답 DTO.
 */
public record MyClearTimeResponse(
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
        boolean isPublic,
        LocalDateTime recordedAt
) {
    public static MyClearTimeResponse from(UserClearTime record) {
        return new MyClearTimeResponse(
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
                record.isPublic(),
                record.getRecordedAt()
        );
    }
}
