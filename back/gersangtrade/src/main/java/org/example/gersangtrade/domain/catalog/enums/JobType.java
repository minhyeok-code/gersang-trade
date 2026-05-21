package org.example.gersangtrade.domain.catalog.enums;

/**
 * 주인공 전직 단계.
 * 인도는 NORMAL 불가 (귀화 아이템으로 1차 이후 진입).
 * 계산기는 SECOND(2차전직)만 대상으로 한다.
 */
public enum JobType {
    NORMAL,  // 노말
    FIRST,   // 1차전직
    SECOND,  // 2차전직 — MVP 계산 대상
}
