package org.example.gersangtrade.admin.dto.response;

import org.example.gersangtrade.domain.catalog.SkillCoefficient;
import org.example.gersangtrade.domain.catalog.enums.SkillType;

/** 관리자 스킬 계수 응답 */
public record SkillCoefficientAdminResponse(
        Long id,
        String rowId,
        String ownerType,
        String ownerName,
        Long skillId,
        String skillName,
        String skillKey,
        float coefStr,
        float coefDex,
        float coefVit,
        float coefInt,
        float coefAtk,
        float coefLvl,
        int hitCount,
        float damageRangeFactor,
        SkillType skillType,
        Float castsPerSecond,
        Integer tickIntervalMs,
        String confidence,
        String measurementNote
) {
    public static SkillCoefficientAdminResponse of(SkillCoefficient sc) {
        String ownerType;
        String ownerName;
        Long skillId;
        String skillName;
        String skillKey;

        if (sc.isMercenarySkill()) {
            ownerType = "MERCENARY";
            ownerName = sc.getMercenarySkill().getMercenary().getName();
            skillId = sc.getMercenarySkill().getId();
            skillName = sc.getMercenarySkill().getSkillName();
            skillKey = sc.getMercenarySkill().getSkillKey();
        } else {
            ownerType = "ITEM";
            ownerName = sc.getItemSkill().getItem().getName();
            skillId = sc.getItemSkill().getId();
            skillName = sc.getItemSkill().getSkillName();
            skillKey = sc.getItemSkill().getSkillKey();
        }

        return new SkillCoefficientAdminResponse(
                sc.getId(),
                sc.getRowId(),
                ownerType,
                ownerName,
                skillId,
                skillName,
                skillKey,
                sc.getCoefStr(),
                sc.getCoefDex(),
                sc.getCoefVit(),
                sc.getCoefInt(),
                sc.getCoefAtk(),
                sc.getCoefLvl(),
                sc.getHitCount(),
                sc.getDamageRangeFactor(),
                sc.getSkillType(),
                sc.getCastsPerSecond(),
                sc.getTickIntervalMs(),
                sc.getConfidence(),
                sc.getMeasurementNote()
        );
    }
}
