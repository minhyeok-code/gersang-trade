package org.example.gersangtrade.calculator.service;

import org.example.gersangtrade.domain.catalog.LegendGeneral;
import org.example.gersangtrade.domain.catalog.LegendGeneralPassive;
import org.example.gersangtrade.domain.catalog.enums.BuffTarget;
import org.example.gersangtrade.domain.catalog.enums.Element;
import org.example.gersangtrade.domain.catalog.enums.StatType;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 전설장수 특성 버프 계산 서비스.
 *
 * <p>포인트 적용 방식: 누적 합산이 아닌 단일 레벨 행 값 적용.
 * 예: 특성 0에 포인트 5 배분 → characteristicIndex=0, level=5 행의 effects만 적용.
 */
@Service
public class LegendGeneralBuffCalculator {

    /**
     * ALLY/ENEMY 타깃 효과를 계산한다 (덱 전체 버프용).
     * SELF 타깃은 calculateSelfBuffs()로 별도 조회.
     *
     * @param legendGeneral  전설장수
     * @param mercenaryLevel 해당 전설장수의 현재 레벨 (타입 A 패시브 계산용)
     * @param pointsMap      characteristicIndex → 배분 포인트 Map
     * @return BuffKey별 합산 버프 Map
     */
    public Map<BuffKey, Float> calculate(LegendGeneral legendGeneral,
                                          int mercenaryLevel,
                                          Map<Integer, Integer> pointsMap) {
        Map<BuffKey, Float> result = new HashMap<>();

        // 1. 레벨 기반 패시브 (타입 A만 존재)
        for (LegendGeneralPassive passive : legendGeneral.getPassives()) {
            float value = calcPassiveValue(passive, mercenaryLevel);
            if (value != 0f) {
                result.merge(new BuffKey(passive.getStatType(), passive.getElement(), passive.getTarget()),
                        value, Float::sum);
            }
        }

        // 2. 특성 효과 — 배분 포인트에 해당하는 레벨 행만 적용
        legendGeneral.getCharacteristics().stream()
                .filter(c -> {
                    Integer points = pointsMap.get(c.getCharacteristicIndex());
                    return points != null && points > 0 && c.getLevel() == points;
                })
                .flatMap(c -> c.getEffects().stream())
                .filter(e -> e.getTarget() != BuffTarget.SELF)
                .forEach(e -> result.merge(
                        new BuffKey(e.getStatType(), e.getElement(), e.getTarget()),
                        e.getValue(), Float::sum));

        return result;
    }

    /**
     * 장착 용병 본인(SELF)에게만 적용되는 특성 효과를 계산한다.
     * DPS 계산기에서 해당 용병 dps 계산 시 사용.
     *
     * @param legendGeneral 전설장수
     * @param pointsMap     characteristicIndex → 배분 포인트 Map
     * @return BuffKey(statType, element, SELF)별 합산 버프 Map
     */
    public Map<BuffKey, Float> calculateSelfBuffs(LegendGeneral legendGeneral,
                                                    Map<Integer, Integer> pointsMap) {
        Map<BuffKey, Float> result = new HashMap<>();

        legendGeneral.getCharacteristics().stream()
                .filter(c -> {
                    Integer points = pointsMap.get(c.getCharacteristicIndex());
                    return points != null && points > 0 && c.getLevel() == points;
                })
                .flatMap(c -> c.getEffects().stream())
                .filter(e -> e.getTarget() == BuffTarget.SELF)
                .forEach(e -> result.merge(
                        new BuffKey(e.getStatType(), e.getElement(), e.getTarget()),
                        e.getValue(), Float::sum));

        return result;
    }

    private float calcPassiveValue(LegendGeneralPassive passive, int level) {
        if (passive.getStartLevel() == null || passive.getStartValue() == null
                || passive.getIncrementPerLevels() == null || passive.getIncrementValue() == null) {
            return 0f; // 미확정 데이터 — 스킵
        }
        if (level < passive.getStartLevel()) return 0f;

        int steps = (level - passive.getStartLevel()) / passive.getIncrementPerLevels();
        float value = passive.getStartValue() + steps * passive.getIncrementValue();

        if (passive.getMaxValue() != null) {
            value = Math.min(value, passive.getMaxValue());
        }
        return value;
    }

    /** StatType + Element + BuffTarget 조합 키 */
    public record BuffKey(StatType statType, Element element, BuffTarget target) {}
}
