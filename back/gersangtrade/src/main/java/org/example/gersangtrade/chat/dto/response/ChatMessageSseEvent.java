package org.example.gersangtrade.chat.dto.response;

/**
 * SSE chat_message 이벤트 페이로드.
 * 메시지 수신 측에 실시간으로 전달한다.
 */
public record ChatMessageSseEvent(Long chatRoomId, ChatMessageResponse message) {}
