package org.example.gersangtrade.domain.catalog.enums;

/**
 * 정령 등급 — 하급·중급·상급·최상급·전설 5단계.
 */
public enum SpiritGrade {
    LOWER("하급"),
    MIDDLE("중급"),
    UPPER("상급"),
    HIGHEST("최상급"),
    LEGEND("전설");

    private final String displayName;

    SpiritGrade(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }
}
