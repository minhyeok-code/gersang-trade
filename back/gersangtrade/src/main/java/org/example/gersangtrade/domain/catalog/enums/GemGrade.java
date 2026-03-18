package org.example.gersangtrade.domain.catalog.enums;

/**
 * 보석 등급.
 * 기본 보석에서 세공 → 강화 → 주술 순서로 업그레이드된다.
 * 빛나는은 별도 제작 루트(시간의가루 + 보석 조각)를 통해 생성된다.
 */
public enum GemGrade {
    /** 가공되지 않은 기본 상태 */
    기본,
    /** 세공된 상태 */
    세공됨,
    /** 강화된 상태 */
    강화됨,
    /** 빛나는 상태 — 별도 제작 루트 */
    빛나는,
    /** 주술이 적용된 상태 — ritual_id가 반드시 존재 */
    주술됨
}
