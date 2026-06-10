package org.example.gersangtrade.home.dto;

import java.util.List;

/**
 * 관심 아이템 시세 — 구매 집계 요약.
 */
public record BuySummary(
        Long avgPrice,
        int count,
        List<WantedSnapshot> listings
) {}
