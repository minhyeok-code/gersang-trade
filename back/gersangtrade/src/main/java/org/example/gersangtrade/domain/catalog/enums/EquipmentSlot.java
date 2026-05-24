package org.example.gersangtrade.domain.catalog.enums;

/**
 * 장비 슬롯(착용 부위) 구분.
 * 일반 장비(EquipmentKind=NORMAL) 8종 + 속성장비 외변(EquipmentKind=APPEARANCE) 9종.
 * 반지는 세트당 2개 착용 — EquipmentSetPiece.pieceCount=2 로 구분한다.
 */
public enum EquipmentSlot {
    // 일반 장비 (EquipmentKind=NORMAL)
    WEAPON,    // 무기    → EquipSlot.WEAPON
    HELMET,    // 투구    → EquipSlot.HELMET  (APPEARANCE이면 APP_HELMET)
    ARMOR,     // 갑옷    → EquipSlot.ARMOR   (APPEARANCE이면 APP_ARMOR)
    GLOVES,    // 장갑    → EquipSlot.GLOVES
    BELT,      // 허리띠  → EquipSlot.BELT
    SHOES,     // 신발    → EquipSlot.SHOES
    RING,      // 반지    → null (RING_1/RING_2 모두 가능)
    TALISMAN,  // 수호부  → EquipSlot.CHARM

    // 속성장비 외변 (EquipmentKind=APPEARANCE)
    // WEAPON/HELMET/ARMOR은 APPEARANCE kind로 재사용 (APP_WEAPON/APP_HELMET/APP_ARMOR 대응)
    ACCESSORY, // 장신구  → APP_SPIRIT
    DIVINE,    // 무신    → APP_WAR_GOD
    BRACELET,  // 팔찌    → APP_BRACELET
    LEGGING,   // 각반    → APP_GREAVES
    EARRING,   // 귀걸이  → APP_EARRING
    NECKLACE,  // 목걸이  → APP_NECKLACE
    ORB,       // 보주    — 덱 슬롯 없음
    WING,      // 날개    — 덱 슬롯 없음
    TITLE      // 칭호    — 덱 슬롯 없음
}
