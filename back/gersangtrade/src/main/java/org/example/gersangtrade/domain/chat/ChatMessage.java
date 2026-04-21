package org.example.gersangtrade.domain.chat;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.gersangtrade.domain.chat.enums.ChatMessageType;
import org.example.gersangtrade.domain.user.User;

import java.time.LocalDateTime;

/**
 * 채팅 메시지 엔티티.
 *
 * 보존 정책:
 *   - archivedAt IS NULL: 사용자 열람 가능 (6개월 이내)
 *   - archivedAt non-null: 6개월 경과 → 사용자 화면 숨김, 관리자는 계속 조회 가능
 *   - archivedAt이 2년 경과한 메시지는 배치 Job이 실제 삭제
 *
 * 자동 감지:
 *   - flagged=true: 서버가 금지 키워드/패턴을 감지한 메시지
 *   - hidden=true: 관리자가 숨김 처리한 메시지 (사용자 화면에 "[삭제된 메시지입니다]" 표시)
 */
@Entity
@Table(name = "chat_messages",
        indexes = {
                @Index(name = "idx_chat_messages_room_sent", columnList = "chat_room_id, sent_at"),
                @Index(name = "idx_chat_messages_archived", columnList = "archived_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage {

    /** 메시지 고유 식별자 (자동 증가) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 메시지가 속한 채팅방 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    /**
     * 메시지 발신자.
     * SYSTEM 메시지인 경우 null.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private User sender;

    /** 메시지 내용 (최대 1,000자) */
    @Column(name = "content", nullable = false, length = 1000)
    private String content;

    /** 메시지 유형 (TEXT: 사용자 입력 / SYSTEM: 서버 자동 생성) */
    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 10)
    private ChatMessageType messageType;

    /**
     * 현금거래 의심 감지 여부.
     * true: KeywordDetectionService가 금지 패턴을 감지한 메시지.
     * 메시지 자체는 정상 전송되며(Soft 방식), 자동 Report가 별도 생성된다.
     */
    @Column(name = "flagged", nullable = false)
    private boolean flagged;

    /** 감지된 패턴 목록 — flagged=true일 때만 값이 존재 */
    @Column(name = "flag_reason", length = 500)
    private String flagReason;

    /**
     * 관리자 숨김 처리 여부.
     * true이면 사용자 화면에 "[삭제된 메시지입니다]"로 표시된다.
     * 관리자는 원문을 계속 조회할 수 있다.
     */
    @Column(name = "hidden", nullable = false)
    private boolean hidden;

    /**
     * 사용자 열람 만료 시각.
     * null: 사용자 열람 가능.
     * non-null: 6개월 경과 후 배치 Job이 설정 — 사용자 화면에서 숨김 처리.
     * 관리자는 archived 여부와 관계없이 전체 조회 가능.
     */
    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    /** 메시지 발송 시각 */
    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    @Builder
    public ChatMessage(ChatRoom chatRoom, User sender, String content,
                       ChatMessageType messageType, boolean flagged, String flagReason) {
        this.chatRoom = chatRoom;
        this.sender = sender;
        this.content = content;
        this.messageType = messageType;
        this.flagged = flagged;
        this.flagReason = flagReason;
        this.hidden = false;
        this.sentAt = LocalDateTime.now();
    }

    /** 관리자 숨김 처리 */
    public void hide() {
        this.hidden = true;
    }

    /** 관리자 숨김 해제 (오탐 DISMISS 시 복원) */
    public void unhide() {
        this.hidden = false;
    }

    /** 배치 Job이 6개월 경과 메시지를 아카이브 처리 */
    public void archive() {
        this.archivedAt = LocalDateTime.now();
    }
}
