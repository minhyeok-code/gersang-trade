package org.example.gersangtrade.domain.catalog.enums;

/**
 * 능력치 종류 — 아이템(ItemStat) 및 용병(MercenaryStat)에서 공유한다.
 * MVP statType: ELEMENT_VALUE / ELEMENT_PIERCE / RESIST_PIERCE
 * 용병 전용: STRENGTH / VITALITY / DEXTERITY / INTELLECT / DEFENSE / SIGHT /
 *           HIT_RATE / CRITICAL_CHANCE / MIN_POWER / MAX_POWER /
 *           MAGIC_RESISTANCE / HITTING_RESISTANCE
 */
public enum StatType {
    // ── 아이템·용병 공통 ───────────────────────────────
    ELEMENT_VALUE("속성값"),
    ELEMENT_PIERCE("속성깎"),
    RESIST_PIERCE("저항깎"),

    // ── 용병 전용 기본 스탯 ────────────────────────────
    STRENGTH("힘"),
    VITALITY("생명력"),
    DEXTERITY("민첩"),
    INTELLECT("지력"),
    DEFENSE("방어"),
    SIGHT("시야"),
    HIT_RATE("명중률"),
    CRITICAL_CHANCE("크리티컬확률"),
    MIN_POWER("최소공격력"),
    MAX_POWER("최대공격력"),
    MAGIC_RESISTANCE("마법저항"),
    HITTING_RESISTANCE("타격저항");

    /** 화면에 표시할 한국어 명칭 */
    private final String displayName;

    StatType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
