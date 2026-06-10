package org.example.gersangtrade.crawler.config;

import org.example.gersangtrade.domain.catalog.enums.EquipmentKind;
import org.example.gersangtrade.domain.catalog.enums.EquipmentSlot;

/**
 * 전용장비 크롤링 정책.
 * 카테고리별로 restriction을 적용할 슬롯 범위가 다르다.
 */
public enum ExclusiveEquipPolicy {

    /** 사천왕·각성 사천왕·명왕·각성 명왕 — 무기·수호부·무신만 전용 */
    HEAVENLY_KING_AND_MYEONGWANG,

    /** 전설장수 — 섹션 내 아이템 전부 전용 */
    LEGENDARY_GENERAL,

    /** 주인공 변신무기(인형) — PROTAGONIST 카테고리 전체 전용 */
    PROTAGONIST_DOLL,

    /** 주인공 국가별 전용장비 — 해당 주인공 1명 전용 */
    PROTAGONIST_NATIONAL;

    /**
     * restriction을 적용할지 판별한다.
     *
     * @param slot 장비 슬롯
     * @param kind 장비 종류
     * @return true이면 ItemMercenaryRestriction UPSERT 대상
     */
    public boolean shouldApplyRestriction(EquipmentSlot slot, EquipmentKind kind) {
        return switch (this) {
            case HEAVENLY_KING_AND_MYEONGWANG ->
                    (kind == EquipmentKind.APPEARANCE && slot == EquipmentSlot.DIVINE)
                            || slot == EquipmentSlot.WEAPON
                            || slot == EquipmentSlot.TALISMAN;
            case LEGENDARY_GENERAL, PROTAGONIST_NATIONAL, PROTAGONIST_DOLL -> true;
        };
    }
}
