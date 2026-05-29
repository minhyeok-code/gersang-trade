package org.example.gersangtrade.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gersangtrade.domain.notification.Notification;
import org.example.gersangtrade.domain.notification.enums.NotificationType;
import org.example.gersangtrade.domain.user.User;
import org.example.gersangtrade.notification.dto.response.NotificationResponse;
import org.example.gersangtrade.notification.repository.NotificationRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 알림 서비스.
 *
 * 알림 발생 시 DB 저장 후 WebSocket으로 실시간 push한다.
 * SSE /subscribe 엔드포인트는 테스트 및 하위 호환용으로 유지한다.
 *
 * 오프라인 사용자: DB에만 저장 → 다음 WS 연결 시점에 미읽음 목록으로 제공
 * 온라인 사용자: DB 저장 + WebSocket push (즉시 전달)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final long SSE_TIMEOUT_MS = 30 * 60 * 1000L;

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /** userId → SseEmitter (SSE 테스트 엔드포인트 전용) */
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    // ──────────────────────────────────────────────────────────────────────
    // SSE 구독 (테스트·하위 호환용)
    // ──────────────────────────────────────────────────────────────────────

    public SseEmitter subscribe(Long userId) {
        SseEmitter existing = emitters.remove(userId);
        if (existing != null) existing.complete();

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        emitters.put(userId, emitter);

        emitter.onCompletion(() -> emitters.remove(userId, emitter));
        emitter.onTimeout(() -> emitters.remove(userId, emitter));
        emitter.onError(e -> emitters.remove(userId, emitter));

        try {
            emitter.send(SseEmitter.event().name("connect").data("connected"));
            List<Notification> unread = notificationRepository.findUnreadByUserId(userId);
            for (Notification n : unread) {
                if (n.getType() == NotificationType.CHAT_MESSAGE) {
                    continue;
                }
                emitter.send(SseEmitter.event().name("notification").data(NotificationResponse.of(n)));
            }
        } catch (IOException e) {
            emitters.remove(userId, emitter);
            log.warn("SSE 초기 전송 실패: userId={}", userId, e);
        }
        return emitter;
    }

    // ──────────────────────────────────────────────────────────────────────
    // 알림 전송 (DB 저장 + WebSocket push)
    // ──────────────────────────────────────────────────────────────────────

    /**
     * 알림을 저장하고 WebSocket으로 즉시 전송한다.
     * 사용자가 미연결 상태이면 DB 저장만 수행한다 (다음 연결 시 미읽음 목록 제공).
     */
    @Transactional
    public void send(User user, NotificationType type, Long chatRoomId, String message) {
        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .chatRoomId(chatRoomId)
                .message(message)
                .build();
        notificationRepository.save(notification);

        // WebSocket push (연결 중인 사용자에게만 전달 — 미연결 시 예외 없이 무시됨)
        try {
            messagingTemplate.convertAndSendToUser(
                    user.getId().toString(),
                    "/queue/notification",
                    NotificationResponse.of(notification));
        } catch (Exception e) {
            log.warn("WebSocket 알림 push 실패: userId={}", user.getId(), e);
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // 알림 조회·읽음 처리
    // ──────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<NotificationResponse> getUnread(Long userId) {
        return notificationRepository.findUnreadByUserId(userId).stream()
                .filter(n -> n.getType() != NotificationType.CHAT_MESSAGE)
                .map(NotificationResponse::of)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getAll(Long userId) {
        return notificationRepository.findTop50ByUserId(userId).stream()
                .filter(n -> n.getType() != NotificationType.CHAT_MESSAGE)
                .map(NotificationResponse::of)
                .toList();
    }

    @Transactional
    public void markAllRead(Long userId) {
        notificationRepository.markAllReadByUserId(userId);
    }

    @Transactional
    public void markRead(Long userId, Long notificationId) {
        int updated = notificationRepository.markReadByIdAndUserId(notificationId, userId);
        if (updated == 0) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다.");
        }
    }
}
