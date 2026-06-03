package org.example.gersangtrade.domain.catalog.enums;

/**
 * 능력치 종류 — ItemStat, MercenaryStat, EquipmentSetEffect, RitualSetEffect에서 공유한다.
 */
public enum StatType {
    // ── 아이템·용병 공통 ───────────────────────────────
    ELEMENT_VALUE("속성값"),
    ELEMENT_PIERCE("속성깎"),
    RESIST_PIERCE("저항깎"),

    // ── 공격력 (아이템·용병 공통) ──────────────────────
    MIN_POWER("최소공격력"),
    MAX_POWER("최대공격력"),

    // ── 기본 스탯 (용병·세트효과·주술세트효과 공통) ───
    STRENGTH("힘"),
    VITALITY("생명력"),
    DEXTERITY("민첩"),
    INTELLECT("지력"),
    DEFENSE("방어"),

    // ── 용병 전용 ─────────────────────────────────────
    SIGHT("시야"),
    HIT_RATE("명중률"),
    CRITICAL_CHANCE("크리티컬확률"),

    // ── 저항 (용병·세트효과 공통) ─────────────────────
    MAGIC_RESISTANCE("마법저항"),
    HITTING_RESISTANCE("타격저항"),

    // ── 세트효과·주술세트효과 전용 ────────────────────
    DAMAGE_PERCENT("데미지증가"),
    SKILL_DAMAGE_PERCENT("스킬데미지증가"),
    FIELD_MOVE_SPEED("필드이동속도"),

    // ── 전체 능력치 일괄 증가 ─────────────────────────
    ALL_STAT("모든능력치"),

    // ── 주술 전용 ─────────────────────────────────────────────
    CRITICAL_RATE("치명타확률"),
    CRITICAL_DAMAGE("치명타피해"),
    MIN_DAMAGE("최소데미지"),
    MAX_DAMAGE("최대데미지"),

    // ── 정령 버프 전용 ────────────────────────────────────────
    ATTACK_SPEED("공격속도"),
    MOVE_SPEED("이동속도"),
    HP_RECOVERY("체력회복"),
    MP_RECOVERY("마력회복"),

    // ── 전설장수 전용 ─────────────────────────────────────────
    DAMAGE_PERCENT_GROUND("지상데미지증가"),
    DAMAGE_PERCENT_AIR("공중데미지증가"),
    STUN_DURATION("기절시간"),

    // ── DPS 계산용 (스킬 계수 곱셈 대상) ────────────────────
    ATTACK_POWER("공격력"),

    // ── 주인공 전용 ───────────────────────────────────────────
    SKILL_RANGE("사거리"),

    // ── 공명·가호 전용 ────────────────────────────────────────
    MAIN_STAT_FLAT("주스텟"),

    // ── 특성 전용 ─────────────────────────────────────────────
    BASE_DAMAGE_MULTIPLIER("기본데미지배율");

    /** 화면에 표시할 한국어 명칭 */
    private final String displayName;

    StatType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
