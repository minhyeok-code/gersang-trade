package org.example.gersangtrade.guide.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 유저 가이드 스텝 표시 내용 수정 요청.
 */
public record GuideStepUpdateRequest(
        @NotBlank @Size(max = 100) String label,
        @Size(max = 255) String note,
        @Size(max = 500) String iconUrl
) {}
