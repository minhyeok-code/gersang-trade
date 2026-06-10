package org.example.gersangtrade.calculator.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

/**
 * DPS 가성비 평가 실행 요청.
 * POST /api/calculator/dps/evaluations
 */
public record DpsEvaluationRequest(

        @NotNull Long deckId,

        @NotNull Long monsterId,

        @NotNull ResistanceType resistanceType,

        @NotNull @Valid ScenarioRequest scenario,

        /**
         * 유저 가격 override 맵 — "ITEM:{itemId}" or "SET:{setId}" → 골드.
         * ITEM_* 시나리오 전용. 지정하지 않으면 TradeStatMonthly 자동 조회.
         */
        Map<String, Long> priceOverrides,

        /**
         * 용병 가격 — 유저 직접 입력 (MERCENARY 시나리오 전용).
         * null·0 → 가성비 null (DPS 증가분은 표시).
         */
        Long price,

        /** 멤버별 레벨·스탯분배 입력 (기존 DPS API와 동일) */
        List<@Valid MemberDpsInput> memberInputs,

        /** false이면 계산·응답만 하고 DB·스냅샷 저장 생략 */
        boolean persist

) {}
