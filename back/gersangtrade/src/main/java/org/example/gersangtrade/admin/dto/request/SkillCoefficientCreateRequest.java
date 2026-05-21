package org.example.gersangtrade.admin.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.example.gersangtrade.domain.catalog.enums.SkillType;

/**
 * 스킬 계수 수동 생성 요청.
 * mercenarySkillId 또는 itemSkillId 중 하나만 입력한다.
 */
public record SkillCoefficientCreateRequest(
        Long mercenarySkillId,
        Long itemSkillId,
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
