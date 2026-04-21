package org.example.gersangtrade.notification.dto.response;

import org.example.gersangtrade.domain.notification.Notification;
import org.example.gersangtrade.domain.notification.enums.NotificationType;

import java.time.LocalDateTime;

/**
 * 알림 응답 DTO.
 * SSE 스트림 및 목록 조회 응답에 사용된다.
 */
public record NotificationResponse(
        Long id,
        NotificationType type,
        Long chatRoomId,
        String message,
        boolean read,
        LocalDateTime createdAt
) {
    public static NotificationResponse of(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getChatRoomId(),
                notification.getMessage(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
