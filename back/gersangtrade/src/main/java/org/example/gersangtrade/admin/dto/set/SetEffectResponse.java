package org.example.gersangtrade.admin.dto.set;

import org.example.gersangtrade.domain.catalog.EquipmentSetEffect;
import org.example.gersangtrade.domain.catalog.enums.BuffTarget;
import org.example.gersangtrade.domain.catalog.enums.Element;
import org.example.gersangtrade.domain.catalog.enums.StatType;
import org.example.gersangtrade.domain.catalog.enums.StatUnit;

/** 세트 효과 응답 — 필요 피스수 임계값별 스탯 보너스. */
public record SetEffectResponse(
        Long id,
        Integer requiredPieces,
        StatType statType,
        Integer statValue,
        StatUnit statUnit,
        Element element,
        BuffTarget scope
) {
    public static SetEffectResponse from(EquipmentSetEffect e) {
        return new SetEffectResponse(
                e.getId(),
                e.getRequiredPieces(),
                e.getStatType(),
                e.getStatValue(),
                e.getStatUnit(),
                e.getElement(),
                e.getScope()
        );
    }
}
