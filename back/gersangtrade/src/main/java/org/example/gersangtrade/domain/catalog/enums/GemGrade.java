package org.example.gersangtrade.domain.catalog.enums;

/**
 * 보석 등급.
 * 기본 보석에서 세공 → 강화 순서로 업그레이드된다.
 * 빛나는은 별도 제작 루트(시간의가루 + 보석 조각)를 통해 생성된다.
 *
 * <p>주술은 독립 등급이 아니라 강화됨 등급에 부가되는 상태이므로,
 * 주술 여부는 Gem.ritual (FK → Ritual, nullable)의 존재 여부로 표현한다.
 * 주술 부착 가능 단계: 강화됨만 허용. 빛나는에는 주술 불가.
 */
public enum GemGrade {
    /** 가공되지 않은 기본 상태 */
    BASIC("기본"),
    /** 세공된 상태 */
    REFINED("세공됨"),
    /** 강화된 상태 — 이 단계에서만 주술(ritual) 부착 가능 */
    ENHANCED("강화됨"),
    /** 빛나는 상태 — 별도 제작 루트. 주술 부착 불가 */
    SHINING("빛나는");

    private final String displayName;

    GemGrade(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
