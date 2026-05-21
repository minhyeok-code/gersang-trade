package org.example.gersangtrade.calculator.dto.response;

import java.util.List;

/**
 * DPS 계산기 응답 DTO.
 *
 * <p>totalDps는 저항 통과율 × 속성 보정이 모두 적용된 전체 합산값이다.
 */
public record DpsResponse(
        Long monsterId,
        String monsterName,
        int totalResistPierce,
        int resistAfterDebuff,
        double resistPassRate,
        double totalDps,
        List<MemberDpsResult> memberResults
) {}
