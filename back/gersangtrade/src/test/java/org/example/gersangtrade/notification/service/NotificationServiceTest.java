package org.example.gersangtrade.notification.service;

import org.example.gersangtrade.domain.notification.Notification;
import org.example.gersangtrade.domain.notification.enums.NotificationType;
import org.example.gersangtrade.domain.user.User;
import org.example.gersangtrade.domain.user.enums.Role;
import org.example.gersangtrade.domain.user.enums.UserStatus;
import org.example.gersangtrade.notification.dto.response.NotificationResponse;
import org.example.gersangtrade.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * NotificationService 단위 테스트.
 * SSE emitter push 동작과 알림 저장·조회·읽음 처리 로직을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    private User user;

    @BeforeEach
    void setUp() {
        user = spy(User.builder()
                .oauthProvider("google").oauthId("google-user")
                .nickname("테스트유저").email("user@test.com")
                .role(Role.USER).status(UserStatus.ACTIVE)
                .build());
        doReturn(1L).when(user).getId();
    }

    // ── send() 테스트 ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("send_SSE연결없음_알림저장만됨_예외없음")
    void send_SSE연결없음_알림저장만됨() {
        // SSE emitter 맵이 비어있을 때
        notificationService.send(user, NotificationType.CHAT_OPENED, 100L, "채팅 요청");

        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    @DisplayName("send_SSE연결있음_알림저장후push됨")
    void send_SSE연결있음_알림저장후push됨() throws IOException {
        // emitter 맵에 Mock emitter 주입
        SseEmitter mockEmitter = mock(SseEmitter.class);
        Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();
        emitters.put(1L, mockEmitter);
        ReflectionTestUtils.setField(notificationService, "emitters", emitters);

        notificationService.send(user, NotificationType.CHAT_OPENED, 100L, "채팅 요청");

        // DB 저장 확인
        verify(notificationRepository).save(any(Notification.class));
        // SSE push 확인
        verify(mockEmitter).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    @DisplayName("send_SSE_IOException발생_emitter제거_예외미전파")
    void send_SSE_IOException발생_emitter제거_예외미전파() throws IOException {
        // push 시 IOException 발생하는 Mock emitter
        SseEmitter mockEmitter = mock(SseEmitter.class);
        doThrow(new IOException("연결 끊김")).when(mockEmitter).send(any(SseEmitter.SseEventBuilder.class));

        Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();
        emitters.put(1L, mockEmitter);
        ReflectionTestUtils.setField(notificationService, "emitters", emitters);

        // IOException이 외부로 전파되지 않아야 함
        assertThatNoException().isThrownBy(
                () -> notificationService.send(user, NotificationType.CHAT_OPENED, 100L, "채팅 요청"));

        // emitter가 맵에서 제거되어야 함
        assertThat(emitters).doesNotContainKey(1L);
        // DB 저장은 정상 완료되어야 함
        verify(notificationRepository).save(any(Notification.class));
    }

    // ── getUnread() 테스트 ────────────────────────────────────────────────────

    @Test
    @DisplayName("getUnread_미읽음알림목록_DTO로매핑반환")
    void getUnread_미읽음알림목록_DTO로매핑반환() {
        Notification n = mockNotification(1L, NotificationType.CHAT_OPENED, 100L, "채팅 요청", false);
        when(notificationRepository.findUnreadByUserId(1L)).thenReturn(List.of(n));

        List<NotificationResponse> result = notificationService.getUnread(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).type()).isEqualTo(NotificationType.CHAT_OPENED);
        assertThat(result.get(0).chatRoomId()).isEqualTo(100L);
        assertThat(result.get(0).read()).isFalse();
    }

    @Test
    @DisplayName("getUnread_미읽음없음_빈목록반환")
    void getUnread_미읽음없음_빈목록반환() {
        when(notificationRepository.findUnreadByUserId(1L)).thenReturn(List.of());

        List<NotificationResponse> result = notificationService.getUnread(1L);

        assertThat(result).isEmpty();
    }

    // ── getAll() 테스트 ──────────────────────────────────────────────────────

    @Test
    @DisplayName("getAll_전체알림목록_최대50건반환")
    void getAll_전체알림목록_최대50건반환() {
        Notification n1 = mockNotification(1L, NotificationType.CHAT_OPENED, 100L, "채팅 요청", false);
        Notification n2 = mockNotification(2L, NotificationType.TRADE_COMPLETED, 100L, "거래 완료", true);
        when(notificationRepository.findTop50ByUserId(1L)).thenReturn(List.of(n1, n2));

        List<NotificationResponse> result = notificationService.getAll(1L);

        assertThat(result).hasSize(2);
        assertThat(result.get(1).read()).isTrue(); // 두 번째는 읽음
    }

    // ── markAllRead() 테스트 ─────────────────────────────────────────────────

    @Test
    @DisplayName("markAllRead_미읽음알림전체읽음처리")
    void markAllRead_미읽음알림전체읽음처리() {
        notificationService.markAllRead(1L);

        verify(notificationRepository).markAllReadByUserId(1L);
    }

    // ── subscribe() 테스트 ────────────────────────────────────────────────────

    @Test
    @DisplayName("subscribe_미읽음알림조회호출_SseEmitter반환")
    void subscribe_미읽음알림조회호출_SseEmitter반환() {
        when(notificationRepository.findUnreadByUserId(1L)).thenReturn(List.of());

        SseEmitter emitter = notificationService.subscribe(1L);

        // SseEmitter가 반환되어야 함
        assertThat(emitter).isNotNull();
        // 미읽음 알림 조회 호출 확인
        verify(notificationRepository).findUnreadByUserId(1L);
    }

    @Test
    @DisplayName("subscribe_기존연결있으면_기존emitter제거후새연결반환")
    void subscribe_기존연결있으면_기존emitter제거후새연결반환() throws IOException {
        // 기존 emitter 주입
        SseEmitter oldEmitter = mock(SseEmitter.class);
        Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();
        emitters.put(1L, oldEmitter);
        ReflectionTestUtils.setField(notificationService, "emitters", emitters);

        when(notificationRepository.findUnreadByUserId(1L)).thenReturn(List.of());

        SseEmitter newEmitter = notificationService.subscribe(1L);

        // 기존 emitter 완료 처리
        verify(oldEmitter).complete();
        // 새 emitter 반환
        assertThat(newEmitter).isNotNull().isNotSameAs(oldEmitter);
    }

    // ── 헬퍼 ─────────────────────────────────────────────────────────────────

    private Notification mockNotification(Long id, NotificationType type, Long chatRoomId,
                                          String message, boolean read) {
        Notification n = mock(Notification.class);
        when(n.getId()).thenReturn(id);
        when(n.getType()).thenReturn(type);
        when(n.getChatRoomId()).thenReturn(chatRoomId);
        when(n.getMessage()).thenReturn(message);
        when(n.isRead()).thenReturn(read);
        when(n.getCreatedAt()).thenReturn(LocalDateTime.now());
        return n;
    }
}
