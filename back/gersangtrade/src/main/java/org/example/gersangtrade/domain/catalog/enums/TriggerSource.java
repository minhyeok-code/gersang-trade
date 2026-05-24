package org.example.gersangtrade.domain.catalog.enums;

/**
 * 트리거 카운트를 누가 발생시키는지.
 * SetGrantedSkill에서 사용한다.
 *
 * MVP에서는 MERCENARY 타입 스킬은 계산에서 제외하고 note로 표시.
 */
public enum TriggerSource {
    SELF,       // 스킬 보유 용병 본인의 스킬 시전 횟수 카운트
    MERCENARY   // 인연에 연결된 사천왕의 스킬 시전 횟수 카운트 (인연 구현 시 활성화)
}
