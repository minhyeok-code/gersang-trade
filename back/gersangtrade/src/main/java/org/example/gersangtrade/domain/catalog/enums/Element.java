package org.example.gersangtrade.domain.catalog.enums;

/**
 * 속성 종류.
 * FIRE: 화, WATER: 수, WIND: 풍, EARTH: 토, LIGHTNING: 뇌, NONE: 속성 구분 없음.
 * null 대신 NONE을 사용하여 유니크 제약 조건이 정상 동작하도록 한다.
 */
public enum Element {
    FIRE,       // 화속성
    WATER,      // 수속성
    WIND,       // 풍속성
    EARTH,      // 토속성
    LIGHTNING,  // 뇌속성
    NONE        // 속성 구분 없음 (null 대체용 기본값)
}
