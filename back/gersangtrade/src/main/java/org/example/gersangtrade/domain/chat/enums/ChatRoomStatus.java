package org.example.gersangtrade.domain.chat.enums;

/**
 * 채팅방 진행 상태.
 *
 * 상태 전이:
 *   OPEN → AWAITING_PARTNER(한쪽만 확인) → COMPLETED(양측 확인)
 *   OPEN | AWAITING_PARTNER → CLOSED (취소 또는 다른 거래 완료로 종료)
 */
public enum ChatRoomStatus {

    /** 채팅 진행 중 — 흥정하기/거래신청 직후 생성 상태 */
    OPEN,

    /** 한쪽만 거래완료 확인 — 상대방 확인 대기 중 (게시자·상대방 누구든 먼저 가능) */
    AWAITING_PARTNER,

    /** 거래 확정 완료 — 양측 모두 확인, TradeConfirmed 생성됨 */
    COMPLETED,

    /** 채팅방 종료 — 취소 또는 다른 사용자와 거래 완료로 강제 종료 */
    CLOSED
}
