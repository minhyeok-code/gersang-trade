package org.example.gersangtrade.report.dto.response;

import org.example.gersangtrade.domain.report.Report;
import org.example.gersangtrade.domain.report.enums.ReportReason;
import org.example.gersangtrade.domain.report.enums.ReportStatus;
import org.example.gersangtrade.domain.report.enums.ReportTargetType;
import org.example.gersangtrade.domain.report.enums.ReporterType;

import java.time.LocalDateTime;

/**
 * 신고 응답 DTO.
 *
 * @param id             신고 ID
 * @param reporterType   신고 주체 유형 (USER / SYSTEM)
 * @param reporterName   신고자 닉네임 (SYSTEM이면 null)
 * @param targetType     신고 대상 유형
 * @param targetId       신고 대상 ID
 * @param reason         신고 사유 분류
 * @param description    신고 상세 내용
 * @param evidenceUrl    증빙 스크린샷 URL
 * @param chatRoomId     관련 채팅방 ID
 * @param status         처리 상태
 * @param adminNote      관리자 처리 메모
 * @param processedByName 처리 관리자 닉네임 (미처리 시 null)
 * @param processedAt    처리 시각 (미처리 시 null)
 * @param createdAt      신고 접수 시각
 */
public record ReportResponse(
        Long id,
        ReporterType reporterType,
        String reporterName,
        ReportTargetType targetType,
        Long targetId,
        ReportReason reason,
        String description,
        String evidenceUrl,
        Long chatRoomId,
        ReportStatus status,
        String adminNote,
        String processedByName,
        LocalDateTime processedAt,
        LocalDateTime createdAt
) {
    public static ReportResponse from(Report report) {
        return new ReportResponse(
                report.getId(),
                report.getReporterType(),
                report.getReporter() != null ? report.getReporter().getNickname() : null,
                report.getTargetType(),
                report.getTargetId(),
                report.getReasonCategory(),
                report.getDescription(),
                report.getEvidenceUrl(),
                report.getChatRoomId(),
                report.getStatus(),
                report.getAdminNote(),
                report.getProcessedBy() != null ? report.getProcessedBy().getNickname() : null,
                report.getProcessedAt(),
                report.getCreatedAt()
        );
    }
}
