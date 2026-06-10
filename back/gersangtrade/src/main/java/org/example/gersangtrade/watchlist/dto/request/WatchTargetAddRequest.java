package org.example.gersangtrade.watchlist.dto.request;

import jakarta.validation.constraints.NotNull;
import org.example.gersangtrade.domain.user.enums.SetComposition;
import org.example.gersangtrade.domain.user.enums.WatchTargetType;

public record WatchTargetAddRequest(
        @NotNull WatchTargetType targetType,
        Long itemId,
        String ritualMark,
        Long setId,
        SetComposition composition,
        Integer ritualCount
) {}
