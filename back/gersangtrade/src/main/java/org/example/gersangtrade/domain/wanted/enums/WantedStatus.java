package org.example.gersangtrade.domain.wanted.enums;

/**
 * 구매 희망 등록글 상태.
 * OPEN: 구매자 모집 중 (초기 상태)
 * IN_TRADE: 거래 진행 중 (판매자와 매칭됨)
 * PURCHASED: 구매 완료
 * CANCELLED: 구매자 취소
 */
public enum WantedStatus {
    OPEN,
    IN_TRADE,
    PURCHASED,
    CANCELLED
}
