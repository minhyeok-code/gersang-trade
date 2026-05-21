package org.example.gersangtrade.admin.dto.request;

import jakarta.validation.constraints.NotNull;

/** 용병 스탯 단건 수정 요청 */
public record MercenaryStatPatchRequest(
        @NotNull Integer value
) {}
