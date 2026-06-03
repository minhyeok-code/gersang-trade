package org.example.gersangtrade.catalog.seeder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gersangtrade.catalog.repository.GahoLevelStatRepository;
import org.example.gersangtrade.domain.catalog.GahoLevelStat;
import org.example.gersangtrade.domain.catalog.enums.StatType;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 가호 레벨별 스탯 초기 데이터 시딩 (65행).
 * lv1~10:  MAIN_STAT_FLAT만.
 * lv11~15: MAIN_STAT_FLAT + DAMAGE_PERCENT.
 * lv16~30: MAIN_STAT_FLAT + DAMAGE_PERCENT + ELEMENT_VALUE.
 */
@Slf4j
@Component
@Order(13)
@RequiredArgsConstructor
public class GahoLevelSeeder implements ApplicationRunner {

    private final GahoLevelStatRepository repository;

    private record Row(int level, StatType statType, int value) {}

    private static final Row[] ROWS = {
            new Row(1,  StatType.MAIN_STAT_FLAT,  25),
            new Row(2,  StatType.MAIN_STAT_FLAT,  50),
            new Row(3,  StatType.MAIN_STAT_FLAT,  75),
            new Row(4,  StatType.MAIN_STAT_FLAT, 100),
            new Row(5,  StatType.MAIN_STAT_FLAT, 125),
            new Row(6,  StatType.MAIN_STAT_FLAT, 150),
            new Row(7,  StatType.MAIN_STAT_FLAT, 175),
            new Row(8,  StatType.MAIN_STAT_FLAT, 200),
            new Row(9,  StatType.MAIN_STAT_FLAT, 225),
            new Row(10, StatType.MAIN_STAT_FLAT, 250),
            new Row(11, StatType.MAIN_STAT_FLAT, 275),
            new Row(11, StatType.DAMAGE_PERCENT,   1),
            new Row(12, StatType.MAIN_STAT_FLAT, 300),
            new Row(12, StatType.DAMAGE_PERCENT,   1),
            new Row(13, StatType.MAIN_STAT_FLAT, 325),
            new Row(13, StatType.DAMAGE_PERCENT,   2),
            new Row(14, StatType.MAIN_STAT_FLAT, 350),
            new Row(14, StatType.DAMAGE_PERCENT,   2),
            new Row(15, StatType.MAIN_STAT_FLAT, 375),
            new Row(15, StatType.DAMAGE_PERCENT,   3),
            new Row(16, StatType.MAIN_STAT_FLAT, 400),
            new Row(16, StatType.DAMAGE_PERCENT,   3),
            new Row(16, StatType.ELEMENT_VALUE,    1),
            new Row(17, StatType.MAIN_STAT_FLAT, 425),
            new Row(17, StatType.DAMAGE_PERCENT,   3),
            new Row(17, StatType.ELEMENT_VALUE,    1),
            new Row(18, StatType.MAIN_STAT_FLAT, 450),
            new Row(18, StatType.DAMAGE_PERCENT,   3),
            new Row(18, StatType.ELEMENT_VALUE,    2),
            new Row(19, StatType.MAIN_STAT_FLAT, 475),
            new Row(19, StatType.DAMAGE_PERCENT,   3),
            new Row(19, StatType.ELEMENT_VALUE,    2),
            new Row(20, StatType.MAIN_STAT_FLAT, 500),
            new Row(20, StatType.DAMAGE_PERCENT,   3),
            new Row(20, StatType.ELEMENT_VALUE,    3),
            new Row(21, StatType.MAIN_STAT_FLAT, 525),
            new Row(21, StatType.DAMAGE_PERCENT,   4),
            new Row(21, StatType.ELEMENT_VALUE,    3),
            new Row(22, StatType.MAIN_STAT_FLAT, 550),
            new Row(22, StatType.DAMAGE_PERCENT,   4),
            new Row(22, StatType.ELEMENT_VALUE,    3),
            new Row(23, StatType.MAIN_STAT_FLAT, 575),
            new Row(23, StatType.DAMAGE_PERCENT,   5),
            new Row(23, StatType.ELEMENT_VALUE,    3),
            new Row(24, StatType.MAIN_STAT_FLAT, 600),
            new Row(24, StatType.DAMAGE_PERCENT,   5),
            new Row(24, StatType.ELEMENT_VALUE,    3),
            new Row(25, StatType.MAIN_STAT_FLAT, 625),
            new Row(25, StatType.DAMAGE_PERCENT,   6),
            new Row(25, StatType.ELEMENT_VALUE,    3),
            new Row(26, StatType.MAIN_STAT_FLAT, 650),
            new Row(26, StatType.DAMAGE_PERCENT,   6),
            new Row(26, StatType.ELEMENT_VALUE,    4),
            new Row(27, StatType.MAIN_STAT_FLAT, 675),
            new Row(27, StatType.DAMAGE_PERCENT,   6),
            new Row(27, StatType.ELEMENT_VALUE,    4),
            new Row(28, StatType.MAIN_STAT_FLAT, 700),
            new Row(28, StatType.DAMAGE_PERCENT,   6),
            new Row(28, StatType.ELEMENT_VALUE,    5),
            new Row(29, StatType.MAIN_STAT_FLAT, 725),
            new Row(29, StatType.DAMAGE_PERCENT,   6),
            new Row(29, StatType.ELEMENT_VALUE,    5),
            new Row(30, StatType.MAIN_STAT_FLAT, 750),
            new Row(30, StatType.DAMAGE_PERCENT,   6),
            new Row(30, StatType.ELEMENT_VALUE,    6),
    };

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (repository.count() > 0) {
            log.debug("가호 레벨 스탯 시딩 skip: 이미 존재");
            return;
        }
        log.info("가호 레벨 스탯 시딩 시작");
        for (Row r : ROWS) {
            repository.save(GahoLevelStat.of(r.level(), r.statType(), r.value()));
        }
        log.info("가호 레벨 스탯 시딩 완료 ({}행)", ROWS.length);
    }
}
