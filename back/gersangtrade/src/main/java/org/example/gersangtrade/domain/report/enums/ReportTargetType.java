package org.example.gersangtrade.domain.report.enums;

/**
 * 신고 대상 유형.
 * TRADE_APPLICATION은 ChatRoom 도입으로 대체되어 더 이상 사용하지 않는다.
 */
public enum ReportTargetType {
    USER,            // 사용자
    TRADE_LISTING,   // 판매 게시물
    WANTED_LISTING,  // 구매 게시물
    CHAT_MESSAGE     // 채팅 메시지 (자동 감지 및 사용자 신고 모두 포함)
}
