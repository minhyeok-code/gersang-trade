package org.example.gersangtrade.catalog.seeder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gersangtrade.catalog.repository.SpiritRepository;
import org.example.gersangtrade.domain.catalog.Spirit;
import org.example.gersangtrade.domain.catalog.SpiritBuff;
import org.example.gersangtrade.domain.catalog.enums.*;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 정령 25종 초기 데이터 시딩.
 * 애플리케이션 기동 시 1회 실행되며, 이미 존재하는 정령은 건너뛴다.
 * 크롤링 대상 없음 — 게임 데이터 하드코딩.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SpiritSeeder implements ApplicationRunner {

    private final SpiritRepository spiritRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        long count = spiritRepository.count();
        if (count >= 25) {
            log.debug("정령 시딩 skip: 이미 {}개 존재", count);
            return;
        }

        log.info("정령 시딩 시작");
        seedWater();
        seedWind();
        seedFire();
        seedThunder();
        seedEarth();
        log.info("정령 시딩 완료: {}개", spiritRepository.count());
    }

    // ── 헬퍼 ──────────────────────────────────────────────────────────────────

    private void upsert(Nature nature, SpiritGrade grade, String name,
                        String acquireCondition, String specialEffectNote,
                        List<BuffDef> buffDefs) {
        if (spiritRepository.findByNatureAndGrade(nature, grade).isPresent()) {
            log.debug("정령 이미 존재 (skip): {} {}", nature, grade);
            return;
        }

        Spirit spirit = Spirit.builder()
                .name(name)
                .nature(nature)
                .grade(grade)
                .acquireCondition(acquireCondition)
                .specialEffectNote(specialEffectNote)
                .build();

        for (BuffDef d : buffDefs) {
            spirit.getBuffs().add(SpiritBuff.builder()
                    .spirit(spirit)
                    .target(d.target())
                    .element(d.element())
                    .statType(d.statType())
                    .statUnit(d.statUnit())
                    .value(d.value())
                    .build());
        }

        spiritRepository.save(spirit);
    }

    private record BuffDef(BuffTarget target, Element element, StatType statType,
                           StatUnit statUnit, float value) {}

    private static BuffDef buff(BuffTarget t, Element el, StatType st, StatUnit su, float v) {
        return new BuffDef(t, el, st, su, v);
    }

    // ── 물 정령 ────────────────────────────────────────────────────────────────

    private void seedWater() {
        upsert(Nature.WATER, SpiritGrade.LOWER, "말랑이", "푸른색 알(유료)", null,
                List.of(
                        buff(BuffTarget.ENEMY, Element.NONE, StatType.MOVE_SPEED,   StatUnit.PERCENT, -2f),
                        buff(BuffTarget.ENEMY, Element.NONE, StatType.ATTACK_SPEED, StatUnit.PERCENT, -2f)
                ));

        upsert(Nature.WATER, SpiritGrade.MIDDLE, "얼음덩이", "친밀도3000, 물의결정1", null,
                List.of(
                        buff(BuffTarget.ENEMY, Element.NONE, StatType.MOVE_SPEED,   StatUnit.PERCENT, -4f),
                        buff(BuffTarget.ENEMY, Element.NONE, StatType.ATTACK_SPEED, StatUnit.PERCENT, -4f)
                ));

        upsert(Nature.WATER, SpiritGrade.UPPER, "발로", "친밀도10000, 물의결정3, 봉인의서1", null,
                List.of(
                        buff(BuffTarget.ENEMY, Element.NONE, StatType.MOVE_SPEED,   StatUnit.PERCENT, -6f),
                        buff(BuffTarget.ENEMY, Element.NONE, StatType.ATTACK_SPEED, StatUnit.PERCENT, -6f)
                ));

        upsert(Nature.WATER, SpiritGrade.HIGHEST, "어린수룡",
                "친밀도30000, 물의정령석30, 푸른색알, 봉인의서", null,
                List.of(
                        buff(BuffTarget.ENEMY, Element.NONE, StatType.MOVE_SPEED,   StatUnit.PERCENT, -7f),
                        buff(BuffTarget.ENEMY, Element.NONE, StatType.ATTACK_SPEED, StatUnit.PERCENT, -7f),
                        buff(BuffTarget.ALLY,  Element.NONE, StatType.MIN_POWER,    StatUnit.FLAT,    100f),
                        buff(BuffTarget.ALLY,  Element.NONE, StatType.MAX_POWER,    StatUnit.FLAT,    100f)
                ));

        upsert(Nature.WATER, SpiritGrade.LEGEND, "어린 심아리",
                "친밀도50000, 안정된푸른시약, 물의정령옥30, 봉인의서3, 푸른색알", null,
                List.of(
                        buff(BuffTarget.ENEMY,    Element.NONE,     StatType.MOVE_SPEED,    StatUnit.PERCENT, -8f),
                        buff(BuffTarget.ENEMY,    Element.NONE,     StatType.ATTACK_SPEED,  StatUnit.PERCENT, -8f),
                        buff(BuffTarget.ALLY,     Element.ADAPTIVE, StatType.ELEMENT_VALUE, StatUnit.FLAT,     5f),
                        buff(BuffTarget.ALLY,     Element.EARTH,    StatType.ELEMENT_VALUE, StatUnit.FLAT,     2f),
                        buff(BuffTarget.ALLY,     Element.NONE,     StatType.MIN_POWER,     StatUnit.FLAT,   200f),
                        buff(BuffTarget.ALLY,     Element.NONE,     StatType.MAX_POWER,     StatUnit.FLAT,   200f)
                ));
    }

    // ── 바람 정령 ──────────────────────────────────────────────────────────────

    private void seedWind() {
        upsert(Nature.WIND, SpiritGrade.LOWER, "씽씽이", "백은색 알(유료)", null,
                List.of(
                        buff(BuffTarget.ALLY, Element.NONE, StatType.MOVE_SPEED,   StatUnit.PERCENT, 2f),
                        buff(BuffTarget.ALLY, Element.NONE, StatType.ATTACK_SPEED, StatUnit.PERCENT, 2f)
                ));

        upsert(Nature.WIND, SpiritGrade.MIDDLE, "바람돌이", "친밀도3000, 바람의결정1", null,
                List.of(
                        buff(BuffTarget.ALLY, Element.NONE, StatType.MOVE_SPEED,   StatUnit.PERCENT, 4f),
                        buff(BuffTarget.ALLY, Element.NONE, StatType.ATTACK_SPEED, StatUnit.PERCENT, 4f)
                ));

        upsert(Nature.WIND, SpiritGrade.UPPER, "풍백", "친밀도10000, 바람의결정3, 봉인의서1", null,
                List.of(
                        buff(BuffTarget.ALLY, Element.NONE, StatType.MOVE_SPEED,   StatUnit.PERCENT, 6f),
                        buff(BuffTarget.ALLY, Element.NONE, StatType.ATTACK_SPEED, StatUnit.PERCENT, 6f)
                ));

        upsert(Nature.WIND, SpiritGrade.HIGHEST, "어린비호",
                "친밀도30000, 바람의정령석30, 백은색알, 봉인의서", null,
                List.of(
                        buff(BuffTarget.ALLY,  Element.NONE, StatType.MOVE_SPEED,   StatUnit.PERCENT,  8f),
                        buff(BuffTarget.ALLY,  Element.NONE, StatType.ATTACK_SPEED, StatUnit.PERCENT,  8f),
                        buff(BuffTarget.ENEMY, Element.NONE, StatType.MOVE_SPEED,   StatUnit.PERCENT, -2f),
                        buff(BuffTarget.ENEMY, Element.NONE, StatType.ATTACK_SPEED, StatUnit.PERCENT, -2f)
                ));

        upsert(Nature.WIND, SpiritGrade.LEGEND, "어린 각웅",
                "친밀도50000, 안정된초록시약, 바람의정령옥30, 봉인의서3, 백은색알", null,
                List.of(
                        buff(BuffTarget.ALLY,  Element.NONE, StatType.MOVE_SPEED,      StatUnit.PERCENT, 10f),
                        buff(BuffTarget.ALLY,  Element.NONE, StatType.ATTACK_SPEED,    StatUnit.PERCENT, 10f),
                        buff(BuffTarget.ALLY,  Element.NONE, StatType.CRITICAL_CHANCE, StatUnit.PERCENT,  2f),
                        buff(BuffTarget.ENEMY, Element.NONE, StatType.MOVE_SPEED,      StatUnit.PERCENT, -3f),
                        buff(BuffTarget.ENEMY, Element.NONE, StatType.ATTACK_SPEED,    StatUnit.PERCENT, -3f)
                ));
    }

    // ── 불 정령 ────────────────────────────────────────────────────────────────

    private void seedFire() {
        upsert(Nature.FIRE, SpiritGrade.LOWER, "불덩이", "붉은색 알(유료)", null,
                List.of(
                        buff(BuffTarget.ALLY, Element.NONE, StatType.MIN_POWER, StatUnit.FLAT, 100f),
                        buff(BuffTarget.ALLY, Element.NONE, StatType.MAX_POWER, StatUnit.FLAT, 100f)
                ));

        upsert(Nature.FIRE, SpiritGrade.MIDDLE, "불꽃돌이", "친밀도3000, 불의결정1", null,
                List.of(
                        buff(BuffTarget.ALLY, Element.NONE, StatType.MIN_POWER, StatUnit.FLAT, 200f),
                        buff(BuffTarget.ALLY, Element.NONE, StatType.MAX_POWER, StatUnit.FLAT, 200f)
                ));

        upsert(Nature.FIRE, SpiritGrade.UPPER, "불여우", "친밀도10000, 불의결정3, 봉인의서1", null,
                List.of(
                        buff(BuffTarget.ALLY, Element.NONE, StatType.MIN_POWER, StatUnit.FLAT, 300f),
                        buff(BuffTarget.ALLY, Element.NONE, StatType.MAX_POWER, StatUnit.FLAT, 300f)
                ));

        upsert(Nature.FIRE, SpiritGrade.HIGHEST, "어린화룡",
                "친밀도30000, 불의정령석30, 붉은색알, 봉인의서", null,
                List.of(
                        buff(BuffTarget.ALLY, Element.NONE, StatType.MIN_POWER,    StatUnit.FLAT,    500f),
                        buff(BuffTarget.ALLY, Element.NONE, StatType.MAX_POWER,    StatUnit.FLAT,    500f),
                        buff(BuffTarget.ALLY, Element.NONE, StatType.MOVE_SPEED,   StatUnit.PERCENT,   2f),
                        buff(BuffTarget.ALLY, Element.NONE, StatType.ATTACK_SPEED, StatUnit.PERCENT,   2f)
                ));

        upsert(Nature.FIRE, SpiritGrade.LEGEND, "어린 불사모",
                "친밀도50000, 안정된붉은시약, 불의정령옥30, 봉인의서3, 붉은색알", null,
                List.of(
                        buff(BuffTarget.ALLY, Element.NONE, StatType.MIN_POWER,           StatUnit.FLAT,    600f),
                        buff(BuffTarget.ALLY, Element.NONE, StatType.MAX_POWER,           StatUnit.FLAT,    600f),
                        buff(BuffTarget.ALLY, Element.NONE, StatType.ATTACK_SPEED,        StatUnit.PERCENT,   3f),
                        buff(BuffTarget.ALLY, Element.NONE, StatType.MOVE_SPEED,          StatUnit.PERCENT,   3f),
                        buff(BuffTarget.ALLY, Element.NONE, StatType.SKILL_DAMAGE_PERCENT,StatUnit.PERCENT,   5f)
                ));
    }

    // ── 번개 정령 ──────────────────────────────────────────────────────────────

    private void seedThunder() {
        upsert(Nature.THUNDER, SpiritGrade.LOWER, "묘아", "황금색 알(유료)", null,
                List.of(
                        buff(BuffTarget.ALLY, Element.NONE, StatType.MAGIC_RESISTANCE, StatUnit.PERCENT, 10f)
                ));

        upsert(Nature.THUNDER, SpiritGrade.MIDDLE, "말붕이", "친밀도3000, 뇌전의결정1", null,
                List.of(
                        buff(BuffTarget.ALLY, Element.NONE, StatType.MAGIC_RESISTANCE, StatUnit.PERCENT, 15f)
                ));

        upsert(Nature.THUNDER, SpiritGrade.UPPER, "천둥이(지상)",
                "친밀도10000, 뇌전의결정3, 봉인의서1", null,
                List.of(
                        buff(BuffTarget.ALLY, Element.NONE, StatType.MAGIC_RESISTANCE, StatUnit.PERCENT, 20f)
                ));

        upsert(Nature.THUNDER, SpiritGrade.HIGHEST, "어린 록아",
                "친밀도30000, 뇌전의정령석30, 황금색알, 봉인의서", null,
                List.of(
                        buff(BuffTarget.ALLY, Element.NONE, StatType.MAGIC_RESISTANCE,  StatUnit.PERCENT, 25f),
                        buff(BuffTarget.ALLY, Element.NONE, StatType.HITTING_RESISTANCE, StatUnit.PERCENT, 10f)
                ));

        upsert(Nature.THUNDER, SpiritGrade.LEGEND, "어린 전비",
                "친밀도50000, 안정된노란시약, 뇌전의정령옥30, 봉인의서3, 황금색알",
                "ALL_STAT 800은 근사값. 원본: 2초당 전체스텟+100(최대 1500). UI에 ※근사값 표시 필요",
                List.of(
                        buff(BuffTarget.ALLY, Element.NONE, StatType.MAGIC_RESISTANCE,  StatUnit.PERCENT, 30f),
                        buff(BuffTarget.ALLY, Element.NONE, StatType.HITTING_RESISTANCE, StatUnit.PERCENT, 15f),
                        buff(BuffTarget.ALLY, Element.NONE, StatType.ALL_STAT,           StatUnit.FLAT,   800f),
                        buff(BuffTarget.ALLY, Element.NONE, StatType.MP_RECOVERY,        StatUnit.FLAT,    50f)
                ));
    }

    // ── 땅 정령 ────────────────────────────────────────────────────────────────

    private void seedEarth() {
        upsert(Nature.EARTH, SpiritGrade.LOWER, "도치", "초록색 알(유료)", null,
                List.of(
                        buff(BuffTarget.ALLY, Element.NONE, StatType.HITTING_RESISTANCE, StatUnit.PERCENT, 10f)
                ));

        upsert(Nature.EARTH, SpiritGrade.MIDDLE, "아목", "친밀도3000, 땅의결정1", null,
                List.of(
                        buff(BuffTarget.ALLY, Element.NONE, StatType.HITTING_RESISTANCE, StatUnit.PERCENT, 15f)
                ));

        upsert(Nature.EARTH, SpiritGrade.UPPER, "하루방",
                "친밀도10000, 땅의결정3, 봉인의서1", null,
                List.of(
                        buff(BuffTarget.ALLY, Element.NONE, StatType.HITTING_RESISTANCE, StatUnit.PERCENT, 20f)
                ));

        upsert(Nature.EARTH, SpiritGrade.HIGHEST, "어린독룡",
                "친밀도30000, 땅의정령석30, 초록색알, 봉인의서", null,
                List.of(
                        buff(BuffTarget.ALLY, Element.NONE, StatType.HITTING_RESISTANCE, StatUnit.PERCENT, 25f),
                        buff(BuffTarget.ALLY, Element.NONE, StatType.MAGIC_RESISTANCE,   StatUnit.PERCENT, 10f)
                ));

        upsert(Nature.EARTH, SpiritGrade.LEGEND, "어린 토석동",
                "친밀도50000, 안정된갈색시약, 땅의정령옥30, 봉인의서3, 초록색알",
                "타 정령 버프 ×2 적용(공속·이속 제외). SpiritBuffCalculator에서 처리. 이 정령 본인 버프는 ×2 대상 아님",
                List.of(
                        buff(BuffTarget.ALLY, Element.NONE, StatType.MAGIC_RESISTANCE,   StatUnit.PERCENT, 15f),
                        buff(BuffTarget.ALLY, Element.NONE, StatType.HITTING_RESISTANCE, StatUnit.PERCENT, 30f),
                        buff(BuffTarget.ALLY, Element.NONE, StatType.HP_RECOVERY,        StatUnit.FLAT,   500f)
                ));
    }
}
