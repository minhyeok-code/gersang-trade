package org.example.gersangtrade.admin.dto.request;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.example.gersangtrade.domain.catalog.enums.SkillBehaviorType;
import org.example.gersangtrade.domain.catalog.enums.SkillType;
import org.example.gersangtrade.domain.catalog.enums.StatSource;
import org.example.gersangtrade.domain.catalog.enums.TriggerSource;

/**
 * 거니버스 Skill-coeff.json 단일 행.
 * snake_case 필드를 그대로 역직렬화한다.
 *
 * <p>type="mercenary"   → mercenaryKey 필수.
 * <p>type="item"        → itemKey 필수.
 * <p>type="set_granted" → skillKey 기준으로 SetGrantedSkill을 자동 upsert 후 계수 연결.
 *                         skillBehaviorType / statSource / triggerSource 필수.
 *                         TRIGGER 행동 유형이면 triggerEveryN / triggerBaseSkillKey 필수.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record SkillCoefficientJsonRow(
        @NotBlank String rowId,
        @NotBlank String type,
        // ── type별 식별 키 ──────────────────────────────────────
        String mercenaryKey,
        String itemKey,
        // ── 공통 스킬 정보 ───────────────────────────────────────
        @NotBlank String skillKey,
        @NotBlank String skillName,
        // ── set_granted 전용 — SetGrantedSkill 생성/갱신용 ──────
        SkillBehaviorType skillBehaviorType,
        StatSource statSource,
        TriggerSource triggerSource,
        Integer triggerEveryN,
        String triggerBaseSkillKey,
        // ── 계수 필드 ────────────────────────────────────────────
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

    public boolean isSetGranted() {
        return "set_granted".equals(type);
    }
}
