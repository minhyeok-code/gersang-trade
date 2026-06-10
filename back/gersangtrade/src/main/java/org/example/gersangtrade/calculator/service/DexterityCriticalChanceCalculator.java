package org.example.gersangtrade.calculator.service;

import org.example.gersangtrade.calculator.dto.request.BonusStatTarget;
import org.example.gersangtrade.domain.catalog.enums.StatType;

import java.util.Map;

/**
 * 민첩 스탯에 따른 크리티컬확률 보너스 계산.
 * 게임 규칙: 민첩 1000당 크리티컬확률 2%p.
 */
public final class DexterityCriticalChanceCalculator {

    public static final int DEX_PER_CRIT_BLOCK = 1000;
    public static final int CRIT_PERCENT_PER_BLOCK = 2;

    private DexterityCriticalChanceCalculator() {}

    /** 민첩 수치로부터 크리티컬확률 보너스(%p)를 산출한다. */
    public static int fromDexterity(int dexterity) {
        if (dexterity <= 0) {
            return 0;
        }
        return (dexterity / DEX_PER_CRIT_BLOCK) * CRIT_PERCENT_PER_BLOCK;
    }

    /**
     * 스탯 맵의 민첩(및 ALL_STAT)을 기준으로 CRITICAL_CHANCE를 가산한다.
     * 덱 최종 스탯·DPS 유효 스탯 모두 동일 규칙으로 호출한다.
     */
    public static void applyToStats(Map<StatType, Integer> stats) {
        int bonus = fromDexterity(effectiveDexterity(stats));
        if (bonus > 0) {
            stats.merge(StatType.CRITICAL_CHANCE, bonus, Integer::sum);
        }
    }

    /**
     * DPS 계산용 총 민첩 — 유효 스탯 민첩 + 레벨/보너스 분배(주스탯이 민첩일 때).
     */
    public static int resolveTotalDexterity(
            Map<StatType, Integer> effectiveStats,
            StatType resolvedMainStat,
            int levelStat,
            BonusStatTarget bonusTarget,
            int bonusAmount) {
        int dex = effectiveDexterity(effectiveStats);
        if (resolvedMainStat == StatType.DEXTERITY) {
            dex += levelStat;
            if (bonusTarget != BonusStatTarget.VITALITY) {
                dex += bonusAmount;
            }
        }
        return dex;
    }

    /** 민첩 + 모든능력치(ALL_STAT) 합산 — UI 최종 스탯 표시와 동일하게 처리 */
    static int effectiveDexterity(Map<StatType, Integer> stats) {
        return stats.getOrDefault(StatType.DEXTERITY, 0)
                + stats.getOrDefault(StatType.ALL_STAT, 0);
    }
}
