package org.example.gersangtrade.catalog.dto;

import org.example.gersangtrade.domain.catalog.enums.EquipmentKind;
import org.example.gersangtrade.domain.catalog.enums.EquipmentSlot;
import org.example.gersangtrade.domain.catalog.enums.ItemType;
import org.example.gersangtrade.domain.deck.enums.EquipSlot;

/**
 * 아이템 자동완성 검색 결과 DTO.
 * jOOQ 또는 QueryDSL 결과를 매핑하는 용도로 사용된다.
 *
 * @param id           아이템 ID
 * @param name         아이템 이름
 * @param type         MATERIAL | EQUIPMENT
 * @param equipmentKind 장비 종류 (null if MATERIAL)
 * @param slot         장비 슬롯 (null if MATERIAL)
 * @param setId        소속 세트 ID (null if 단품 장비 or MATERIAL)
 * @param setName      소속 세트명 (null if 단품 장비 or MATERIAL)
 * @param stackUnitName 재료 수량 단위 (null if EQUIPMENT)
 * @param imageUrl     아이템 이미지 URL
 * @param equipSlot    덱 착용 슬롯 (반지는 null일 수 있음)
 */
public record ItemSearchResult(
        Long id,
        String name,
        ItemType type,
        EquipmentKind equipmentKind,
        EquipmentSlot slot,
        Long setId,
        String setName,
        String stackUnitName,
        String imageUrl,
        EquipSlot equipSlot
) {
}
