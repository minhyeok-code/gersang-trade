package org.example.gersangtrade.domain.catalog.enums;

/**
 * 스탯·버프 적용 대상.
 * SELF                : 해당 아이템을 장착한 용병 본인 (기본값).
 * ALLY                : 아군 덱 전체 용병에게 적용되는 파티 버프.
 * ENEMY               : 적 전체에게 적용되는 디버프.
 * ALLY_HEAVENLY_KING  : 명왕부·일반/고급 명왕 무기 — 착용 명왕과 같은 속성 사천왕 버프.
 * ALLY_SAME_ELEMENT   : 각성 명왕 무기 — 착용자 속성값의 n%를 동속성 아군 전원에게 공유.
 */
public enum BuffTarget {
    SELF,                // 장착 용병 본인
    ALLY,                // 아군 전체 버프
    ENEMY,               // 적 디버프
    ALLY_HEAVENLY_KING,  // 같은 속성 사천왕 버프 (명왕부·일반/고급 명왕 무기)
    ALLY_SAME_ELEMENT    // 동속성 아군 속성값 % 공유 (각성 명왕 무기)
}
