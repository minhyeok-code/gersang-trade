package org.example.gersangtrade.domain.catalog.enums;

/**
 * 아이템 능력치 종류.
 * 각 타입은 한국어 표시명(displayName)을 포함한다.
 * ELEMENT_VALUE: 속성값, ELEMENT_PIERCE: 속성깎, RESIST_PIERCE: 저항깎
 */
public enum StatType {
    ELEMENT_VALUE("속성값"),
    ELEMENT_PIERCE("속성깎"),
    RESIST_PIERCE("저항깎");

    /** 화면에 표시할 한국어 명칭 */
    private final String displayName;

    StatType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
