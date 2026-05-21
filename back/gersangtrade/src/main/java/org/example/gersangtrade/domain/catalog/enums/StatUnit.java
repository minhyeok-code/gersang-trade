package org.example.gersangtrade.domain.catalog.enums;

/**
 * 능력치 수치의 단위.
 * ItemStat, EquipmentSetEffect, RitualSetEffect에서 공통으로 사용한다.
 */
public enum StatUnit {
    FLAT,    // 수치 그대로 — 예: 힘 +400
    PERCENT, // 퍼센트 — 예: 타저 +25%, 데미지 +2%
    LEVEL    // 단계 — 예: 이속 +1단계
}
