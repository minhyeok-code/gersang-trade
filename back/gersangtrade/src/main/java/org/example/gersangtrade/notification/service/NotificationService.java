package org.example.gersangtrade.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gersangtrade.domain.notification.Notification;
import org.example.gersangtrade.domain.notification.enums.NotificationType;
import org.example.gersangtrade.domain.user.User;
import org.example.gersangtrade.notification.dto.response.NotificationResponse;
import org.example.gersangtrade.notification.repository.NotificationRepository;
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
 * SSE(Server-Sent Events) emitter를 사용자별로 관리하며,
 * 알림 발생 시 DB 저장과 실시간 push를 함께 처리한다.
 *
 * 오프라인 사용자: DB에만 저장 → 다음 SSE 연결 시점에 미읽음 목록으로 제공
 * 온라인 사용자: DB 저장 + SSE push (즉시 전달)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    /** SSE 연결 유지 시간 (30분) */
    private static final long SSE_TIMEOUT_MS = 30 * 60 * 1000L;

    private final NotificationRepository notificationRepository;

    /** userId → SseEmitter 연결 관리 (사용자당 최신 연결 1개 유지) */
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    // ──────────────────────────────────────────────────────────────────────
    // SSE 구독
    // ──────────────────────────────────────────────────────────────────────

    /**
     * SSE 구독 — 연결 즉시 미읽음 알림 전송 후 대기.
     * 같은 사용자의 기존 연결이 있으면 종료 후 새 연결로 교체한다.
     */
    public SseEmitter subscribe(Long userId) {
        // 기존 연결 종료
        SseEmitter existing = emitters.remove(userId);
        if (existing != null) {
            existing.complete();
        }

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        emitters.put(userId, emitter);

        // 연결 종료·만료·에러 시 emitter 제거
        emitter.onCompletion(() -> emitters.remove(userId, emitter));
        emitter.onTimeout(() -> emitters.remove(userId, emitter));
        emitter.onError(e -> emitters.remove(userId, emitter));

        try {
            // SSE 연결 유지를 위한 초기 이벤트
            emitter.send(SseEmitter.event().name("connect").data("connected"));

            // 미읽음 알림 즉시 전송
            List<Notification> unread = notificationRepository.findUnreadByUserId(userId);
            for (Notification n : unread) {
                emitter.send(SseEmitter.event()
                        .name("notification")
                        .data(NotificationResponse.of(n)));
            }
        } catch (IOException e) {
            emitters.remove(userId, emitter);
            log.warn("SSE 초기 전송 실패: userId={}", userId, e);
        }

        return emitter;
    }

    // ──────────────────────────────────────────────────────────────────────
    // 알림 전송 (DB 저장 + SSE push)
    // ──────────────────────────────────────────────────────────────────────

    /**
     * 알림을 저장하고 SSE로 즉시 전송한다.
     * ChatService 등 다른 서비스에서 알림 발생 시 호출한다.
     * 사용자가 SSE 미연결 상태이면 DB 저장만 수행한다.
     */
    @Transactional
    public void send(User user, NotificationType type, Long chatRoomId, String message) {
        // DB 저장
        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .chatRoomId(chatRoomId)
                .message(message)
                .build();
        notificationRepository.save(notification);

        // SSE push (연결 중인 경우만)
        SseEmitter emitter = emitters.get(user.getId());
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("notification")
                        .data(NotificationResponse.of(notification)));
            } catch (IOException e) {
                // push 실패 시 emitter 제거 (예외는 외부로 전파하지 않음)
                emitters.remove(user.getId(), emitter);
                log.warn("SSE push 실패, emitter 제거: userId={}", user.getId(), e);
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // 알림 조회·읽음 처리
    // ──────────────────────────────────────────────────────────────────────

    /**
     * 미읽음 알림 목록 조회 (최신순).
     */
    @Transactional(readOnly = true)
    public List<NotificationResponse> getUnread(Long userId) {
        return notificationRepository.findUnreadByUserId(userId).stream()
                .map(NotificationResponse::of)
                .toList();
    }

    /**
     * 알림 목록 전체 조회 (최신순, 최대 50건).
     */
    @Transactional(readOnly = true)
    public List<NotificationResponse> getAll(Long userId) {
        return notificationRepository.findTop50ByUserId(userId).stream()
                .map(NotificationResponse::of)
                .toList();
    }

    /**
     * 사용자의 미읽음 알림 전체 읽음 처리.
     */
    @Transactional
    public void markAllRead(Long userId) {
        notificationRepository.markAllReadByUserId(userId);
    }
}
