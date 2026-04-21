package org.example.gersangtrade.report.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.example.gersangtrade.domain.report.enums.ReportReason;
import org.example.gersangtrade.domain.report.enums.ReportTargetType;

/**
 * 신고 접수 요청 DTO.
 *
 * @param targetType   신고 대상 유형 (USER / TRADE_LISTING / WANTED_LISTING / CHAT_MESSAGE)
 * @param targetId     신고 대상 엔티티 ID
 * @param reason       신고 사유 분류
 * @param description  신고 상세 내용
 * @param evidenceUrl  증빙 스크린샷 URL (선택)
 * @param chatRoomId   관련 채팅방 ID (선택 — CHAT_MESSAGE 신고 시 권장)
 */
public record ReportCreateRequest(
        @NotNull ReportTargetType targetType,
        @NotNull Long targetId,
        @NotNull ReportReason reason,
        @NotBlank @Size(max = 1000) String description,
        @Size(max = 500) String evidenceUrl,
        Long chatRoomId
) {}
