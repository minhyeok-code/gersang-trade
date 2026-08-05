package org.example.gersangtrade.guide.dto.response;

import java.util.List;

/**
 * 가이드 일괄 주입 리포트.
 * 주입된 가이드는 모두 published=false 상태 — 관리자가 검수 후 공개한다.
 */
public record GuideImportReport(
        int guidesCreated,
        List<GuideImportResult> results
) {}
