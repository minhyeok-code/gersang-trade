package org.example.gersangtrade.domain.catalog.enums;

/**
 * 용병 속성(자연 원소).
 * NONE은 무속성 용병.
 */
public enum Nature {
    FIRE("화"),
    WATER("수"),
    THUNDER("뇌"),
    AIR("풍"),
    EARTH("토"),
    NONE("-");

    /** 화면에 표시할 한국어 명칭 */
    private final String displayName;

    Nature(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
