package org.example.gersangtrade.listing.dto.request;

import jakarta.validation.constraints.NotNull;
import org.example.gersangtrade.domain.listing.enums.RitualOutcome;

/**
 * 장비 라인에 적용된 주술 결과 등록 요청.
 *
 * @param ritualId 주술 ID (RitualApplicability에서 조회한 값)
 * @param outcome  주술 결과 — SUCCESS | GREAT_SUCCESS
 */
public record RitualResultRequest(
        @NotNull(message = "주술 ID는 필수입니다.")
        Long ritualId,

        @NotNull(message = "주술 결과(SUCCESS/GREAT_SUCCESS)는 필수입니다.")
        RitualOutcome outcome
) {
}
