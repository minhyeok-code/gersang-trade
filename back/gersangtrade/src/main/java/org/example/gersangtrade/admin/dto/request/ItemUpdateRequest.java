package org.example.gersangtrade.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.example.gersangtrade.domain.catalog.enums.ItemType;

/** 아이템 기본정보 수정 요청 */
public record ItemUpdateRequest(
        @NotBlank @Size(max = 100) String name,
        ItemType type,
        String tradeCategory
) {}
