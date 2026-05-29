package org.example.gersangtrade.chat.dto.response;

/**
 * SSE room_status 이벤트 페이로드.
 * 거래완료 확인 시 양쪽 참여자에게 실시간으로 전달한다.
 * my/partnerTradeConfirmed는 수신자(viewer) 기준이다.
 */
public record RoomStatusSseEvent(
        Long chatRoomId,
        String status,
        boolean myTradeConfirmed,
        boolean partnerTradeConfirmed
) {}
