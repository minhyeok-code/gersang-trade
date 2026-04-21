package org.example.gersangtrade.report.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 신고 처리/기각 요청 DTO.
 * 관리자가 처리 완료(process) 또는 기각(dismiss) 시 사유를 기록한다.
 *
 * @param adminNote 관리자 처리 메모
 */
public record ReportProcessRequest(
        @NotBlank @Size(max = 500) String adminNote
) {}
