package org.example.gersangtrade.calculator.dto.response;

/** 가성비 평가에 사용된 가격의 출처 (장비 아이템 경로 전용). */
public enum PriceSource {

    /** 유저가 직접 입력한 가격 */
    USER_INPUT,

    /** TradeStatMonthly 시세에서 자동 조회한 가격 */
    TRADE_STAT,

    /** 일부 피스는 시세, 나머지는 유저 입력으로 합산한 세트 가격 */
    MIXED,

    /** 시세 없음 & 유저 입력도 없어 가격 산출 불가 */
    MISSING
}
