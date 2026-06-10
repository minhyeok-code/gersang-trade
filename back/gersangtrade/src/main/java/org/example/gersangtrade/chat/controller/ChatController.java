package org.example.gersangtrade.chat.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.chat.dto.request.ChatMessageSendRequest;
import org.example.gersangtrade.chat.dto.request.ChatRoomCreateRequest;
import org.example.gersangtrade.chat.dto.request.PosterConfirmRequest;
import org.example.gersangtrade.chat.dto.response.ChatMessageResponse;
import org.example.gersangtrade.chat.dto.response.ChatRoomDetailResponse;
import org.example.gersangtrade.chat.dto.response.ChatRoomSummaryResponse;
import org.example.gersangtrade.chat.service.ChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 채팅 컨트롤러.
 *
 * 엔드포인트 목록:
 *   POST   /api/chat-rooms                              — 채팅방 생성
 *   GET    /api/chat-rooms                              — 내 채팅방 목록
 *   GET    /api/chat-rooms/{chatRoomId}                 — 채팅방 상세 + 메시지 목록
 *   POST   /api/chat-rooms/{chatRoomId}/messages        — 메시지 전송
 *   POST   /api/chat-rooms/{chatRoomId}/trade-confirm      — 거래완료 확인 (양측 공통)
 *   POST   /api/chat-rooms/{chatRoomId}/poster-confirm     — (deprecated) trade-confirm 위임
 *   POST   /api/chat-rooms/{chatRoomId}/counterparty-confirm — (deprecated) trade-confirm 위임
 */
@RestController
@RequestMapping("/api/chat-rooms")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * 채팅방을 생성한다.
     * 흥정하기(NEGOTIATE) 또는 거래신청(APPLY) 버튼 클릭 시 호출된다.
     * 이미 OPEN 상태의 채팅방이 있으면 기존 채팅방을 반환한다.
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ChatRoomSummaryResponse> createChatRoom(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ChatRoomCreateRequest request) {
        return ResponseEntity.ok(chatService.createChatRoom(userId, request));
    }

    /**
     * 내가 참여 중인 채팅방 목록을 반환한다.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ChatRoomSummaryResponse>> getMyChatRooms(
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(chatService.getMyChatRooms(userId));
    }

    /**
     * 채팅방 상세 정보와 메시지 목록을 반환한다.
     * 채팅방 참여자만 조회 가능하다.
     */
    @GetMapping("/{chatRoomId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ChatRoomDetailResponse> getChatRoomDetail(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long chatRoomId) {
        return ResponseEntity.ok(chatService.getChatRoomDetail(userId, chatRoomId));
    }

    /**
     * 채팅방 읽음 처리 — 패널 열람 중 실시간 메시지 수신 시 호출.
     */
    @PostMapping("/{chatRoomId}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> markChatRoomRead(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long chatRoomId) {
        chatService.markChatRoomRead(userId, chatRoomId);
        return ResponseEntity.noContent().build();
    }

    /** 참여 중인 모든 채팅방 읽음 처리 */
    @PostMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> markAllChatRoomsRead(@AuthenticationPrincipal Long userId) {
        chatService.markAllChatRoomsRead(userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 채팅 메시지를 전송한다.
     */
    @PostMapping("/{chatRoomId}/messages")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ChatMessageResponse> sendMessage(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long chatRoomId,
            @Valid @RequestBody ChatMessageSendRequest request) {
        return ResponseEntity.ok(chatService.sendMessage(userId, chatRoomId, request));
    }

    /**
     * 거래완료 확인 — 게시자·상대방 모두 호출 가능.
     * 한쪽만 확인 시 상대방에게 알림, 양측 확인 시 거래 확정.
     */
    @PostMapping("/{chatRoomId}/trade-confirm")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ChatRoomSummaryResponse> confirmTrade(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long chatRoomId,
            @Valid @RequestBody(required = false) PosterConfirmRequest request) {
        PosterConfirmRequest body = request != null ? request : new PosterConfirmRequest(null);
        return ResponseEntity.ok(chatService.confirmTrade(userId, chatRoomId, body));
    }

    /** @deprecated {@link #confirmTrade} 사용 */
    @Deprecated
    @PostMapping("/{chatRoomId}/poster-confirm")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ChatRoomSummaryResponse> posterConfirm(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long chatRoomId,
            @Valid @RequestBody PosterConfirmRequest request) {
        return ResponseEntity.ok(chatService.confirmTrade(userId, chatRoomId, request));
    }

    /** @deprecated {@link #confirmTrade} 사용 */
    @Deprecated
    @PostMapping("/{chatRoomId}/counterparty-confirm")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ChatRoomSummaryResponse> counterpartyConfirm(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long chatRoomId) {
        return ResponseEntity.ok(chatService.confirmTrade(userId, chatRoomId, new PosterConfirmRequest(null)));
    }
}
