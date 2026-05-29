package org.example.gersangtrade.domain.notification.enums;

/**
 * 알림 유형 — SSE로 실시간 전송되며, 오프라인 사용자는 DB에서 다음 접속 시 조회한다.
 */
public enum NotificationType {

    // ── 채팅 관련 ──────────────────────────────────────

    /** 흥정하기 또는 거래신청으로 채팅방이 생성됐을 때 → 게시자에게 전송 */
    CHAT_OPENED,

    /** 상대방이 채팅 메시지를 전송했을 때 — 알림 센터 미사용, 채팅방 미읽음 표시만 사용 */
    CHAT_MESSAGE,

    /** 게시자가 거래완료 버튼을 눌렀을 때 → 상대방(counterparty)에게 전송 */
    POSTER_CONFIRMED,

    // ── 거래 완료 관련 ────────────────────────────────

    /** 양측 모두 거래완료 확인 → 거래 양측 모두에게 전송 */
    TRADE_COMPLETED,

    /** 거래 완료 후 상대방 평가 요청 (3일 이내) → 거래 양측 모두에게 전송 */
    REVIEW_REQUESTED,

    /** 3일 만료 후 평가 결과 공개 → 거래 양측 모두에게 전송 */
    REVIEW_PUBLISHED,

    // ── 신고·관리 관련 ────────────────────────────────

    /** 채팅 메시지에서 현금거래 의심 키워드 자동 감지 → 관리자(ADMIN) 전체에게 전송 */
    CASH_TRADE_DETECTED,

    /** 사용자가 신고를 접수했을 때 → 관리자(ADMIN) 전체에게 전송 */
    REPORT_RECEIVED,

    /** 관리자가 신고를 처리 완료했을 때 → 신고자에게 전송 */
    REPORT_PROCESSED,

    /** 관리자가 경고 처리했을 때 → 신고 대상 사용자에게 전송 */
    USER_WARNED,

    /** 관리자가 차단 처리했을 때 → 차단된 사용자에게 전송 */
    USER_BLOCKED,

    /** 동일 두 사용자 간 7일 내 3건 이상 거래 탐지 → 관리자(ADMIN) 전체에게 전송 */
    ABUSE_SUSPECTED
}
