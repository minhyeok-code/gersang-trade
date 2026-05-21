package org.example.gersangtrade.admin.dto.response;

import java.util.List;

/** 스킬 계수 이슈 전체 목록 응답 */
public record SkillCoefficientIssueListResponse(
        int total,
        int issueCount,
        List<SkillCoefficientIssueResponse> entries
) {}
