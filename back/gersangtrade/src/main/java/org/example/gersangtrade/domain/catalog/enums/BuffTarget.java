package org.example.gersangtrade.domain.catalog.enums;

/**
 * 스탯·버프 적용 대상.
 * SELF : 해당 아이템을 장착한 용병 본인 (기본값).
 * ALLY : 아군 덱 전체 용병에게 적용되는 파티 버프.
 * ENEMY: 적 전체에게 적용되는 디버프.
 */
public enum BuffTarget {
    SELF,   // 장착 용병 본인
    ALLY,   // 아군 전체 버프
    ENEMY   // 적 디버프
}
