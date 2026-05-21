package org.example.gersangtrade.domain.deck.enums;

/**
 * 덱 장비 슬롯 통합 enum.
 * 일반 슬롯(8종 9슬롯) + 외변 슬롯(9종 9슬롯) = 총 18슬롯.
 * 반지는 2슬롯(RING_1 / RING_2) — EquipmentSlot.RING 아이템은 어느 쪽에나 착용 가능.
 * 외변 슬롯은 APP_ 접두사로 구분하며 일반 슬롯과 동시 착용 가능.
 */
public enum EquipSlot {

    // ── 일반 슬롯 (8종 9슬롯) ─────────────────────────────
    HELMET,   // 투구
    ARMOR,    // 갑옷
    WEAPON,   // 무기
    SHOES,    // 신발
    GLOVES,   // 장갑
    BELT,     // 요대
    CHARM,    // 부적
    RING_1,   // 반지1
    RING_2,   // 반지2

    // ── 외변 슬롯 (9종 9슬롯) ─────────────────────────────
    APP_SPIRIT,    // 기운
    APP_HELMET,    // 투구 (외변)
    APP_ARMOR,     // 갑옷 (외변)
    APP_WEAPON,    // 무기 (외변)
    APP_WAR_GOD,   // 무신
    APP_EARRING,   // 귀걸이
    APP_NECKLACE,  // 목걸이
    APP_BRACELET,  // 팔찌
    APP_GREAVES    // 각반
}
