package org.example.gersangtrade.domain.notification;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.gersangtrade.domain.notification.enums.NotificationType;
import org.example.gersangtrade.domain.user.User;

import java.time.LocalDateTime;

/**
 * 알림 엔티티.
 * SSE로 실시간 전송되며, 오프라인 사용자는 다음 접속 시 미읽음 목록으로 확인한다.
 * 알림 타입별 발생 시점은 NotificationType 참고.
 */
@Entity
@Table(name = "notifications",
        indexes = @Index(name = "idx_notifications_user_read", columnList = "user_id, is_read")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

    /** 알림 고유 식별자 (자동 증가) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 알림 수신 대상 사용자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 알림 유형 */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private NotificationType type;

    /**
     * 관련 채팅방 ID.
     * 채팅·거래 관련 알림일 때 설정. 그 외(신고 처리 등)는 null.
     */
    @Column(name = "chat_room_id")
    private Long chatRoomId;

    /** 사용자에게 표시할 알림 문구 */
    @Column(name = "message", nullable = false, length = 500)
    private String message;

    /** 읽음 여부 — 기본값 false */
    @Column(name = "is_read", nullable = false)
    private boolean read;

    /** 알림 생성 시각 */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Notification(User user, NotificationType type, Long chatRoomId, String message) {
        this.user = user;
        this.type = type;
        this.chatRoomId = chatRoomId;
        this.message = message;
        this.read = false;
        this.createdAt = LocalDateTime.now();
    }

    /** 알림 읽음 처리 */
    public void markAsRead() {
        this.read = true;
    }
}
