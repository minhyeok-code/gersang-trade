package org.example.gersangtrade.domain.catalog.enums;

/**
 * 버프 수치 타입.
 * DeckBuff, LegendGeneralPassive, LegendGeneralCharacteristic에서 사용한다.
 * StatUnit과 다르게 PERCENT_ADD(가산 퍼센트)를 명시적으로 구분한다.
 */
public enum BuffValueType {
    FLAT,        // 고정값 — 예: 최소공격력 +50
    PERCENT_ADD  // 퍼센트 가산 — 예: 데미지 +2%
}
