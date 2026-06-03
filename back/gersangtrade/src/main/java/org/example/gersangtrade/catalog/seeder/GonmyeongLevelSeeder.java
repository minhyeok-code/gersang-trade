package org.example.gersangtrade.catalog.seeder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gersangtrade.catalog.repository.GonmyeongLevelStatRepository;
import org.example.gersangtrade.domain.catalog.GonmyeongLevelStat;
import org.example.gersangtrade.domain.catalog.enums.StatType;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 공명 레벨별 스탯 초기 데이터 시딩 (44행).
 * lv1~14: MAIN_STAT_FLAT만.
 * lv15~30: MAIN_STAT_FLAT + DAMAGE_PERCENT.
 */
@Slf4j
@Component
@Order(12)
@RequiredArgsConstructor
public class GonmyeongLevelSeeder implements ApplicationRunner {

    private final GonmyeongLevelStatRepository repository;

    private record Row(int level, StatType statType, int value) {}

    private static final Row[] ROWS = {
            new Row(1,  StatType.MAIN_STAT_FLAT, 100),
            new Row(2,  StatType.MAIN_STAT_FLAT, 110),
            new Row(3,  StatType.MAIN_STAT_FLAT, 120),
            new Row(4,  StatType.MAIN_STAT_FLAT, 130),
            new Row(5,  StatType.MAIN_STAT_FLAT, 150),
            new Row(6,  StatType.MAIN_STAT_FLAT, 160),
            new Row(7,  StatType.MAIN_STAT_FLAT, 170),
            new Row(8,  StatType.MAIN_STAT_FLAT, 180),
            new Row(9,  StatType.MAIN_STAT_FLAT, 190),
            new Row(10, StatType.MAIN_STAT_FLAT, 250),
            new Row(11, StatType.MAIN_STAT_FLAT, 260),
            new Row(12, StatType.MAIN_STAT_FLAT, 270),
            new Row(13, StatType.MAIN_STAT_FLAT, 280),
            new Row(14, StatType.MAIN_STAT_FLAT, 290),
            new Row(15, StatType.MAIN_STAT_FLAT, 300),
            new Row(15, StatType.DAMAGE_PERCENT,  1),
            new Row(16, StatType.MAIN_STAT_FLAT, 310),
            new Row(16, StatType.DAMAGE_PERCENT,  1),
            new Row(17, StatType.MAIN_STAT_FLAT, 320),
            new Row(17, StatType.DAMAGE_PERCENT,  1),
            new Row(18, StatType.MAIN_STAT_FLAT, 330),
            new Row(18, StatType.DAMAGE_PERCENT,  1),
            new Row(19, StatType.MAIN_STAT_FLAT, 340),
            new Row(19, StatType.DAMAGE_PERCENT,  1),
            new Row(20, StatType.MAIN_STAT_FLAT, 400),
            new Row(20, StatType.DAMAGE_PERCENT,  2),
            new Row(21, StatType.MAIN_STAT_FLAT, 410),
            new Row(21, StatType.DAMAGE_PERCENT,  2),
            new Row(22, StatType.MAIN_STAT_FLAT, 420),
            new Row(22, StatType.DAMAGE_PERCENT,  2),
            new Row(23, StatType.MAIN_STAT_FLAT, 430),
            new Row(23, StatType.DAMAGE_PERCENT,  2),
            new Row(24, StatType.MAIN_STAT_FLAT, 440),
            new Row(24, StatType.DAMAGE_PERCENT,  2),
            new Row(25, StatType.MAIN_STAT_FLAT, 450),
            new Row(25, StatType.DAMAGE_PERCENT,  3),
            new Row(26, StatType.MAIN_STAT_FLAT, 460),
            new Row(26, StatType.DAMAGE_PERCENT,  3),
            new Row(27, StatType.MAIN_STAT_FLAT, 470),
            new Row(27, StatType.DAMAGE_PERCENT,  3),
            new Row(28, StatType.MAIN_STAT_FLAT, 480),
            new Row(28, StatType.DAMAGE_PERCENT,  3),
            new Row(29, StatType.MAIN_STAT_FLAT, 490),
            new Row(29, StatType.DAMAGE_PERCENT,  3),
            new Row(30, StatType.MAIN_STAT_FLAT, 550),
            new Row(30, StatType.DAMAGE_PERCENT,  4),
    };

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (repository.count() > 0) {
            log.debug("공명 레벨 스탯 시딩 skip: 이미 존재");
            return;
        }
        log.info("공명 레벨 스탯 시딩 시작");
        for (Row r : ROWS) {
            repository.save(GonmyeongLevelStat.of(r.level(), r.statType(), r.value()));
        }
        log.info("공명 레벨 스탯 시딩 완료 ({}행)", ROWS.length);
    }
}
