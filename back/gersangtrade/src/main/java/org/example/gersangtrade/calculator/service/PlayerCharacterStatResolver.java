package org.example.gersangtrade.calculator.service;

import org.example.gersangtrade.domain.catalog.Mercenary;
import org.example.gersangtrade.domain.catalog.enums.Nature;
import org.example.gersangtrade.domain.catalog.enums.StatType;
import org.example.gersangtrade.domain.deck.UserDeckMemberSlot;
import org.example.gersangtrade.domain.deck.enums.EquipSlot;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 주인공 공명 MAIN_STAT_FLAT 대상 스탯을 결정한다.
 *
 * <p>결정 순서:
 * 1. WEAPON 슬롯에 사인검이 장착되어 있으면 → INTELLECT (모든속성 공통)
 * 2. 사인검 미장착: 주인공 속성(Nature) 기준
 *    화(FIRE)→STRENGTH, 풍(WIND)→VITALITY, 뇌(THUNDER)→DEXTERITY, 수(WATER)→INTELLECT
 */
@Component
public class PlayerCharacterStatResolver {

    public StatType resolve(Mercenary protagonist, List<UserDeckMemberSlot> slots) {
        boolean saintSword = slots.stream()
                .filter(s -> s.getSlot() == EquipSlot.WEAPON)
                .findFirst()
                .map(s -> s.getEquipmentItem().isSainSword())
                .orElse(false);

        if (saintSword) return StatType.INTELLECT;

        return resolveByNature(protagonist.getNature());
    }

    /** 속성 → 주스텟 매핑. EARTH·null은 STRENGTH 기본(실제 주인공은 4속성만 존재). */
    private StatType resolveByNature(Nature nature) {
        if (nature == null) return StatType.STRENGTH;
        return switch (nature) {
            case FIRE    -> StatType.STRENGTH;
            case WIND    -> StatType.VITALITY;
            case THUNDER -> StatType.DEXTERITY;
            case WATER   -> StatType.INTELLECT;
            default      -> StatType.STRENGTH;
        };
    }
}
