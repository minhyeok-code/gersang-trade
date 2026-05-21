package org.example.gersangtrade.admin.dto.request;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.example.gersangtrade.domain.catalog.enums.SkillType;

/**
 * 거니버스 Skill-coeff.json 단일 행.
 * snake_case 필드를 그대로 역직렬화한다.
 *
 * <p>type="mercenary" → mercenaryKey 필수, itemKey 무시.
 * <p>type="item"      → itemKey 필수, mercenaryKey 무시.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record SkillCoefficientJsonRow(
        @NotBlank String rowId,
        @NotBlank String type,
        String mercenaryKey,
        String itemKey,
        @NotBlank String skillKey,
        @NotBlank String skillName,
        float coefStr,
        float coefDex,
        float coefVit,
        float coefInt,
        float coefAtk,
        float coefLvl,
        @Min(1) int hitCount,
        float damageRangeFactor,
        SkillType skillType,
        Float castsPerSecond,
        Integer tickIntervalMs,
        String confidence,
        String note
) {
    public boolean isItem() {
        return "item".equals(type);
    }
}
