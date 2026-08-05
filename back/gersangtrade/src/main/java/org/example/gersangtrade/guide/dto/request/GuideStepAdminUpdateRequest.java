package org.example.gersangtrade.guide.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.example.gersangtrade.domain.guide.enums.GuideStepType;

/**
 * 관리자 원본 스텝 검수 수정 요청.
 * 연결 id는 원하는 최종 상태 — null이면 해당 연결을 해제한다.
 */
public record GuideStepAdminUpdateRequest(
        GuideStepType stepType,
        @NotBlank @Size(max = 100) String label,
        @Size(max = 255) String note,
        @Size(max = 500) String iconUrl,
        Long linkedItemId,
        Long linkedSetId,
        Long linkedMercenaryId
) {}
