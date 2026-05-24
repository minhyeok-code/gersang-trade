package org.example.gersangtrade.admin.dto.setgrantedskill;

import org.example.gersangtrade.domain.catalog.EquipmentSetSkillEffect;
import org.example.gersangtrade.domain.catalog.enums.Enhancement;

public record SkillEffectResponse(
        Long id,
        Long setId,
        String setName,
        int requiredPieces,
        Enhancement enhancement,
        Long setGrantedSkillId,
        String setGrantedSkillName
) {
    public static SkillEffectResponse from(EquipmentSetSkillEffect e) {
        return new SkillEffectResponse(
                e.getId(),
                e.getEquipmentSet().getId(),
                e.getEquipmentSet().getName(),
                e.getRequiredPieces(),
                e.getEnhancement(),
                e.getSetGrantedSkill().getId(),
                e.getSetGrantedSkill().getSkillName());
    }
}
