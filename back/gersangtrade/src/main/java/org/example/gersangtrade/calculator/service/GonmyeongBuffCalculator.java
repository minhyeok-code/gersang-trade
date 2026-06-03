package org.example.gersangtrade.calculator.service;

import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.catalog.repository.GonmyeongLevelStatRepository;
import org.example.gersangtrade.domain.catalog.GonmyeongLevelStat;
import org.example.gersangtrade.domain.catalog.enums.StatType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 공명 레벨 → 스탯 버프 계산기.
 *
 * <p>MAIN_STAT_FLAT: 주인공 전용. 스탯 종류는 PlayerCharacterStatResolver로 결정.
 * <p>DAMAGE_PERCENT:  전체 용병 공통.
 */
@Component
@RequiredArgsConstructor
public class GonmyeongBuffCalculator {

    private final GonmyeongLevelStatRepository repository;

    /**
     * 해당 레벨의 MAIN_STAT_FLAT 수치를 반환한다.
     * lv1~30 모두 존재하므로 empty는 레벨 오류를 의미한다.
     */
    public Optional<Integer> getMainStatFlat(int level) {
        return repository.findByLevelAndStatType(level, StatType.MAIN_STAT_FLAT)
                .map(GonmyeongLevelStat::getValue);
    }

    /**
     * 해당 레벨의 DAMAGE_PERCENT 수치를 반환한다.
     * lv1~14는 empty (DAMAGE_PERCENT 미존재).
     */
    public Optional<Integer> getDamagePercent(int level) {
        return repository.findByLevelAndStatType(level, StatType.DAMAGE_PERCENT)
                .map(GonmyeongLevelStat::getValue);
    }

    /** 해당 레벨의 모든 스탯 행을 반환한다. */
    public List<GonmyeongLevelStat> getStatsByLevel(int level) {
        return repository.findByLevel(level);
    }
}
