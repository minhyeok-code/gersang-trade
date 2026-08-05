package org.example.gersangtrade.guide.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.example.gersangtrade.domain.guide.enums.GuideStepType;

/**
 * 유저 가이드 커스텀 스텝 추가 요청.
 * 연결 id(item/set/mercenary)는 선택 — 주어지면 카탈로그에서 조회해 매물 funnel·이미지에 쓴다.
 */
public record GuideStepAddRequest(
        @NotNull GuideStepType stepType,
        @NotBlank @Size(max = 100) String label,
        @Size(max = 255) String note,
        @Size(max = 500) String iconUrl,
        Long linkedItemId,
        Long linkedSetId,
        Long linkedMercenaryId
) {}
