package org.example.gersangtrade.calculator.overlay;

/** 용병 시나리오 편성 방식 */
public enum MercenaryMode {
    /** 기존 멤버(affectedMemberId)와 교체 */
    REPLACE,
    /** 빈 슬롯에 추가 (가상 멤버 — 음수 memberId) */
    APPEND
}
