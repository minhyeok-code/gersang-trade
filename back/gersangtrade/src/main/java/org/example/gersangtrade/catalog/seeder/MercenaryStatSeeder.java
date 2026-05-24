package org.example.gersangtrade.catalog.seeder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gersangtrade.catalog.repository.MercenaryRepository;
import org.example.gersangtrade.catalog.repository.MercenaryStatRepository;
import org.example.gersangtrade.domain.catalog.Mercenary;
import org.example.gersangtrade.domain.catalog.MercenaryStat;
import org.example.gersangtrade.domain.catalog.enums.Nature;
import org.example.gersangtrade.domain.catalog.enums.StatType;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 사천왕·각성사천왕·명왕·각성명왕·전설장수 기본 스탯 시딩.
 *
 * <p>저장 대상 StatType:
 * STRENGTH, DEXTERITY, VITALITY, INTELLECT, MIN_POWER, MAX_POWER,
 * ATTACK_POWER = (min + max) / 2 (스킬 계수 coef_atk 계산 기준값)
 *
 * <p>용병이 아직 시딩되지 않은 경우(key 미발견) 해당 항목은 경고 로그 후 스킵한다.
 */
@Slf4j
@Component
@Order(12)
@RequiredArgsConstructor
public class MercenaryStatSeeder implements ApplicationRunner {

    private final MercenaryRepository mercenaryRepository;
    private final MercenaryStatRepository mercenaryStatRepository;

    /** EARTH 속성값 = 5, 나머지 = 20. nature를 기준으로 자동 계산한다. */
    private record StatRow(String key, Nature nature, int str, int dex, int vit, int intel, int minPower, int maxPower) {
        int elementValue() { return nature == Nature.EARTH ? 5 : 20; }
    }

    private static final List<StatRow> STAT_ROWS = List.of(
            // ── 사천왕 ────────────────────────────────────────────────────────────
            new StatRow("jigook",      Nature.FIRE,    600, 150, 500,  250, 180, 200),
            new StatRow("gwangmok",    Nature.WIND,    400,  50, 800,  250, 180, 200),
            new StatRow("jeungjang",   Nature.THUNDER, 300, 750, 450,  200, 180, 200),
            new StatRow("damoon",      Nature.WATER,   300, 100, 450,  650, 180, 200),

            // ── 각성사천왕 ────────────────────────────────────────────────────────
            new StatRow("gakJigook",    Nature.FIRE,    800, 150,  600, 300, 250, 250),
            new StatRow("gakGwangmok",  Nature.WIND,    500,  50, 1000, 300, 250, 250),
            new StatRow("gakJeungjang", Nature.THUNDER, 300, 950,  450, 350, 250, 250),
            new StatRow("gakDamoon",    Nature.WATER,   300, 100,  600, 850, 250, 250),

            // ── 명왕 ──────────────────────────────────────────────────────────────
            new StatRow("hangsamse",     Nature.FIRE,    900, 100,  600, 100, 200, 200),
            new StatRow("daewideok",     Nature.WIND,    250, 100, 1000, 300, 180, 180),
            new StatRow("geumgangyacha", Nature.WATER,   250, 100,  500, 800, 170, 170),
            new StatRow("goondari",      Nature.THUNDER, 200, 1100, 250, 200, 170, 170),
            new StatRow("boodong",       Nature.EARTH,   850, 100,  700, 100, 220, 230),

            // ── 각성명왕 ──────────────────────────────────────────────────────────
            new StatRow("gakHangsamse",     Nature.FIRE,    1100, 100,  600,  150, 250, 250),
            new StatRow("gakDaewideok",     Nature.WIND,     350, 100, 1100,  400, 250, 250),
            new StatRow("gakGeumgangyacha", Nature.WATER,    250, 100,  500, 1100, 250, 250),
            new StatRow("gakGoondari",      Nature.THUNDER,  200, 1100, 300,  350, 250, 250),

            // ── 전설장수 (스펙: mercenary-stat-spec.md 기준) ──────────────────────
            new StatRow("joomong",           Nature.THUNDER, 100, 700, 200, 200, 100, 150),
            new StatRow("chosun",            Nature.WATER,   100,  50, 300, 550, 110, 110),
            new StatRow("maenghwaek",        Nature.WIND,    200,  50, 600, 200, 120, 130),
            new StatRow("nobootsuna",        Nature.FIRE,    500,  50, 300, 200, 130, 130),
            new StatRow("bajirao",           Nature.EARTH,   250,  50, 550, 200, 130, 130),
            new StatRow("choimoosun",        Nature.FIRE,    550,  50, 350, 200, 140, 140),
            new StatRow("hwamokran",         Nature.THUNDER, 100, 800, 200, 200, 130, 160),
            new StatRow("bokuten",           Nature.WIND,    100, 750, 200, 200, 120, 150),
            new StatRow("majo",              Nature.WATER,   100,  50, 350, 550, 110, 110),
            new StatRow("akbar",             Nature.EARTH,   200, 100, 400, 350, 120, 120),
            new StatRow("honggildong",       Nature.WIND,    100,  50, 700, 300, 150, 150),
            new StatRow("yeopo",             Nature.FIRE,    600,  50, 400, 100, 150, 150),
            new StatRow("mochizki-chiyome",  Nature.WATER,    50,  50, 300, 750, 150, 150),
            new StatRow("tjsdlsakstjsdi",    Nature.THUNDER,  50, 750, 250, 250, 150, 150),
            new StatRow("fpwlsktnfxksk",     Nature.EARTH,    50,  50, 450, 600, 150, 150)
    );

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("용병 스탯 시딩 시작 ({}건)", STAT_ROWS.size());
        int seeded = 0;
        for (StatRow row : STAT_ROWS) {
            Mercenary mercenary = mercenaryRepository.findByKey(row.key()).orElse(null);
            if (mercenary == null) {
                log.warn("용병 스탯 시딩 스킵 — key 미발견: {}", row.key());
                continue;
            }
            int atk = (row.minPower() + row.maxPower()) / 2;
            upsert(mercenary, StatType.STRENGTH,      row.str());
            upsert(mercenary, StatType.DEXTERITY,     row.dex());
            upsert(mercenary, StatType.VITALITY,      row.vit());
            upsert(mercenary, StatType.INTELLECT,     row.intel());
            upsert(mercenary, StatType.MIN_POWER,     row.minPower());
            upsert(mercenary, StatType.MAX_POWER,     row.maxPower());
            upsert(mercenary, StatType.ATTACK_POWER,  atk);
            upsert(mercenary, StatType.ELEMENT_VALUE, row.elementValue());
            seeded++;
        }
        log.info("용병 스탯 시딩 완료 ({}/{}건)", seeded, STAT_ROWS.size());
    }

    private void upsert(Mercenary mercenary, StatType statType, int value) {
        mercenaryStatRepository.findByMercenaryIdAndStatKey(mercenary.getId(), statType)
                .ifPresentOrElse(
                        stat -> stat.updateValue(value),
                        () -> mercenaryStatRepository.save(MercenaryStat.builder()
                                .mercenary(mercenary)
                                .statKey(statType)
                                .statValue(value)
                                .build())
                );
    }
}
