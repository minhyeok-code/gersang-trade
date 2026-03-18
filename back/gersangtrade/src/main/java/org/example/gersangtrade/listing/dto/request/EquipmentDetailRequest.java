package org.example.gersangtrade.listing.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.List;

/**
 * 번들 라인의 장비 상세 정보 등록 요청.
 * BundleLine이 장비 아이템인 경우에만 사용된다.
 *
 * @param enhanceLevel  강화 수치 (null 허용. 외변이면 5만 유효, 일반 장비면 실제 수치)
 * @param hasRitual     주술 적용 여부
 * @param rituals       적용된 주술 목록 (hasRitual=true이면 1개 이상 필수)
 */
public record EquipmentDetailRequest(
        @Min(value = 0, message = "강화 수치는 0 이상이어야 합니다.")
        @Max(value = 20, message = "강화 수치는 20 이하이어야 합니다.")
        Integer enhanceLevel,

        boolean hasRitual,

        @Valid
        List<RitualResultRequest> rituals
) {
    /** 주술 여부와 주술 목록의 일관성을 검사한다. */
    public boolean isRitualConsistent() {
        if (hasRitual) {
            return rituals != null && !rituals.isEmpty();
        }
        return true;
    }
}
