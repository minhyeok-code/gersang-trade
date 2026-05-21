package org.example.gersangtrade.calculator.dto.response;

import java.util.List;

/**
 * 덱 멤버 1명의 DPS 계산 결과.
 * adjustedDps는 저항 통과율 × 속성 보정이 적용된 최종값이다.
 */
public record MemberDpsResult(
        Long memberId,
        Long mercenaryId,
        String mercenaryName,
        double elementBonus,
        double rawDps,
        double adjustedDps,
        List<SkillDpsResult> skillResults
) {}
