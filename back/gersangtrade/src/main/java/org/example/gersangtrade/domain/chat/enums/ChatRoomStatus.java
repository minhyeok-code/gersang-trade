package org.example.gersangtrade.domain.chat.enums;

/**
 * 채팅방 진행 상태.
 *
 * 상태 전이:
 *   OPEN → POSTER_CONFIRMED → COMPLETED
 *   OPEN | POSTER_CONFIRMED → CLOSED (취소 또는 다른 거래 완료로 종료)
 */
public enum ChatRoomStatus {

    /** 채팅 진행 중 — 흥정하기/거래신청 직후 생성 상태 */
    OPEN,

    /** 게시자 거래완료 완료 대기 — 게시자가 먼저 확인, 상대방 확인 대기 중 */
    POSTER_CONFIRMED,

    /** 거래 확정 완료 — 양측 모두 확인, TradeConfirmed 생성됨 */
    COMPLETED,

    /** 채팅방 종료 — 취소 또는 다른 사용자와 거래 완료로 강제 종료 */
    CLOSED
}
