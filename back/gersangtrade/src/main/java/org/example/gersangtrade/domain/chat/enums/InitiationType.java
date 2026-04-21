package org.example.gersangtrade.domain.chat.enums;

/**
 * 채팅방 개설 방식 — 흥정하기(협상 목적) vs 거래신청(게시 가격 그대로 거래 희망).
 * 채팅방 생성 시 자동 시스템 메시지 내용과 알림 문구가 달라진다.
 */
public enum InitiationType {

    /** 흥정하기 — 가격·조건 협의를 원하는 경우 */
    NEGOTIATE,

    /** 거래신청 — 게시된 가격으로 즉시 거래를 희망하는 경우 */
    APPLY
}
