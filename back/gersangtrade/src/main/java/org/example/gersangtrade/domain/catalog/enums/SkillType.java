package org.example.gersangtrade.domain.catalog.enums;

/**
 * 스킬 시전 방식 — DPS 계산 공식이 타입에 따라 달라진다.
 *
 * <p>INSTANT:    원데미지 × hit_count × casts_per_second
 * <p>PERSISTENT: 원데미지 / (tick_interval_ms / 1000.0)
 */
public enum SkillType {
    /** 즉발형 — 일반 시전. casts_per_second 측정 필요. */
    INSTANT,
    /** 지속/장판형 — 주기적 타격. tick_interval_ms 측정 필요. */
    PERSISTENT
}
