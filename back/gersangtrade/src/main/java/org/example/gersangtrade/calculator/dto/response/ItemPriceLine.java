package org.example.gersangtrade.calculator.dto.response;

/**
 * 세트 가격 breakdown의 피스별 가격 항목.
 * 세트 피스 합산 시 각 피스의 가격과 출처를 기록한다.
 */
public record ItemPriceLine(
        Long itemId,
        Long price,         // null이면 해당 피스 가격 미확인
        PriceSource source
) {}
