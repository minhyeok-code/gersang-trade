package org.example.gersangtrade.domain.catalog.enums;

/**
 * 장비 슬롯(착용 부위) 구분.
 * 일반 세트 장비 7종 + 속성장비(외변) 4종.
 * 반지는 세트당 2개 착용 — EquipmentSetPiece.pieceCount=2 로 구분한다.
 */
public enum EquipmentSlot {
    // 일반 세트 장비
    WEAPON,    // 무기
    HELMET,    // 투구
    ARMOR,     // 갑옷
    GLOVES,    // 장갑
    BELT,      // 허리띠
    SHOES,     // 신발
    RING,      // 반지 (세트 내 2개)

    // 속성장비 (외변) — EquipmentKind=APPEARANCE 항상
    ACCESSORY, // 장신구 (waixing.asp)
    DIVINE,    // 무신 (wushen.asp)
    BRACELET,  // 팔찌 (pal.asp)
    LEGGING,   // 각반 (gak.asp)
    ORB,       // 보주 (baozhu.asp)
    WING,      // 날개 (nal.asp)
    TITLE,     // 칭호 (chenghao.asp)

    // 보조 슬롯 — EquipmentKind=NORMAL
    TALISMAN   // 수호부 (hushenfu.asp)
}
