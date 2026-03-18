package org.example.gersangtrade.listing.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 번들 구성 라인(개별 아이템) 등록 요청.
 *
 * @param itemId          아이템 ID (Item.id)
 * @param quantity        수량 (재료: 1 이상, 장비: 보통 1)
 * @param sortOrder       번들 내 표시 순서
 * @param equipmentDetail 장비 상세 정보 (재료 아이템이면 null)
 */
public record BundleLineCreateRequest(
        @NotNull(message = "아이템 ID는 필수입니다.")
        Long itemId,

        @NotNull(message = "수량은 필수입니다.")
        @Min(value = 1, message = "수량은 1 이상이어야 합니다.")
        Integer quantity,

        @NotNull(message = "표시 순서는 필수입니다.")
        @Min(value = 0, message = "표시 순서는 0 이상이어야 합니다.")
        Integer sortOrder,

        @Valid
        EquipmentDetailRequest equipmentDetail  // 재료면 null
) {
    /** 장비 라인인지 여부 */
    public boolean isEquipment() {
        return equipmentDetail != null;
    }
}
