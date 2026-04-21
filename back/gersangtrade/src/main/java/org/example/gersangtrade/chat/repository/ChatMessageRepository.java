package org.example.gersangtrade.chat.repository;

import org.example.gersangtrade.domain.chat.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 채팅 메시지 레포지토리.
 * 채팅방별 메시지 페이징 조회 및 아카이브 배치 쿼리에 사용된다.
 */
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /**
     * 채팅방 메시지 커서 기반 페이징 조회 (사용자용).
     * archivedAt IS NULL 조건으로 6개월 이내 메시지만 반환한다.
     */
    @Query("SELECT m FROM ChatMessage m WHERE m.chatRoom.id = :chatRoomId " +
           "AND m.archivedAt IS NULL ORDER BY m.sentAt DESC")
    Slice<ChatMessage> findActiveByRoomId(@Param("chatRoomId") Long chatRoomId, Pageable pageable);

    /**
     * 채팅방 메시지 전체 조회 (관리자용).
     * 아카이브·숨김 여부와 관계없이 모든 메시지를 반환한다.
     */
    @Query("SELECT m FROM ChatMessage m WHERE m.chatRoom.id = :chatRoomId ORDER BY m.sentAt ASC")
    List<ChatMessage> findAllByRoomIdForAdmin(@Param("chatRoomId") Long chatRoomId);

    /**
     * 아카이브 대상 메시지 조회 — 배치 Job용.
     * archivedAt IS NULL이고 sentAt이 cutoffDate 이전인 메시지를 반환한다.
     */
    @Query("SELECT m FROM ChatMessage m WHERE m.archivedAt IS NULL AND m.sentAt < :cutoffDate")
    List<ChatMessage> findUnarchived(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * 영구 삭제 대상 메시지 조회 — 배치 Job용.
     * archivedAt이 deleteBefore 이전인 메시지를 반환한다 (2년 경과).
     */
    @Query("SELECT m FROM ChatMessage m WHERE m.archivedAt IS NOT NULL AND m.archivedAt < :deleteBefore")
    List<ChatMessage> findExpiredArchived(@Param("deleteBefore") LocalDateTime deleteBefore);
}
