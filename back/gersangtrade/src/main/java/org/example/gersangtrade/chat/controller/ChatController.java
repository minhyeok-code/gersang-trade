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
 *   POST   /api/chat-rooms/{chatRoomId}/poster-confirm  — 게시자 거래완료 요청 (1단계)
 *   POST   /api/chat-rooms/{chatRoomId}/counterparty-confirm — 상대방 거래완료 확인 (2단계)
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
     * 게시자가 거래완료를 요청한다 (1단계).
     * finalPrice가 없으면 게시물 원래 가격으로 처리된다.
     */
    @PostMapping("/{chatRoomId}/poster-confirm")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ChatRoomSummaryResponse> posterConfirm(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long chatRoomId,
            @Valid @RequestBody PosterConfirmRequest request) {
        return ResponseEntity.ok(chatService.posterConfirm(userId, chatRoomId, request));
    }

    /**
     * 상대방이 거래완료를 최종 확인한다 (2단계).
     * 이 호출로 TradeConfirmed가 생성되고 EXP·거래평가가 처리된다.
     */
    @PostMapping("/{chatRoomId}/counterparty-confirm")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ChatRoomSummaryResponse> counterpartyConfirm(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long chatRoomId) {
        return ResponseEntity.ok(chatService.counterpartyConfirm(userId, chatRoomId));
    }
}
