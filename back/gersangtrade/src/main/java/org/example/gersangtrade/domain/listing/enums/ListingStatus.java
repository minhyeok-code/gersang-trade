package org.example.gersangtrade.domain.listing.enums;

/**
 * 거래 등록글 상태 흐름.
 * ACTIVE → IN_TRADE → SOLD | CANCELLED
 *
 * ACTIVE: 거래 모집 중, IN_TRADE: 거래 진행 중(신청 수락됨),
 * SOLD: 거래 완료, CANCELLED: 판매자 또는 시스템에 의해 취소됨
 */
public enum ListingStatus {
    ACTIVE,      // 모집 중
    IN_TRADE,    // 거래 진행 중
    SOLD,        // 거래 완료
    CANCELLED    // 취소됨
}
