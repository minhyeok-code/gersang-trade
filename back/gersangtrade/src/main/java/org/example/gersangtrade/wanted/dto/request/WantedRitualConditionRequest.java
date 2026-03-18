package org.example.gersangtrade.wanted.dto.request;

import jakarta.validation.constraints.NotNull;
import org.example.gersangtrade.domain.wanted.enums.PreferredOutcome;

/**
 * 구매 희망 주술 조건 요청.
 *
 * @param ritualId        원하는 주술 ID
 * @param preferredOutcome 허용 가능한 주술 결과 — ANY | SUCCESS | GREAT_SUCCESS
 */
public record WantedRitualConditionRequest(
        @NotNull(message = "주술 ID는 필수입니다.")
        Long ritualId,

        @NotNull(message = "선호 주술 결과는 필수입니다.")
        PreferredOutcome preferredOutcome
) {
}
