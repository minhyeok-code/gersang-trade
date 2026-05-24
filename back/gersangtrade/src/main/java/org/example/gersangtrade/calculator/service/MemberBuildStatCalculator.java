package org.example.gersangtrade.calculator.service;

import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.calculator.dto.request.BonusStatTarget;
import org.example.gersangtrade.catalog.repository.SkillCoefficientRepository;
import org.example.gersangtrade.domain.catalog.SkillCoefficient;
import org.example.gersangtrade.domain.catalog.enums.StatType;
import org.example.gersangtrade.domain.deck.UserDeckMember;
import org.example.gersangtrade.domain.deck.UserDeckMemberSlot;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 멤버 레벨·보너스 스탯을 DPS 계산과 동일한 규칙으로 산출한다.
 * 레벨 스탯은 주스탯(스킬 계수 기준)에, 보너스는 주스탯/생명력 선택에 따라 부여한다.
 */
@Component
@RequiredArgsConstructor
public class MemberBuildStatCalculator {

    public static final int LEVEL_STAT_250 = 2256;
    public static final int LEVEL_STAT_260 = 2466;

    private final SkillCoefficientRepository skillCoefficientRepository;

    public record BuildStatBonus(
            Map<StatType, Integer> levelBonusStats,
            Map<StatType, Integer> bonusStats,
            StatType resolvedMainStat
    ) {}

    /** 레벨·보너스 스탯 맵과 판별된 주스탯을 반환한다. */
    public BuildStatBonus compute(UserDeckMember member, List<UserDeckMemberSlot> slots) {
        StatType mainStat = resolvePrimaryMainStat(member, slots);
        int levelStat = member.getLevel() == 260 ? LEVEL_STAT_260 : LEVEL_STAT_250;

        Map<StatType, Integer> levelBonusStats = new EnumMap<>(StatType.class);
        levelBonusStats.put(mainStat, levelStat);

        Map<StatType, Integer> bonusStats = new EnumMap<>(StatType.class);
        int bonusAmount = member.getBonusAmount();
        if (bonusAmount > 0) {
            if (member.getBonusTarget() == BonusStatTarget.VITALITY) {
                bonusStats.put(StatType.VITALITY, bonusAmount);
            } else {
                bonusStats.put(mainStat, bonusAmount);
            }
        }

        return new BuildStatBonus(levelBonusStats, bonusStats, mainStat);
    }

    /** DPS와 동일 — 착용 장비 스킬 우선, 없으면 용병 스킬. 둘 다 없으면 생명력. */
    private StatType resolvePrimaryMainStat(UserDeckMember member, List<UserDeckMemberSlot> slots) {
        List<SkillCoefficient> coefs = loadPrimarySkillCoefs(member, slots);
        if (coefs.isEmpty()) {
            return StatType.VITALITY;
        }
        return resolveMainStat(coefs.getFirst());
    }

    /** 주스탯 판별용 스킬 계수 — 장비 스킬 우선, 없으면 용병 스킬. */
    private List<SkillCoefficient> loadPrimarySkillCoefs(UserDeckMember member, List<UserDeckMemberSlot> slots) {
        if (!slots.isEmpty()) {
            List<Long> itemIds = slots.stream()
                    .map(s -> s.getEquipmentItem().getItemId())
                    .distinct()
                    .toList();
            Map<Long, List<SkillCoefficient>> coefsByItemId = skillCoefficientRepository.findByItemIdIn(itemIds)
                    .stream()
                    .collect(Collectors.groupingBy(sc -> sc.getItemSkill().getItem().getId()));

            for (UserDeckMemberSlot slot : slots) {
                Long itemId = slot.getEquipmentItem().getItemId();
                List<SkillCoefficient> itemCoefs = coefsByItemId.get(itemId);
                if (itemCoefs != null && !itemCoefs.isEmpty()) {
                    return itemCoefs;
                }
            }
        }

        return new ArrayList<>(skillCoefficientRepository.findByMercenaryIdIn(
                List.of(member.getMercenary().getId())));
    }

    /**
     * 주스탯 판별 — STR/DEX/VIT/INT 중 계수가 가장 높은 스탯 (공격력·레벨 계수 제외).
     * 동점이면 STRENGTH → DEXTERITY → VITALITY → INTELLECT 순.
     */
    public static StatType resolveMainStat(SkillCoefficient coef) {
        float max = Math.max(Math.max(coef.getCoefStr(), coef.getCoefDex()),
                Math.max(coef.getCoefVit(), coef.getCoefInt()));
        if (max == 0f) {
            return StatType.VITALITY;
        }
        if (coef.getCoefStr() >= max) {
            return StatType.STRENGTH;
        }
        if (coef.getCoefDex() >= max) {
            return StatType.DEXTERITY;
        }
        if (coef.getCoefVit() >= max) {
            return StatType.VITALITY;
        }
        return StatType.INTELLECT;
    }
}
