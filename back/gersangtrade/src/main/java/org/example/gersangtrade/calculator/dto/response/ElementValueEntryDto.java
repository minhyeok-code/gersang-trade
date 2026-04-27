package org.example.gersangtrade.calculator.dto.response;

/**
 * 속성값 아이템/용병 가성비 리스트 항목 DTO.
 *
 * <p>현재 속성값에 해당 아이템/용병을 추가했을 때의 효과를 담는다.
 * 가성비 점수 내림차순으로 정렬되며, 최고 점수 항목은 recommended=true.
 */
public record ElementValueEntryDto(

        /** 출처 종류 — "ITEM" 또는 "MERCENARY" */
        String sourceType,

        /** 아이템 ID 또는 용병 ID */
        Long sourceId,

        /** 아이템명 또는 용병명 */
        String name,

        /** 해당 아이템/용병이 제공하는 속성값 증가량 */
        int elementValueIncrease,

        /**
         * 가격 (전(錢) 단위).
         * 집계 테이블 직전 달 평균가 또는 유저 수정값.
         * 가격 정보가 없으면 null.
         */
        Long price,

        /** 해당 아이템/용병 추가 후 속성 보정(%). clamp(-50 ~ +50) */
        double newElementBonus,

        /** 현재 대비 데미지 상승률(%) */
        double damageIncreaseRate,

        /**
         * 가성비 점수 = 데미지 상승률(%) / 가격(억 골드).
         * 가격 정보가 없거나 0이면 null.
         */
        Double efficiencyScore,

        /** 리스트 내 가성비 점수 최고 항목 여부 */
        boolean recommended,

        /**
         * 표시용 가격 문자열.
         * 1,000만 이상: "0.1억", "1억", "1.5억" 등 억 단위.
         * 1,000만 미만: "500만", "90만" 등 만 단위.
         * 가격 없으면 "-".
         */
        String formattedPrice

) {}
