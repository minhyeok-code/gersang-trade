package org.example.gersangtrade.home.dto;

import java.util.List;

/**
 * 관심 아이템 시세 — 판매 집계 요약.
 */
public record SellSummary(
        Long avgPrice,
        int count,
        List<ListingSnapshot> listings
) {}
