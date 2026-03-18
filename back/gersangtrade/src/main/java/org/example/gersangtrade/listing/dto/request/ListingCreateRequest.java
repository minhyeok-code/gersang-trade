package org.example.gersangtrade.listing.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 거래 등록글(TradeListing) 등록 요청.
 * 판매자가 아이템 거래를 올릴 때 사용하는 최상위 요청 DTO다.
 *
 * @param server  거래 대상 서버명
 * @param price   판매자 희망 가격 (게임 내 재화 기준, 1 이상)
 * @param note    판매자 메모 또는 연락처 (선택)
 * @param bundles 번들(판매 단위) 목록 (1개 이상 필수)
 */
public record ListingCreateRequest(
        @NotBlank(message = "서버명은 필수입니다.")
        String server,

        @NotNull(message = "가격은 필수입니다.")
        @Min(value = 1, message = "가격은 1 이상이어야 합니다.")
        Long price,

        String note,

        @NotEmpty(message = "번들은 1개 이상이어야 합니다.")
        @Valid
        List<BundleCreateRequest> bundles
) {
}
