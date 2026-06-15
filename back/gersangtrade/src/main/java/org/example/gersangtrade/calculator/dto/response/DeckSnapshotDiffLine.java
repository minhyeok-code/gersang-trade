package org.example.gersangtrade.calculator.dto.response;

/**
 * 평가 당시 덱 vs 현재 덱 장비 차이 한 줄.
 */
public record DeckSnapshotDiffLine(
        Long memberId,
        String mercenaryName,
        String slot,
        String beforeItemName,
        String afterItemName
) {}
