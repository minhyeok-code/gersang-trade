package org.example.gersangtrade.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 용병 특성 생성 요청.
 *
 * @param name                   특성명 (예: "광풍", "기습")
 * @param point                  특성 포인트 비용. 각성 특성이면 null.
 * @param description            특성 설명 (선택)
 * @param requiredCharacteristicId 선행 특성 ID. 루트 특성이면 null.
 */
public record CharacteristicCreateRequest(
        @NotBlank @Size(max = 50) String name,
        Integer point,
        @Size(max = 500) String description,
        Long requiredCharacteristicId
) {}
