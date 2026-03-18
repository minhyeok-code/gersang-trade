package org.example.gersangtrade.catalog.dto;

import org.example.gersangtrade.domain.catalog.enums.EquipmentKind;
import org.example.gersangtrade.domain.catalog.enums.EquipmentSlot;
import org.example.gersangtrade.domain.catalog.enums.ItemType;

/**
 * 아이템 자동완성 검색 결과 DTO.
 * jOOQ 또는 QueryDSL 결과를 매핑하는 용도로 사용된다.
 *
 * @param id           아이템 ID
 * @param name         아이템 이름
 * @param type         MATERIAL | EQUIPMENT
 * @param equipmentKind 장비 종류 (null if MATERIAL)
 * @param slot         장비 슬롯 (null if MATERIAL)
 * @param setName      소속 세트명 (null if 단품 장비 or MATERIAL)
 * @param stackUnitName 재료 수량 단위 (null if EQUIPMENT)
 */
public record ItemSearchResult(
        Long id,
        String name,
        ItemType type,
        EquipmentKind equipmentKind,
        EquipmentSlot slot,
        String setName,
        String stackUnitName
) {
}
