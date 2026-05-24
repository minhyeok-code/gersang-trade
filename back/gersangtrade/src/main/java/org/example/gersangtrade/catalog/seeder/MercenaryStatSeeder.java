package org.example.gersangtrade.catalog.seeder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gersangtrade.catalog.repository.MercenaryRepository;
import org.example.gersangtrade.catalog.repository.MercenaryStatRepository;
import org.example.gersangtrade.domain.catalog.Mercenary;
import org.example.gersangtrade.domain.catalog.MercenaryStat;
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

    /** 스탯 시딩 대상 용병 데이터 */
    private record StatRow(String key, int str, int dex, int vit, int intel, int minPower, int maxPower) {}

    private static final List<StatRow> STAT_ROWS = List.of(
            // ── 사천왕 ────────────────────────────────────────────────────────────
            new StatRow("jigook",      600, 150, 500,  250, 180, 200),
            new StatRow("gwangmok",    400,  50, 800,  250, 180, 200),
            new StatRow("jeungjang",   300, 750, 450,  200, 180, 200),
            new StatRow("damoon",      300, 100, 450,  650, 180, 200),

            // ── 각성사천왕 ────────────────────────────────────────────────────────
            new StatRow("gakJigook",    800, 150,  600, 300, 250, 250),
            new StatRow("gakGwangmok",  500,  50, 1000, 300, 250, 250),
            new StatRow("gakJeungjang", 300, 950,  450, 350, 250, 250),
            new StatRow("gakDamoon",    300, 100,  600, 850, 250, 250),

            // ── 명왕 ──────────────────────────────────────────────────────────────
            new StatRow("hangsamse",     900, 100,  600, 100, 200, 200),
            new StatRow("daewideok",     250, 100, 1000, 300, 180, 180),
            new StatRow("geumgangyacha", 250, 100,  500, 800, 170, 170),
            new StatRow("goondari",      200, 1100, 250, 200, 170, 170),
            new StatRow("boodong",       850, 100,  700, 100, 220, 230),

            // ── 각성명왕 ──────────────────────────────────────────────────────────
            new StatRow("gakHangsamse",     1100, 100,  600,  150, 250, 250),
            new StatRow("gakDaewideok",      350, 100, 1100,  400, 250, 250),
            new StatRow("gakGeumgangyacha",  250, 100,  500, 1100, 250, 250),
            new StatRow("gakGoondari",       200, 1100, 300,  350, 250, 250),

            // ── 전설장수 ──────────────────────────────────────────────────────────
            new StatRow("joomong",           100, 700, 200, 200, 100, 150),
            new StatRow("chosun",            100,  50, 300, 550, 110, 110),
            new StatRow("maenghwaek",        200,  50, 600, 200, 120, 130),
            new StatRow("nobootsuna",        500,  50, 300, 200, 130, 130),
            new StatRow("bajirao",           250,  50, 550, 200, 130, 130),
            new StatRow("choimoosun",        550,  50, 350, 200, 140, 140),
            new StatRow("hwamokran",         100, 800, 200, 200, 130, 160),
            new StatRow("bokuten",           100, 750, 200, 200, 120, 150),
            new StatRow("majo",              100,  50, 350, 550, 110, 110),
            new StatRow("akbar",             200, 100, 400, 350, 120, 120),
            new StatRow("honggildong",       100,  50, 700, 300, 150, 150),
            new StatRow("yeopo",             600,  50, 400, 100, 150, 150),
            new StatRow("mochizki-chiyome",   50,  50, 300, 750, 150, 150),
            new StatRow("tjsdlsakstjsdi",     50, 750, 250, 250, 150, 150),
            new StatRow("fpwlsktnfxksk",      50,  50, 450, 600, 150, 150)
    );

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (mercenaryStatRepository.findByMercenaryIdAndStatKey(
                mercenaryRepository.findByKey("jigook")
                        .map(Mercenary::getId).orElse(-1L),
                StatType.STRENGTH).isPresent()) {
            log.debug("용병 스탯 시딩 skip: 이미 존재");
            return;
        }

        log.info("용병 스탯 시딩 시작 ({}건)", STAT_ROWS.size());
        int seeded = 0;
        for (StatRow row : STAT_ROWS) {
            Mercenary mercenary = mercenaryRepository.findByKey(row.key()).orElse(null);
            if (mercenary == null) {
                log.warn("용병 스탯 시딩 스킵 — key 미발견: {}", row.key());
                continue;
            }
            int atk = (row.minPower() + row.maxPower()) / 2;
            upsert(mercenary, StatType.STRENGTH,   row.str());
            upsert(mercenary, StatType.DEXTERITY,  row.dex());
            upsert(mercenary, StatType.VITALITY,   row.vit());
            upsert(mercenary, StatType.INTELLECT,  row.intel());
            upsert(mercenary, StatType.MIN_POWER,  row.minPower());
            upsert(mercenary, StatType.MAX_POWER,  row.maxPower());
            upsert(mercenary, StatType.ATTACK_POWER, atk);
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
