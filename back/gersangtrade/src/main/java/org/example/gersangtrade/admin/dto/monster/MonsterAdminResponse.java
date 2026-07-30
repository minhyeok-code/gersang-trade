package org.example.gersangtrade.admin.dto.monster;

import org.example.gersangtrade.domain.catalog.Monster;
import org.example.gersangtrade.domain.catalog.enums.Element;

/** 몬스터 관리자 응답 DTO */
public record MonsterAdminResponse(
        Long id,
        String name,
        Long hp,
        Integer hittingResistance,
        Integer magicResistance,
        Integer elementValue,
        Element element,
        String imageUrl,
        boolean hidden
) {
    public static MonsterAdminResponse from(Monster m) {
        return new MonsterAdminResponse(
                m.getId(),
                m.getName(),
                m.getHp(),
                m.getHittingResistance(),
                m.getMagicResistance(),
                m.getElementValue(),
                m.getElement(),
                m.getImageUrl(),
                m.isHidden()
        );
    }
}
