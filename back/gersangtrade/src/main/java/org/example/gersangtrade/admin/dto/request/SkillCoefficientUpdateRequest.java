package org.example.gersangtrade.admin.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.example.gersangtrade.domain.catalog.enums.SkillType;

/**
 * 스킬 계수 전체 수정 요청 (PUT 의미론).
 * FK(스킬 대상)는 변경 불가. 계수·측정값·메타데이터만 수정한다.
 */
public record SkillCoefficientUpdateRequest(
        String rowId,
        float coefStr,
        float coefDex,
        float coefVit,
        float coefInt,
        float coefAtk,
        float coefLvl,
        @Min(1) @NotNull Integer hitCount,
        float damageRangeFactor,
        SkillType skillType,
        Float castsPerSecond,
        Integer tickIntervalMs,
        String confidence,
        String note
) {}
