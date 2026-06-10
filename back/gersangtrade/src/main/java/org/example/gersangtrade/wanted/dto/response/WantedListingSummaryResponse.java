package org.example.gersangtrade.wanted.dto.response;

import org.example.gersangtrade.domain.wanted.WantedItem;
import org.example.gersangtrade.domain.wanted.WantedListing;
import org.example.gersangtrade.domain.wanted.enums.WantedStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 구매 희망 등록글 목록 조회용 요약 응답 DTO.
 *
 * @param id          등록글 ID
 * @param buyerName   구매자 닉네임
 * @param server      거래 서버
 * @param status      등록글 상태
 * @param offeredPrice 제시 가격
 * @param displayTitle 세트 구성 표기 제목 (풀 XX 세트명 등). 산출 불가 시 null
 * @param itemNames   구매 희망 아이템명 목록 (미리보기용)
 * @param createdAt   등록 일시
 */
public record WantedListingSummaryResponse(
        Long id,
        String buyerName,
        String server,
        WantedStatus status,
        Long offeredPrice,
        String displayTitle,
        List<String> itemNames,
        LocalDateTime createdAt
) {
    public static WantedListingSummaryResponse from(WantedListing listing,
                                                     List<WantedItem> items,
                                                     String displayTitle) {
        List<String> names = items.stream()
                .map(item -> item.getItem().getName())
                .toList();
        return new WantedListingSummaryResponse(
                listing.getId(),
                listing.getBuyer().getNickname(),
                listing.getServer(),
                listing.getStatus(),
                listing.getOfferedPrice(),
                displayTitle,
                names,
                listing.getCreatedAt()
        );
    }
}
