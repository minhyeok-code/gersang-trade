package org.example.gersangtrade.admin.dto.setgrantedskill;

import org.example.gersangtrade.domain.catalog.SetGrantedSkill;
import org.example.gersangtrade.domain.catalog.enums.SkillBehaviorType;
import org.example.gersangtrade.domain.catalog.enums.StatSource;
import org.example.gersangtrade.domain.catalog.enums.TriggerSource;

public record SetGrantedSkillResponse(
        Long id,
        String skillKey,
        String skillName,
        SkillBehaviorType skillBehaviorType,
        StatSource statSource,
        TriggerSource triggerSource,
        Integer triggerEveryN,
        String triggerBaseSkillKey,
        String note
) {
    public static SetGrantedSkillResponse from(SetGrantedSkill s) {
        return new SetGrantedSkillResponse(
                s.getId(), s.getSkillKey(), s.getSkillName(),
                s.getSkillBehaviorType(), s.getStatSource(), s.getTriggerSource(),
                s.getTriggerEveryN(), s.getTriggerBaseSkillKey(), s.getNote());
    }
}
