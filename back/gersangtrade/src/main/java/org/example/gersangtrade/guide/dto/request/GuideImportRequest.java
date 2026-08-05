package org.example.gersangtrade.guide.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * 가이드 일괄 주입 요청 (관리자).
 * 이미지 추출 JSON을 그대로 받는다. link.matchName으로 카탈로그 이름 매칭을 시도한다.
 * 알 수 없는 필드(_comment, needsReview 등)는 무시한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GuideImportRequest(
        @NotEmpty List<GuideImport> guides
) {}
