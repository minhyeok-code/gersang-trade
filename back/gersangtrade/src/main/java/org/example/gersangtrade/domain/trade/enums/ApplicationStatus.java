package org.example.gersangtrade.domain.trade.enums;

/**
 * 거래 신청 상태.
 * PENDING: 대기 중, ACCEPTED: 판매자가 수락, REJECTED: 판매자가 거절, CANCELLED: 구매자가 취소
 */
public enum ApplicationStatus {
    PENDING,    // 신청 대기 중
    ACCEPTED,   // 판매자 수락
    REJECTED,   // 판매자 거절
    CANCELLED   // 구매자 취소
}
