package org.example.gersangtrade.listing.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.example.gersangtrade.domain.listing.enums.BundleType;

import java.util.List;

/**
 * 번들(판매 단위) 등록 요청.
 * 하나의 TradeListing은 하나 이상의 번들을 가질 수 있다.
 *
 * @param bundleType    번들 유형 — MATERIAL_BUNDLE | EQUIPMENT_SINGLE | EQUIPMENT_SET
 * @param titleOverride 판매자가 직접 입력한 번들 제목 (null이면 시스템 자동 생성)
 * @param lines         번들 구성 라인 목록 (1개 이상 필수)
 */
public record BundleCreateRequest(
        @NotNull(message = "번들 유형은 필수입니다.")
        BundleType bundleType,

        String titleOverride,

        @NotEmpty(message = "번들 라인은 1개 이상이어야 합니다.")
        @Valid
        List<BundleLineCreateRequest> lines
) {
}
