package org.example.gersangtrade.admin.dto.set;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 장비 세트 신규 등록 요청 */
public record AdminSetCreateRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull Integer totalPieces,
        boolean isTradeable
) {}
