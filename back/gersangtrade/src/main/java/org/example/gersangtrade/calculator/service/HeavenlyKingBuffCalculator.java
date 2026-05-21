package org.example.gersangtrade.calculator.service;

import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.catalog.repository.MercenaryCharacteristicLevelRepository;
import org.example.gersangtrade.catalog.repository.MercenaryCharacteristicRepository;
import org.example.gersangtrade.domain.catalog.Mercenary;
import org.example.gersangtrade.domain.catalog.enums.StatType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * 사천왕(일반/각성) 저항 감소 버프 계산 서비스.
 *
 * <p>저항 감소 대상:
 * <ul>
 *   <li>지국천왕 / 각성 지국천왕 (FIRE)     — 마법저항 감소 (MAGIC_RESISTANCE)
 *   <li>광목천왕 / 각성 광목천왕 (WIND)     — 타격저항 감소 (HITTING_RESISTANCE)
 *   <li>각성 증장천왕 (THUNDER)             — 마법저항 감소 (공중 몬스터 한정, 항상 적용 가정)
 *   <li>각성 다문천왕 (WATER)              — 마법저항 감소
 *   <li>증장천왕 / 다문천왕 (일반)         — 덱버프 없음 (빈 Map 반환)
 * </ul>
 *
 * <p>레벨 적용 방식: 배분된 레벨의 단일 값 사용 (전설장수의 누적 합산과 다름).
 * 예: 광목천왕 충돌 레벨 7 → -9% (1+2+...+7이 아닌 레벨 7의 값 그대로).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HeavenlyKingBuffCalculator {

    private final MercenaryCharacteristicRepository characteristicRepository;
    private final MercenaryCharacteristicLevelRepository levelRepository;

    /**
     * 사천왕 저항 감소 버프를 계산한다.
     *
     * <p>statType이 설정된 레벨 행만 집계하고, statType=null인 스킬 강화 행은 제외한다.
     *
     * @param heavenlyKing        사천왕 용병 (일반/각성 모두 동일하게 처리)
     * @param characteristicLevels 특성 키 → 배분된 레벨 Map (레벨 0이면 집계 제외)
     * @return StatType별 합산 저항 감소 수치 (음수값). 덱버프 없는 사천왕이면 빈 Map.
     */
    public Map<StatType, Float> calculate(Mercenary heavenlyKing,
                                           Map<String, Integer> characteristicLevels) {
        Map<StatType, Float> result = new HashMap<>();

        characteristicRepository.findByMercenaryId(heavenlyKing.getId())
                .forEach(characteristic -> {
                    int allocatedLevel = characteristicLevels.getOrDefault(characteristic.getKey(), 0);
                    if (allocatedLevel <= 0) return;

                    levelRepository.findByCharacteristicId(characteristic.getId()).stream()
                            .filter(l -> l.getStatType() != null)
                            .filter(l -> l.getLevel() != null && l.getLevel() == allocatedLevel)
                            .filter(l -> l.getAmountValue() != null)
                            .forEach(l -> result.merge(l.getStatType(), l.getAmountValue(), Float::sum));
                });

        return result;
    }
}
