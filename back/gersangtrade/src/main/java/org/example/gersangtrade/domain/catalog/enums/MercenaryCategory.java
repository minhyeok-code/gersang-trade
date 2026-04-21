package org.example.gersangtrade.domain.catalog.enums;

/**
 * 용병 카테고리 — gerniverse 용병 상세 페이지의 배지 분류.
 */
public enum MercenaryCategory {
    PROTAGONIST("주인공"),
    FOUR_HEAVENLY_KINGS("사천왕"),
    FOUR_HEAVENLY_KINGS_AWAKENING("각성사천왕"),
    MYEONG_KING("명왕"),
    MYEONG_KING_AWAKENING("각성명왕"),
    LEGENDARY_GENERAL("전설장수"),
    DIVINE_BEAST("신수"),
    EVIL_BEAST("흉수"),
    EVIL_BEAST_AWAKENING("각성흉수"),
    HIRED_MONSTER("고용몬스터"),
    EVOLVE_MONSTER("전직몬스터"),
    SPIRIT_MONSTER("정령몬스터"),
    GENERAL_AWAKENING("각성장수"),
    MODIFIED_GENERAL("개조장수"),
    SECOND_GRADE_GENERAL("2차장수"),
    FIRST_GRADE_GENERAL("1차장수"),
    MERCENARY("용병");

    /** 화면에 표시할 한국어 명칭 */
    private final String displayName;

    MercenaryCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
