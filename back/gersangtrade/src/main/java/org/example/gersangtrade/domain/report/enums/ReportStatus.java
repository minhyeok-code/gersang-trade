package org.example.gersangtrade.domain.report.enums;

/**
 * 신고 처리 상태.
 * PENDING: 미처리, PROCESSED: 처리 완료, DISMISSED: 기각
 */
public enum ReportStatus {
    PENDING,    // 처리 대기 중
    PROCESSED,  // 관리자 처리 완료
    DISMISSED   // 기각 (사유 없음 또는 허위 신고)
}
