package org.example.gersangtrade.calculator.dto.response;

import java.util.List;

/**
 * 가성비 계산기 응답 DTO.
 *
 * <p>현재 스펙 기반 데미지 현황과 저항깎·속성값 두 탭의 아이템/용병 리스트를 반환한다.
 * 각 리스트는 가성비 점수 내림차순으로 정렬되며 최고 점수 항목에 recommended=true가 설정된다.
 */
public record CalculatorResponse(

        /** 현재 스펙 기준 데미지 현황 */
        CurrentStatsDto currentStats,

        /**
         * 저항깎 아이템/용병 리스트.
         * 현재 저항깎에 각 항목을 추가했을 때의 효과를 가성비 점수 내림차순으로 정렬.
         */
        List<ResistPierceEntryDto> resistPierceList,

        /**
         * 속성값 아이템/용병 리스트.
         * 현재 속성값에 각 항목을 추가했을 때의 효과를 가성비 점수 내림차순으로 정렬.
         */
        List<ElementValueEntryDto> elementValueList

) {}
