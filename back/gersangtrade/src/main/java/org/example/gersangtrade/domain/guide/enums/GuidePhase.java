package org.example.gersangtrade.domain.guide.enums;

/**
 * 가이드 단계 구분.
 * 지국천왕처럼 일반 → 각성으로 이어지는 육성 로드맵을 두 단계로 나눈다.
 * NORMAL(일반) 가이드의 마지막이 AWAKENED(각성) 가이드의 출발점이 된다.
 */
public enum GuidePhase {
    NORMAL,   // 일반
    AWAKENED  // 각성
}
