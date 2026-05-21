package org.example.gersangtrade.calculator.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * DPS 계산기 요청 DTO.
 *
 * <p>덱 ID와 몬스터 ID를 기반으로 DPS를 계산한다.
 * memberInputs가 null이거나 특정 멤버가 누락된 경우 해당 멤버는
 * 레벨 250, 보너스 스탯 없음(MAIN_STAT 0)으로 기본값 처리된다.
 */
public record DpsRequest(

        /** 계산에 사용할 덱 ID */
        @NotNull Long deckId,

        /** 대상 몬스터 ID */
        @NotNull Long monsterId,

        /** 저항 종류 — 기본값 HITTING */
        ResistanceType resistanceType,

        /** 덱 멤버별 레벨·보너스 스탯 입력 (null이면 전원 기본값) */
        @Valid List<MemberDpsInput> memberInputs

) {}
