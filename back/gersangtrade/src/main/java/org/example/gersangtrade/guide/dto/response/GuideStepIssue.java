package org.example.gersangtrade.guide.dto.response;

/**
 * 주입 중 카탈로그 매칭에 실패한 스텝 항목.
 * status: "UNMATCHED"(0건) | "AMBIGUOUS"(2건 이상).
 * 해당 스텝은 링크 없이 저장되며, 관리자가 검수해 수정한다.
 */
public record GuideStepIssue(
        int order,
        String label,
        String kind,
        String matchName,
        String status
) {}
