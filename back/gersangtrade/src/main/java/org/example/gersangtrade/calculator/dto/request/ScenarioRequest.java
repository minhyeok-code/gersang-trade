package org.example.gersangtrade.calculator.dto.request;

import org.example.gersangtrade.calculator.overlay.MercenaryMode;
import org.example.gersangtrade.calculator.overlay.ScenarioItemType;
import org.example.gersangtrade.calculator.overlay.ScenarioLine;
import org.example.gersangtrade.hunt.dto.DeckSnapshotContent.CharacteristicSelection;

import java.util.List;

/**
 * 가성비 평가 시나리오 요청 — 아이템/용병 공용.
 * type에 따라 유효 필드가 다르며, DpsScenarioOverlayFactory에서 검증·변환한다.
 * 가격(price)은 MERCENARY 시 DpsEvaluationRequest 최상위에서 수신한다.
 */
public record ScenarioRequest(

        /** 시나리오 종류 */
        ScenarioItemType type,

        // ── ITEM_SET 전용 ──

        /** 세트 ID (ITEM_SET 시 필수) */
        Long setId,

        // ── ITEM_SINGLE · ITEM_SET 공용 ──

        /**
         * 아이템·세트를 장착할 덱 멤버 ID.
         * ITEM_* 시 필수, MERCENARY REPLACE 시 교체 대상, MERCENARY APPEND 시 null.
         */
        Long affectedMemberId,

        /**
         * 피스별 장비 상세 (강화·주술).
         * ITEM_SET 주술 미사용 시 생략·[] 허용 → 전 피스 미주술.
         * ITEM_SET 주술 사용 시 피스 수만큼 필수.
         * ITEM_SINGLE 시 lines 1개 필수.
         */
        List<ScenarioLine> lines,

        // ── MERCENARY 전용 ──

        /** 용병 ID (MERCENARY 시 필수) */
        Long mercenaryId,

        /** 편성 방식 (MERCENARY 시 필수) */
        MercenaryMode mode,

        /** 용병 레벨 250 or 260 (MERCENARY 시 필수) */
        Integer level,

        /** 보너스 스탯 투자 대상 (MERCENARY 시 필수) */
        BonusStatTarget bonusTarget,

        /** 보너스 스탯 총량 (MERCENARY 시 필수) */
        Integer bonusAmount,

        /** 선택 특성 목록 (MERCENARY 시 선택, 없으면 빈 리스트) */
        List<CharacteristicSelection> characteristics

) {}
