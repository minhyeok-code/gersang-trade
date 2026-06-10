package org.example.gersangtrade.calculator.overlay;

import org.example.gersangtrade.listing.dto.request.EquipmentDetailRequest;

/**
 * 시나리오 장비 라인 — 거래 등록 BundleLineCreateRequest와 동일 구조.
 * 장비 단품·세트 시나리오에서 피스별 아이템·주술·강화 정보를 담는다.
 */
public record ScenarioLine(
        Long itemId,
        int quantity,
        int sortOrder,
        EquipmentDetailRequest equipmentDetail
) {}
