package org.example.gersangtrade.report.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * 사용자 차단 요청 DTO.
 *
 * @param reason       차단 사유
 * @param blockedUntil 차단 만료 일시 (null이면 영구 차단)
 */
public record UserBlockRequest(
        @NotBlank @Size(max = 500) String reason,
        LocalDateTime blockedUntil
) {}
