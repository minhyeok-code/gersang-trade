package org.example.gersangtrade.domain.catalog.enums;

/**
 * 용병 출신 국가.
 * NONE은 국가 구분이 없는 경우(예: 정령, 마수류).
 */
public enum Nation {
    JOSEON("조선"),
    CHINA("중국"),
    JAPAN("일본"),
    TAIWAN("대만"),
    INDIA("인도"),
    NONE("-");

    /** 화면에 표시할 한국어 명칭 */
    private final String displayName;

    Nation(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
