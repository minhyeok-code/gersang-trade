package org.example.gersangtrade.admin.dto.set;

import jakarta.validation.constraints.NotNull;
import org.example.gersangtrade.domain.catalog.enums.BuffTarget;
import org.example.gersangtrade.domain.catalog.enums.Element;
import org.example.gersangtrade.domain.catalog.enums.StatType;
import org.example.gersangtrade.domain.catalog.enums.StatUnit;

/**
 * 세트 효과 추가 요청.
 * element·scope는 생략 시 각각 NONE·SELF로 저장된다.
 */
public record SetEffectAddRequest(
        @NotNull Integer requiredPieces,
        @NotNull StatType statType,
        @NotNull Integer statValue,
        @NotNull StatUnit statUnit,
        Element element,
        BuffTarget scope
) {}
