package org.example.gersangtrade.notification.controller;

import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.notification.dto.response.NotificationResponse;
import org.example.gersangtrade.notification.service.NotificationService;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * 알림 컨트롤러.
 *
 * SSE 구독, 알림 목록 조회, 일괄 읽음 처리를 제공한다.
 * 모든 엔드포인트는 로그인(JWT) 필수.
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * SSE 구독.
     * 클라이언트는 EventSource API로 연결하며, 새 알림 발생 시 서버가 push한다.
     * 연결 즉시 미읽음 알림이 전송된다.
     *
     * GET /api/notifications/subscribe
     */
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@AuthenticationPrincipal Long userId) {
        return notificationService.subscribe(userId);
    }

    /**
     * 알림 목록 조회 (전체, 최대 50건, 최신순).
     *
     * GET /api/notifications
     */
    @GetMapping
    public List<NotificationResponse> getNotifications(@AuthenticationPrincipal Long userId) {
        return notificationService.getAll(userId);
    }

    /**
     * 미읽음 알림 전체 읽음 처리.
     *
     * PATCH /api/notifications/read-all
     */
    @PatchMapping("/read-all")
    public void markAllRead(@AuthenticationPrincipal Long userId) {
        notificationService.markAllRead(userId);
    }

    /**
     * 알림 개별 읽음 처리.
     * 본인 소유 알림이 아니면 404 반환.
     *
     * PATCH /api/notifications/{id}/read
     */
    @PatchMapping("/{id}/read")
    public void markRead(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        notificationService.markRead(userId, id);
    }
}
