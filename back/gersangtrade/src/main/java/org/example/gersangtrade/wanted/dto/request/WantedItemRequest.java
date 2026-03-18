package org.example.gersangtrade.wanted.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 구매 희망 아이템 단건 요청.
 *
 * @param itemId           아이템 ID
 * @param quantity         구매 희망 수량 (1 이상)
 * @param sortOrder        목록 내 표시 순서 (0 이상)
 * @param equipmentCondition 장비 조건 (재료 아이템이면 null)
 */
public record WantedItemRequest(
        @NotNull(message = "아이템 ID는 필수입니다.")
        Long itemId,

        @NotNull(message = "수량은 필수입니다.")
        @Min(value = 1, message = "수량은 1 이상이어야 합니다.")
        Integer quantity,

        @NotNull(message = "표시 순서는 필수입니다.")
        @Min(value = 0, message = "표시 순서는 0 이상이어야 합니다.")
        Integer sortOrder,

        @Valid
        WantedEquipmentConditionRequest equipmentCondition  // 재료면 null
) {
    /** 장비 조건이 있는 아이템인지 여부 */
    public boolean isEquipment() {
        return equipmentCondition != null;
    }
}
