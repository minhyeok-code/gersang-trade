package org.example.gersangtrade.guide.dto.response;

import java.util.List;

/**
 * 가이드 한 건의 주입 결과.
 */
public record GuideImportResult(
        Long guideId,
        String title,
        boolean targetMercenaryMatched,
        int stepCount,
        int linkedCount,
        int unmatchedCount,
        int ambiguousCount,
        List<GuideStepIssue> issues
) {}
