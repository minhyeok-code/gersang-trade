package org.example.gersangtrade.calculator.service;

import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.catalog.repository.GahoLevelStatRepository;
import org.example.gersangtrade.domain.catalog.GahoLevelStat;
import org.example.gersangtrade.domain.catalog.enums.Nature;
import org.example.gersangtrade.domain.catalog.enums.StatType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 가호 레벨 → 스탯 버프 계산기.
 *
 * <p>MAIN_STAT_FLAT: 용병별 주스텟. 속성(Nature) 기준으로 스탯 종류를 개별 결정.
 *    탱커(EARTH·무속성)는 VITALITY.
 * <p>DAMAGE_PERCENT:  전체 용병 공통.
 * <p>ELEMENT_VALUE:   속성값 증가. EARTH 용병은 floor(value / 2) 적용.
 */
@Component
@RequiredArgsConstructor
public class GahoBuffCalculator {

    private final GahoLevelStatRepository repository;

    public Optional<Integer> getMainStatFlat(int level) {
        return repository.findByLevelAndStatType(level, StatType.MAIN_STAT_FLAT)
                .map(GahoLevelStat::getValue);
    }

    public Optional<Integer> getDamagePercent(int level) {
        return repository.findByLevelAndStatType(level, StatType.DAMAGE_PERCENT)
                .map(GahoLevelStat::getValue);
    }

    /**
     * ELEMENT_VALUE 수치를 반환한다.
     * nature가 EARTH이면 floor(value / 2)를 적용한다.
     * lv1~15는 empty.
     */
    public Optional<Integer> getElementValue(int level, Nature nature) {
        return repository.findByLevelAndStatType(level, StatType.ELEMENT_VALUE)
                .map(s -> nature == Nature.EARTH ? s.getValue() / 2 : s.getValue());
    }

    /** 해당 레벨의 모든 스탯 행을 반환한다. */
    public List<GahoLevelStat> getStatsByLevel(int level) {
        return repository.findByLevel(level);
    }

    /**
     * 용병 속성 기준 주스텟 결정.
     * FIRE→STRENGTH, WIND→VITALITY, THUNDER→DEXTERITY, WATER→INTELLECT.
     * EARTH·null(무속성)→VITALITY.
     */
    public StatType resolveMainStat(Nature nature) {
        if (nature == null) return StatType.VITALITY;
        return switch (nature) {
            case FIRE    -> StatType.STRENGTH;
            case WIND    -> StatType.VITALITY;
            case THUNDER -> StatType.DEXTERITY;
            case WATER   -> StatType.INTELLECT;
            case EARTH   -> StatType.VITALITY;
            default      -> StatType.VITALITY;
        };
    }
}
