package org.example.gersangtrade.trade.dto.response;

import org.example.gersangtrade.domain.chat.enums.ListingType;

import java.time.LocalDateTime;

/**
 * 거래 확정 내역 응답 DTO.
 * 판매자·구매자 양쪽 모두의 거래내역 조회에 사용된다.
 */
public record TradeHistoryResponse(
        Long tradeId,
        /** "판매" 또는 "구매" — 조회 사용자 기준 역할 */
        String role,
        ListingType listingType,
        String displayName,
        Long confirmedPrice,
        String serverSnapshot,
        LocalDateTime confirmedAt
) {}
