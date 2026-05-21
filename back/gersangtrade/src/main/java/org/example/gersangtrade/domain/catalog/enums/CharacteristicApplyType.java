package org.example.gersangtrade.domain.catalog.enums;

/**
 * 특성 적용 방식.
 * MercenaryCharacteristic.applyType 컬럼에 저장된다.
 */
public enum CharacteristicApplyType {
    /** 포인트 배분형 — 일반 특성 트리 */
    NORMAL,
    /** 자동 적용, 자신에게만 — 각성 사천왕 각성 특성 */
    SELF_AUTO,
    /** 자동 적용, 조건부 아군에게 — 주인공 국적 버프 */
    ALLY_AUTO,
}
