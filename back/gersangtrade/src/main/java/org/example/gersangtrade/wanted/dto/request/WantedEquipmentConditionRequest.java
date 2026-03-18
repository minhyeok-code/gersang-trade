package org.example.gersangtrade.wanted.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.List;

/**
 * 구매 희망 장비 조건 요청.
 * WantedItem이 장비 아이템인 경우에만 사용된다.
 *
 * @param minEnhanceLevel 최소 허용 강화 수치 (null이면 강화 수치 무관)
 * @param hasRitual       주술 적용 여부 조건
 * @param ritualConditions 원하는 주술 조건 목록 (hasRitual=true이면 1개 이상 필수)
 */
public record WantedEquipmentConditionRequest(
        @Min(value = 0, message = "최소 강화 수치는 0 이상이어야 합니다.")
        @Max(value = 20, message = "최소 강화 수치는 20 이하이어야 합니다.")
        Integer minEnhanceLevel,

        boolean hasRitual,

        @Valid
        List<WantedRitualConditionRequest> ritualConditions
) {
    /** 주술 여부와 주술 조건 목록의 일관성을 검사한다. */
    public boolean isRitualConsistent() {
        if (hasRitual) {
            return ritualConditions != null && !ritualConditions.isEmpty();
        }
        return true;
    }
}
