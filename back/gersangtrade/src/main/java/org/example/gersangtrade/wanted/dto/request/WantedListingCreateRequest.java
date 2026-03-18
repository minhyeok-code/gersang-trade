package org.example.gersangtrade.wanted.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 구매 희망 등록글 등록 요청.
 * 구매자가 원하는 아이템 조건과 제시 가격을 함께 등록한다.
 *
 * @param server       거래 대상 서버명
 * @param offeredPrice 구매자 제시 가격 (1 이상)
 * @param note         요청 메모 또는 연락처 (선택)
 * @param items        구매 희망 아이템 목록 (1개 이상 필수)
 */
public record WantedListingCreateRequest(
        @NotBlank(message = "서버명은 필수입니다.")
        @Size(max = 30, message = "서버명은 30자 이하이어야 합니다.")
        String server,

        @NotNull(message = "제시 가격은 필수입니다.")
        @Min(value = 1, message = "제시 가격은 1 이상이어야 합니다.")
        Long offeredPrice,

        @Size(max = 500, message = "메모는 500자 이하이어야 합니다.")
        String note,

        @NotEmpty(message = "구매 희망 아이템은 1개 이상이어야 합니다.")
        @Valid
        List<WantedItemRequest> items
) {
}
