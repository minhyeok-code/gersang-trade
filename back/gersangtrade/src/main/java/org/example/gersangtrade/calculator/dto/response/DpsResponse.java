package org.example.gersangtrade.calculator.dto.response;

import java.util.List;

/**
 * DPS 계산기 응답 DTO.
 *
 * <p>DPS 구분:
 * <ul>
 *   <li>rawTotalDps       — 계수 기반 순수 합산 (보정 없음)
 *   <li>adjustTotalDps    — 속성 보정만 적용한 합산 (저항 미적용). 비중 계산 기준.
 *   <li>totalDps          — 속성 + 저항 통과율 모두 적용한 최종 합산
 * </ul>
 */
public record DpsResponse(
        Long monsterId,
        String monsterName,
        int totalResistPierce,
        int resistAfterDebuff,
        double resistPassRate,
        int totalElementPierce,
        int effectiveMonsterElement,
        long rawTotalDps,
        long adjustTotalDps,
        long totalDps,
        List<MemberDpsResult> memberResults
) {}
