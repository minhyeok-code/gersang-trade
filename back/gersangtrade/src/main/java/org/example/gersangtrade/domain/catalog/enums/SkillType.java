package org.example.gersangtrade.domain.catalog.enums;

/**
 * 스킬 DPS 계산 방식 — 타입에 따라 사용하는 공식과 파라미터가 다르다.
 *
 * <p>INSTANT:    원데미지 × hit_count × casts_per_second
 * <p>PERSISTENT: 원데미지 / (tick_interval_ms / 1000.0)
 * <p>TRIGGER:    원데미지 × hit_count × (base_skill_dps / base_skill_damage) / trigger_every_n
 *               → trigger_every_n은 연결된 ItemSkill 또는 SetGrantedSkill에서 조회
 */
public enum SkillType {
    /** 즉발형 — 일반 시전. casts_per_second 측정 필요. */
    INSTANT,
    /** 지속/장판형 — 주기적 타격. tick_interval_ms 측정 필요. */
    PERSISTENT,
    /** 트리거형 — 기준 스킬 N회 시전마다 1회 발동. trigger_every_n 참조. */
    TRIGGER
}
