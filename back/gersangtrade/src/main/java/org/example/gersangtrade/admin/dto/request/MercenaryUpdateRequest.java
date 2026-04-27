package org.example.gersangtrade.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.example.gersangtrade.domain.catalog.enums.MercenaryCategory;
import org.example.gersangtrade.domain.catalog.enums.Nation;
import org.example.gersangtrade.domain.catalog.enums.Nature;

/** 용병 기본정보 수정 요청 */
public record MercenaryUpdateRequest(
        @NotBlank @Size(max = 100) String name,
        MercenaryCategory category,
        Nation nation,
        Nature nature,
        Integer natureValue,
        boolean comingSoon
) {}
