package org.example.gersangtrade.domain.chat.enums;

/**
 * 채팅 메시지 유형.
 */
public enum ChatMessageType {

    /** 사용자가 직접 입력한 일반 텍스트 메시지 */
    TEXT,

    /**
     * 서버가 자동 생성하는 시스템 안내 메시지.
     * 예: 거래완료 요청, 채팅방 종료, 거래 확정 알림 등.
     * senderId는 null로 저장된다.
     */
    SYSTEM
}
