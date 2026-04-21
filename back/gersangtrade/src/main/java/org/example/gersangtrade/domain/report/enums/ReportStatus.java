package org.example.gersangtrade.domain.report.enums;

/**
 * 신고 처리 상태.
 */
public enum ReportStatus {
    PENDING,    // 처리 대기 중
    REVIEWING,  // 관리자 검토 중 (선점 처리 — 다중 관리자 동시 처리 방지)
    PROCESSED,  // 관리자 처리 완료 (경고/숨김/차단 등 조치됨)
    DISMISSED   // 기각 (오탐 또는 허위 신고)
}
