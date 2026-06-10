package org.example.gersangtrade.home.dto;

import java.util.List;

/**
 * GET /api/home/price-watch 응답 최상위 DTO.
 */
public record PriceWatchResponse(
        Integer serverId,
        String serverName,
        List<PriceWatchTargetResponse> targets
) {}
