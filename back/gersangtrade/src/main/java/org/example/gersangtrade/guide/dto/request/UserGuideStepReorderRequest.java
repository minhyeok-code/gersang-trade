package org.example.gersangtrade.guide.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * 유저 가이드 스텝 순서 재정렬 요청.
 * orderedStepIds는 해당 사본의 전체 스텝 id를 원하는 순서대로 나열해야 한다.
 */
public record UserGuideStepReorderRequest(
        @NotEmpty List<Long> orderedStepIds
) {}
