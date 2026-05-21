package org.example.gersangtrade.domain.catalog.enums;

/**
 * 속성 종류.
 * FIRE: 화, WATER: 수, WIND: 풍, EARTH: 토, THUNDER: 뇌.
 * ADAPTIVE: 착용 용병의 속성을 따라감 (용병 속성이 FIRE면 화속성값 +n).
 * NONE: 속성 구분 없음 — null 대신 사용하여 UNIQUE 제약이 정상 동작하도록 한다.
 */
public enum Element {
    FIRE,       // 화속성
    WATER,      // 수속성
    WIND,       // 풍속성
    EARTH,      // 토속성
    THUNDER,    // 뇌속성
    ADAPTIVE,   // 용병 속성 추종 (착용 용병 속성값 +n)
    NONE        // 속성 구분 없음 (null 대체용 기본값)
}
