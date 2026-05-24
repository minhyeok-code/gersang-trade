package org.example.gersangtrade.domain.catalog.enums;

/**
 * 스킬 데미지 계산 시 스탯 참조 대상.
 * SetGrantedSkill에서 사용한다.
 *
 * MVP에서는 AFFINITY 타입 스킬은 계산에서 제외하고 note로 표시.
 */
public enum StatSource {
    SELF,       // 스킬 보유 용병(전설장수) 본인의 스탯으로 계산
    AFFINITY    // 인연에 연결된 사천왕의 스탯으로 계산 (인연 구현 시 활성화)
}
