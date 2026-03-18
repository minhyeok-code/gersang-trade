package org.example.gersangtrade.domain.report.enums;

/**
 * 신고 사유 분류.
 * FRAUD: 사기, ABUSE: 욕설/비하, FAKE_LISTING: 허위 매물, CASH_TRADE: 현금 거래 유도(정책 위반), OTHER: 기타
 */
public enum ReportReason {
    FRAUD,        // 사기
    ABUSE,        // 욕설 / 비하
    FAKE_LISTING, // 허위 매물
    CASH_TRADE,   // 현금 거래 유도 (서비스 정책 위반)
    OTHER         // 기타
}
