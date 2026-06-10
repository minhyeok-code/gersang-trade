package org.example.gersangtrade.admin.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.example.gersangtrade.domain.catalog.enums.BuffTarget;
import org.example.gersangtrade.domain.catalog.enums.Element;
import org.example.gersangtrade.domain.catalog.enums.StatType;

import java.util.List;

/** 아이템 스탯 전체 교체 요청 (기존 삭제 후 재적재) */
public record ItemStatReplaceRequest(
        @Valid @NotNull List<Entry> stats
) {
    public record Entry(
            @NotNull StatType statType,
            Element element,   // null이면 NONE으로 처리
            @NotNull Integer value,
            BuffTarget scope   // null이면 SELF
    ) {}
}
