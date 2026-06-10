package org.example.gersangtrade.home.dto;

import org.example.gersangtrade.domain.listing.TradeListing;

import java.time.LocalDateTime;

/**
 * 판매 등록글 요약 스냅샷 DTO (관심 아이템 시세 판매 탭용).
 */
public record ListingSnapshot(
        Long listingId,
        Long price,
        LocalDateTime createdAt
) {
    public static ListingSnapshot from(TradeListing listing) {
        return new ListingSnapshot(listing.getId(), listing.getPrice(), listing.getCreatedAt());
    }
}
