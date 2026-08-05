package org.example.gersangtrade.guide.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 유저 가이드 사본 이름 변경 요청.
 */
public record UserGuideRenameRequest(
        @NotBlank @Size(max = 100) String title
) {}
