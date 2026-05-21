package org.example.gersangtrade.calculator.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 덱 멤버별 DPS 계산 입력값.
 * 레벨과 보너스 스탯 배분은 DB에 저장하지 않으며 요청 시에만 적용된다.
 */
public record MemberDpsInput(

        /** 덱 멤버 ID (UserDeckMember.id) */
        @NotNull Long memberId,

        /** 용병 레벨 — 250 또는 260 */
        @NotNull int level,

        /** 보너스 스탯 투자 대상 */
        @NotNull BonusStatTarget bonusTarget,

        /** 보너스 스탯 총량 (300/500/700/900/1000 또는 직접 입력) */
        @Min(0) int bonusAmount

) {}
