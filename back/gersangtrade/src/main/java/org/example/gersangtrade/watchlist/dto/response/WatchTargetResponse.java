package org.example.gersangtrade.watchlist.dto.response;

import org.example.gersangtrade.domain.user.UserWatchTarget;
import org.example.gersangtrade.domain.user.enums.SetComposition;
import org.example.gersangtrade.domain.user.enums.WatchTargetType;

import java.time.LocalDateTime;

public record WatchTargetResponse(
        Long id,
        WatchTargetType targetType,
        String watchKey,
        String displayLabel,
        Long itemId,
        Long setId,
        String setName,
        SetComposition composition,
        Integer ritualCount,
        String ritualMark,
        int sortOrder,
        LocalDateTime createdAt
) {
    public static WatchTargetResponse from(UserWatchTarget w, String displayLabel) {
        return new WatchTargetResponse(
                w.getId(),
                w.getTargetType(),
                w.getWatchKey(),
                displayLabel,
                w.getItem() != null ? w.getItem().getId() : null,
                w.getEquipmentSet() != null ? w.getEquipmentSet().getId() : null,
                w.getEquipmentSet() != null ? w.getEquipmentSet().getName() : null,
                w.getComposition(),
                w.getRitualCount(),
                w.getRitualMark(),
                w.getSortOrder(),
                w.getCreatedAt()
        );
    }
}
