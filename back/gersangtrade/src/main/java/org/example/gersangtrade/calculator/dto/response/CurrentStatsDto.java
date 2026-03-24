package org.example.gersangtrade.calculator.dto.response;

/**
 * 현재 스펙 기준 데미지 현황 DTO.
 *
 * <p>유저 입력(현재 저항깎, 속성값, 몬스터 스펙)을 바탕으로 계산한 결과다.
 * 가성비 리스트의 "before" 기준값으로도 활용된다.
 */
public record CurrentStatsDto(

        /** 깎은 뒤 저항 = 몬스터 저항 - 총 저항깎 수치 */
        int resistAfterDebuff,

        /** 저항 통과율(%). 260 이상이면 1.4% 고정, 미만이면 100 - (저항 × 0.16 + 57) */
        double resistPassRate,

        /** 속성 보정(%). clamp((3 × 용병 속성값 - 몬스터 속성값) / 2, -50, +50) */
        double elementBonus,

        /** 종합 데미지 배율 = (100 + 속성 보정) × (통과율 / 100) */
        double damageMultiplier,

        /** 저깎 0% 기준 배율 = 현재 데미지 배율 / 저깎 없을 때 데미지 배율 */
        double baseMultiplier

) {}
