package org.example.gersangtrade.calculator.dto.response;

/**
 * 가성비 평가 기록의 기준 덱 일치 여부.
 */
public enum EvaluationDeckStatus {
    /** baseline과 현재 덱 구성이 동일 */
    CURRENT,
    /** 덱이 변경되어 재평가 권장 */
    STALE,
    /** 기준 덱이 삭제됨 */
    DECK_DELETED,
    /** baseline 스냅샷 없음(구 기록) */
    UNKNOWN
}
