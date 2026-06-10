package org.example.gersangtrade.home.dto;

import org.example.gersangtrade.domain.wanted.WantedListing;

import java.time.LocalDateTime;

/**
 * 구매 희망 등록글 요약 스냅샷 DTO (관심 아이템 시세 구매 탭용).
 */
public record WantedSnapshot(
        Long wantedId,
        Long offeredPrice,
        LocalDateTime createdAt
) {
    public static WantedSnapshot from(WantedListing listing) {
        return new WantedSnapshot(listing.getId(), listing.getOfferedPrice(), listing.getCreatedAt());
    }
}
