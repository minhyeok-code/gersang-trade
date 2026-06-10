package org.example.gersangtrade.home.dto;

import org.example.gersangtrade.domain.user.enums.WatchTargetType;

/**
 * 관심 항목 1개의 시세 집계 응답 DTO.
 */
public record PriceWatchTargetResponse(
        Long entryId,
        WatchTargetType targetType,
        String watchKey,
        String displayLabel,
        SellSummary sell,
        BuySummary buy,
        CompletedSummary completed
) {}
