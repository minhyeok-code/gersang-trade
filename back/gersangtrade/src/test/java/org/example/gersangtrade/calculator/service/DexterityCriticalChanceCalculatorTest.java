package org.example.gersangtrade.calculator.service;

import org.example.gersangtrade.calculator.dto.request.BonusStatTarget;
import org.example.gersangtrade.domain.catalog.enums.StatType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DexterityCriticalChanceCalculatorTest {

    @Test
    @DisplayName("민첩 1000당 크리티컬확률 2%p")
    void fromDexterity_blocks() {
        assertThat(DexterityCriticalChanceCalculator.fromDexterity(0)).isZero();
        assertThat(DexterityCriticalChanceCalculator.fromDexterity(999)).isZero();
        assertThat(DexterityCriticalChanceCalculator.fromDexterity(1000)).isEqualTo(2);
        assertThat(DexterityCriticalChanceCalculator.fromDexterity(1999)).isEqualTo(2);
        assertThat(DexterityCriticalChanceCalculator.fromDexterity(2500)).isEqualTo(4);
    }

    @Test
    @DisplayName("ALL_STAT은 민첩 환산에 포함")
    void effectiveDexterity_includesAllStat() {
        Map<StatType, Integer> stats = new EnumMap<>(StatType.class);
        stats.put(StatType.DEXTERITY, 600);
        stats.put(StatType.ALL_STAT, 500);

        DexterityCriticalChanceCalculator.applyToStats(stats);

        assertThat(stats.get(StatType.CRITICAL_CHANCE)).isEqualTo(2);
    }

    @Test
    @DisplayName("레벨·보너스 분배 민첩이 크리티컬 계산에 반영")
    void resolveTotalDexterity_levelAndBonus() {
        Map<StatType, Integer> stats = Map.of(StatType.DEXTERITY, 800);

        int totalDex = DexterityCriticalChanceCalculator.resolveTotalDexterity(
                stats,
                StatType.DEXTERITY,
                MemberBuildStatCalculator.LEVEL_STAT_250,
                BonusStatTarget.MAIN_STAT,
                200);

        assertThat(totalDex).isEqualTo(800 + MemberBuildStatCalculator.LEVEL_STAT_250 + 200);
        assertThat(DexterityCriticalChanceCalculator.fromDexterity(totalDex)).isEqualTo(6);
    }
}
