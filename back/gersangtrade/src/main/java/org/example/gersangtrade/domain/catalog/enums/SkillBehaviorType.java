package org.example.gersangtrade.domain.catalog.enums;

/**
 * 스킬 행동 유형.
 * DPS 계산 방식을 나타내는 SkillType(INSTANT/PERSISTENT)과 별개로,
 * 스킬이 어떤 조건으로 발동되는지를 구분한다.
 *
 * ItemSkill, SetGrantedSkill에서 사용한다.
 */
public enum SkillBehaviorType {
    ACTIVE,   // 일반 발동 스킬
    TRIGGER,  // 조건부 발동 스킬 (매 n번째 시전마다 발동)
    PASSIVE   // 상시 적용 패시브
}
