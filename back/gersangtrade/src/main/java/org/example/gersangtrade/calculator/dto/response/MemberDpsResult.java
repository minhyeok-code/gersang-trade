package org.example.gersangtrade.calculator.dto.response;

import java.util.List;

/**
 * 덱 멤버 1명의 DPS 계산 결과.
 *
 * <p>DPS 구분:
 * <ul>
 *   <li>rawDps              — 계수 기반 순수 DPS (보정 없음)
 *   <li>elementAdjustedDps  — 속성 보정만 적용한 DPS (저항 미적용)
 *   <li>adjustedDps         — 속성 + 저항 통과율 모두 적용한 최종 DPS
 *   <li>damageShare         — adjustTotalDps 기준 이 용병의 데미지 비중 (%)
 * </ul>
 */
public record MemberDpsResult(
        Long memberId,
        Long mercenaryId,
        String mercenaryName,
        int elementValue,
        double elementBonus,
        long rawDps,
        long elementAdjustedDps,
        long adjustedDps,
        double damageShare,
        List<SkillDpsResult> skillResults
) {}
