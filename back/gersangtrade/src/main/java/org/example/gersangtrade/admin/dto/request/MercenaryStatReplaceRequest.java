package org.example.gersangtrade.admin.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.example.gersangtrade.domain.catalog.enums.StatType;

import java.util.List;

/** 용병 스탯 전체 교체 요청 (기존 삭제 후 재적재) */
public record MercenaryStatReplaceRequest(
        @Valid @NotNull List<Entry> stats
) {
    public record Entry(
            @NotNull StatType statType,
            @NotNull Integer value
    ) {}
}
