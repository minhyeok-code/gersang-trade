package org.example.gersangtrade.home.event;

/**
 * 거래 확정 완료 이벤트.
 * 트랜잭션 커밋 후 priceWatch 캐시 무효화를 위해 사용된다.
 */
public record TradeConfirmedEvent() {}
