package org.example.gersangtrade.listing.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 거래 등록글 수정 요청 DTO.
 * price·note만 수정 가능. 아이템 구성 변경은 불가.
 *
 * @param price 변경할 가격 (1 이상)
 * @param note  변경할 메모 (null이면 빈 값으로 초기화)
 */
public record ListingUpdateRequest(
        @NotNull @Min(1) Long price,
        String note
) {
}
