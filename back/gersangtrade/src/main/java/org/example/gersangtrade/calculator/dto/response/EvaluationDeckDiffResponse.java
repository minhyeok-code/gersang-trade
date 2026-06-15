package org.example.gersangtrade.calculator.dto.response;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * 평가 기록의 baseline 덱 vs 현재 덱 비교 응답.
 */
public record EvaluationDeckDiffResponse(
        EvaluationDeckStatus deckStatus,
        Long baselineSnapshotId,
        Long currentSnapshotId,
        List<DeckSnapshotDiffLine> changes,
        JsonNode baselineContent,
        JsonNode currentContent
) {}
