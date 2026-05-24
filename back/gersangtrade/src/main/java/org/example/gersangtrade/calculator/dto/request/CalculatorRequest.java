package org.example.gersangtrade.calculator.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.example.gersangtrade.domain.catalog.enums.Element;

import java.util.Map;

/**
 * 가성비 계산기 요청 DTO.
 *
 * <p>유저가 현재 스펙과 목표 몬스터 정보를 입력하면,
 * 서버에서 현재 데미지 현황과 추가 시 가성비가 좋은 아이템/용병 목록을 반환한다.
 *
 * <p>priceOverrides — 유저가 가격을 직접 수정한 경우 입력한다.
 * 키 형식: "ITEM_{itemId}" 또는 "MERC_{mercenaryId}", 값: 골드 단위 가격.
 * DB에 저장하지 않으며 해당 요청에만 적용된다.
 */
public record CalculatorRequest(

        /** 현재 보유한 총 저항깎 수치 (디버퍼 용병 + 장비 합산) */
        @NotNull(message = "현재 저항깎 수치는 필수입니다.")
        @Min(value = 0, message = "저항깎 수치는 0 이상이어야 합니다.")
        Integer currentResistPierce,

        /** 현재 용병의 속성값 */
        @NotNull(message = "현재 용병 속성값은 필수입니다.")
        @Min(value = 0, message = "용병 속성값은 0 이상이어야 합니다.")
        Integer currentElementValue,

        /** 목표 몬스터의 저항 수치 */
        @NotNull(message = "몬스터 저항 수치는 필수입니다.")
        @Min(value = 0, message = "몬스터 저항 수치는 0 이상이어야 합니다.")
        Integer monsterResistance,

        /** 목표 몬스터의 속성값 */
        @NotNull(message = "몬스터 속성값은 필수입니다.")
        @Min(value = 0, message = "몬스터 속성값은 0 이상이어야 합니다.")
        Integer monsterElementValue,

        /**
         * 목표 몬스터 속성 종류.
         * null 또는 NONE이면 무속성 — 속성 보정 0.
         */
        Element monsterElement,

        /** 가격 기본값 조회에 사용할 서버 ID (1~13) */
        @NotNull(message = "서버 ID는 필수입니다.")
        Integer serverId,

        /**
         * 유저가 수정한 아이템/용병 가격 (선택).
         * 키: "ITEM_{itemId}" 또는 "MERC_{mercenaryId}", 값: 골드 단위 가격.
         * null이면 모든 가격을 집계 테이블 기본값으로 사용한다.
         */
        Map<String, Long> priceOverrides

) {}
