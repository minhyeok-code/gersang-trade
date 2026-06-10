package org.example.gersangtrade.home.dto;

import java.util.List;

/**
 * 관심 아이템 시세 — 거래완료 집계 요약.
 * dataQuality: "OK" = 정상 집계, "LIMITED" = Step 0 이전 SET·비대표 피스 (데이터 누락 가능)
 */
public record CompletedSummary(
        int count,
        String dataQuality,
        List<CompletedSnapshot> trades
) {}
