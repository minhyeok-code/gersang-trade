package org.example.gersangtrade.catalog.seeder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gersangtrade.catalog.repository.LegendGeneralCharacteristicRepository;
import org.example.gersangtrade.catalog.repository.LegendGeneralRepository;
import org.example.gersangtrade.catalog.repository.MercenaryRepository;
import org.example.gersangtrade.domain.catalog.CharacteristicEffect;
import org.example.gersangtrade.domain.catalog.LegendGeneral;
import org.example.gersangtrade.domain.catalog.LegendGeneralCharacteristic;
import org.example.gersangtrade.domain.catalog.LegendGeneralPassive;
import org.example.gersangtrade.domain.catalog.Mercenary;
import org.example.gersangtrade.domain.catalog.enums.BuffTarget;
import org.example.gersangtrade.domain.catalog.enums.BuffValueType;
import org.example.gersangtrade.domain.catalog.enums.Element;
import org.example.gersangtrade.domain.catalog.enums.LegendGeneralType;
import org.example.gersangtrade.domain.catalog.enums.MercenaryCategory;
import org.example.gersangtrade.domain.catalog.enums.MercenaryType;
import org.example.gersangtrade.domain.catalog.enums.Nature;
import org.example.gersangtrade.domain.catalog.enums.StatType;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 전설장수 15마리 초기 데이터 시딩.
 * 애플리케이션 기동 시 1회 실행. 이미 존재하면 건너뛴다.
 */
@Slf4j
@Component
@Order(10)
@RequiredArgsConstructor
public class LegendGeneralSeeder implements ApplicationRunner {

    private final MercenaryRepository mercenaryRepository;
    private final LegendGeneralRepository legendGeneralRepository;
    private final LegendGeneralCharacteristicRepository characteristicRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (mercenaryRepository.findByName("주몽").isPresent()) {
            log.debug("전설장수 시딩 skip: 이미 존재");
            return;
        }

        log.info("전설장수 시딩 시작");
        // 타입 A
        seedJumong();
        seedMaenghwak();
        seedNobutsuna();
        seedBajirao();
        seedChoseon();
        // 타입 B
        seedBokuten();
        seedAkbareu();
        seedHonggildong();
        seedYeopo();
        seedChiyome();
        seedRejina();
        seedHwamokran();
        seedManseonya();
        seedMajo();
        seedChoimuseon();
        log.info("전설장수 시딩 완료 (15마리)");
    }

    // ── 타입 A ─────────────────────────────────────────────────────────────────

    private void seedJumong() {
        LegendGeneral lg = saveLg("주몽", Nature.FIRE, LegendGeneralType.TYPE_A);

        addPassive(lg, StatType.MAGIC_RESISTANCE, Element.NONE, BuffValueType.PERCENT_ADD, BuffTarget.ALLY,
                100, 10f, 2, 1f, null);

        // 특성 0: 마법저항 ALLY
        float[] c0 = {1, 2, 3, 4, 5, 6, 8, 10, 12, 15};
        for (int lvl = 1; lvl <= 10; lvl++) {
            LegendGeneralCharacteristic row = saveChar(lg, 0, lvl);
            addEffect(row, StatType.MAGIC_RESISTANCE, Element.NONE, BuffValueType.PERCENT_ADD, BuffTarget.ALLY, c0[lvl-1]);
        }

        // 특성 1: 스킬 데미지 SELF
        float[] c1 = {10, 20, 30, 40, 50, 60, 75, 90, 120, 150};
        for (int lvl = 1; lvl <= 10; lvl++) {
            LegendGeneralCharacteristic row = saveChar(lg, 1, lvl);
            addEffect(row, StatType.SKILL_DAMAGE_PERCENT, Element.NONE, BuffValueType.PERCENT_ADD, BuffTarget.SELF, c1[lvl-1]);
        }
    }

    private void seedMaenghwak() {
        LegendGeneral lg = saveLg("맹획", Nature.EARTH, LegendGeneralType.TYPE_A);

        addPassive(lg, StatType.HITTING_RESISTANCE, Element.NONE, BuffValueType.PERCENT_ADD, BuffTarget.ALLY,
                100, 10f, 2, 1f, null);

        // 특성 0: 타격저항 ALLY
        float[] c0 = {1, 2, 3, 4, 5, 6, 8, 10, 12, 15};
        for (int lvl = 1; lvl <= 10; lvl++) {
            LegendGeneralCharacteristic row = saveChar(lg, 0, lvl);
            addEffect(row, StatType.HITTING_RESISTANCE, Element.NONE, BuffValueType.PERCENT_ADD, BuffTarget.ALLY, c0[lvl-1]);
        }

        // 특성 1: 스킬 데미지 SELF
        float[] c1 = {10, 20, 30, 40, 50, 60, 75, 90, 120, 150};
        for (int lvl = 1; lvl <= 10; lvl++) {
            LegendGeneralCharacteristic row = saveChar(lg, 1, lvl);
            addEffect(row, StatType.SKILL_DAMAGE_PERCENT, Element.NONE, BuffValueType.PERCENT_ADD, BuffTarget.SELF, c1[lvl-1]);
        }
    }

    private void seedNobutsuna() {
        LegendGeneral lg = saveLg("노부츠나", Nature.WATER, LegendGeneralType.TYPE_A);

        // 미확정 — null 저장
        addPassive(lg, StatType.MIN_POWER, Element.NONE, BuffValueType.FLAT, BuffTarget.ALLY,
                100, null, null, null, null);
        addPassive(lg, StatType.MAX_POWER, Element.NONE, BuffValueType.FLAT, BuffTarget.ALLY,
                100, null, null, null, null);

        // 특성 0: MIN_POWER + MAX_POWER ALLY (동일 수치)
        float[] c0 = {50, 100, 150, 200, 250, 300, 350, 400, 450, 500};
        for (int lvl = 1; lvl <= 10; lvl++) {
            LegendGeneralCharacteristic row = saveChar(lg, 0, lvl);
            addEffect(row, StatType.MIN_POWER, Element.NONE, BuffValueType.FLAT, BuffTarget.ALLY, c0[lvl-1]);
            addEffect(row, StatType.MAX_POWER, Element.NONE, BuffValueType.FLAT, BuffTarget.ALLY, c0[lvl-1]);
        }

        // 특성 1: 스킬 데미지 + 크리티컬확률 SELF
        float[] sdPct = {10, 20, 30, 40, 50, 60, 70, 80, 90, 100};
        float[] critPct = {4, 8, 12, 16, 20, 24, 28, 32, 36, 40};
        for (int lvl = 1; lvl <= 10; lvl++) {
            LegendGeneralCharacteristic row = saveChar(lg, 1, lvl);
            addEffect(row, StatType.SKILL_DAMAGE_PERCENT, Element.NONE, BuffValueType.PERCENT_ADD, BuffTarget.SELF, sdPct[lvl-1]);
            addEffect(row, StatType.CRITICAL_CHANCE, Element.NONE, BuffValueType.PERCENT_ADD, BuffTarget.SELF, critPct[lvl-1]);
        }
    }

    private void seedBajirao() {
        LegendGeneral lg = saveLg("바지라오", Nature.WIND, LegendGeneralType.TYPE_A);

        addPassive(lg, StatType.DAMAGE_PERCENT, Element.NONE, BuffValueType.PERCENT_ADD, BuffTarget.ALLY,
                200, 0.5f, 10, 0.5f, null);

        // 특성 0: DAMAGE_PERCENT NONE + DAMAGE_PERCENT EARTH ALLY
        float[] c0none  = {0.5f, 1.0f, 1.5f, 2.0f, 2.5f, 3.0f, 3.5f, 4.0f, 5.0f, 6.0f};
        float[] c0earth = {1, 1, 1, 2, 2, 3, 3, 4, 4, 5};
        for (int lvl = 1; lvl <= 10; lvl++) {
            LegendGeneralCharacteristic row = saveChar(lg, 0, lvl);
            addEffect(row, StatType.DAMAGE_PERCENT, Element.NONE,  BuffValueType.PERCENT_ADD, BuffTarget.ALLY, c0none[lvl-1]);
            addEffect(row, StatType.DAMAGE_PERCENT, Element.EARTH, BuffValueType.PERCENT_ADD, BuffTarget.ALLY, c0earth[lvl-1]);
        }

        // 특성 1: 스킬 데미지 SELF
        float[] c1 = {10, 20, 30, 40, 50, 60, 70, 80, 90, 100};
        for (int lvl = 1; lvl <= 10; lvl++) {
            LegendGeneralCharacteristic row = saveChar(lg, 1, lvl);
            addEffect(row, StatType.SKILL_DAMAGE_PERCENT, Element.NONE, BuffValueType.PERCENT_ADD, BuffTarget.SELF, c1[lvl-1]);
        }
    }

    private void seedChoseon() {
        LegendGeneral lg = saveLg("초선", Nature.WATER, LegendGeneralType.TYPE_A);

        // 패시브: 마력회복 — 계산기 대상 아님, 저장만
        addPassive(lg, StatType.MP_RECOVERY, Element.NONE, BuffValueType.FLAT, BuffTarget.ALLY,
                100, 10f, 2, 1f, null);

        // 특성 0: 마력회복 관련 — 계산기 제외, 수록 안 함 (행 없음)

        // 특성 1: 스킬 데미지 SELF
        float[] c1 = {10, 20, 30, 40, 50, 60, 75, 90, 120, 150};
        for (int lvl = 1; lvl <= 10; lvl++) {
            LegendGeneralCharacteristic row = saveChar(lg, 1, lvl);
            addEffect(row, StatType.SKILL_DAMAGE_PERCENT, Element.NONE, BuffValueType.PERCENT_ADD, BuffTarget.SELF, c1[lvl-1]);
        }
    }

    // ── 타입 B ─────────────────────────────────────────────────────────────────

    private void seedBokuten() {
        LegendGeneral lg = saveLg("보쿠텐", Nature.THUNDER, LegendGeneralType.TYPE_B);

        // 특성 0: 크리티컬확률 ALLY
        float[] c0 = {1, 2, 2, 3, 3, 4, 4, 5, 6, 7};
        for (int lvl = 1; lvl <= 10; lvl++) {
            LegendGeneralCharacteristic row = saveChar(lg, 0, lvl);
            addEffect(row, StatType.CRITICAL_CHANCE, Element.NONE, BuffValueType.PERCENT_ADD, BuffTarget.ALLY, c0[lvl-1]);
        }

        // 특성 1: 스킬 데미지 SELF
        float[] c1 = {10, 20, 30, 40, 50, 60, 75, 90, 120, 150};
        for (int lvl = 1; lvl <= 10; lvl++) {
            LegendGeneralCharacteristic row = saveChar(lg, 1, lvl);
            addEffect(row, StatType.SKILL_DAMAGE_PERCENT, Element.NONE, BuffValueType.PERCENT_ADD, BuffTarget.SELF, c1[lvl-1]);
        }
    }

    private void seedAkbareu() {
        LegendGeneral lg = saveLg("악바르", Nature.FIRE, LegendGeneralType.TYPE_B);

        // 특성 0: 속성값 ADAPTIVE ENEMY (음수)
        float[] c0 = {-1, -1, -2, -2, -2, -3, -3, -4, -4, -5};
        for (int lvl = 1; lvl <= 10; lvl++) {
            LegendGeneralCharacteristic row = saveChar(lg, 0, lvl);
            addEffect(row, StatType.ELEMENT_VALUE, Element.ADAPTIVE, BuffValueType.FLAT, BuffTarget.ENEMY, c0[lvl-1]);
        }

        // 특성 1: 속성값 ADAPTIVE ALLY (양수)
        float[] c1 = {1, 1, 2, 2, 2, 3, 3, 4, 4, 5};
        for (int lvl = 1; lvl <= 10; lvl++) {
            LegendGeneralCharacteristic row = saveChar(lg, 1, lvl);
            addEffect(row, StatType.ELEMENT_VALUE, Element.ADAPTIVE, BuffValueType.FLAT, BuffTarget.ALLY, c1[lvl-1]);
        }
    }

    private void seedHonggildong() {
        LegendGeneral lg = saveLg("홍길동", Nature.WIND, LegendGeneralType.TYPE_B);

        // 특성 0: 데미지증가 WIND ALLY
        float[] c0 = {1, 1, 2, 2, 3, 3, 4, 5, 6, 7};
        for (int lvl = 1; lvl <= 10; lvl++) {
            LegendGeneralCharacteristic row = saveChar(lg, 0, lvl);
            addEffect(row, StatType.DAMAGE_PERCENT, Element.WIND, BuffValueType.PERCENT_ADD, BuffTarget.ALLY, c0[lvl-1]);
        }

        // 특성 1: 스킬 데미지 SELF + 타격저항 ENEMY (음수)
        float[] sd  = {10, 20, 30, 40, 50, 60, 70, 80, 90, 100};
        float[] hit = {-1, -1, -2, -2, -3, -3, -5, -5, -7, -10};
        for (int lvl = 1; lvl <= 10; lvl++) {
            LegendGeneralCharacteristic row = saveChar(lg, 1, lvl);
            addEffect(row, StatType.SKILL_DAMAGE_PERCENT, Element.NONE, BuffValueType.PERCENT_ADD, BuffTarget.SELF,  sd[lvl-1]);
            addEffect(row, StatType.HITTING_RESISTANCE,   Element.NONE, BuffValueType.PERCENT_ADD, BuffTarget.ENEMY, hit[lvl-1]);
        }
    }

    private void seedYeopo() {
        LegendGeneral lg = saveLg("여포", Nature.FIRE, LegendGeneralType.TYPE_B);

        // 특성 0: 데미지증가 FIRE ALLY
        float[] c0 = {1, 1, 2, 2, 3, 3, 4, 5, 6, 7};
        for (int lvl = 1; lvl <= 10; lvl++) {
            LegendGeneralCharacteristic row = saveChar(lg, 0, lvl);
            addEffect(row, StatType.DAMAGE_PERCENT, Element.FIRE, BuffValueType.PERCENT_ADD, BuffTarget.ALLY, c0[lvl-1]);
        }

        // 특성 1: 스킬 데미지 SELF + 마법저항 ENEMY (음수)
        float[] sd  = {10, 20, 30, 40, 50,  60,  70,  80,  90, 100};
        float[] mag = {-1, -2, -3, -4, -5,  -6,  -8, -10, -12, -15};
        for (int lvl = 1; lvl <= 10; lvl++) {
            LegendGeneralCharacteristic row = saveChar(lg, 1, lvl);
            addEffect(row, StatType.SKILL_DAMAGE_PERCENT, Element.NONE, BuffValueType.PERCENT_ADD, BuffTarget.SELF,  sd[lvl-1]);
            addEffect(row, StatType.MAGIC_RESISTANCE,     Element.NONE, BuffValueType.PERCENT_ADD, BuffTarget.ENEMY, mag[lvl-1]);
        }
    }

    private void seedChiyome() {
        LegendGeneral lg = saveLg("치요메", Nature.WIND, LegendGeneralType.TYPE_B);

        // 특성 0: 마법저항 ENEMY (음수)
        float[] c0 = {-1, -2, -3, -4, -5, -6, -8, -10, -12, -15};
        for (int lvl = 1; lvl <= 10; lvl++) {
            LegendGeneralCharacteristic row = saveChar(lg, 0, lvl);
            addEffect(row, StatType.MAGIC_RESISTANCE, Element.NONE, BuffValueType.PERCENT_ADD, BuffTarget.ENEMY, c0[lvl-1]);
        }

        // 특성 1: 데미지증가 WATER ALLY
        float[] c1 = {1, 1, 2, 2, 3, 3, 4, 5, 6, 7};
        for (int lvl = 1; lvl <= 10; lvl++) {
            LegendGeneralCharacteristic row = saveChar(lg, 1, lvl);
            addEffect(row, StatType.DAMAGE_PERCENT, Element.WATER, BuffValueType.PERCENT_ADD, BuffTarget.ALLY, c1[lvl-1]);
        }
    }

    private void seedRejina() {
        LegendGeneral lg = saveLg("레지나", Nature.EARTH, LegendGeneralType.TYPE_B);

        // 특성 0: 지상데미지증가 + 공중데미지증가 ALLY
        float[] ground = {1, 1, 2, 2, 3,  3,  4,  5,  6,  7};
        float[] air    = {1, 2, 3, 4, 5,  6,  7,  8,  9, 10};
        for (int lvl = 1; lvl <= 10; lvl++) {
            LegendGeneralCharacteristic row = saveChar(lg, 0, lvl);
            addEffect(row, StatType.DAMAGE_PERCENT_GROUND, Element.NONE, BuffValueType.PERCENT_ADD, BuffTarget.ALLY, ground[lvl-1]);
            addEffect(row, StatType.DAMAGE_PERCENT_AIR,    Element.NONE, BuffValueType.PERCENT_ADD, BuffTarget.ALLY, air[lvl-1]);
        }

        // 특성 1: 스킬 데미지 SELF + 공격속도 ALLY
        float[] sd     = {10, 20, 30, 40, 50, 60, 70, 80, 90, 100};
        float[] aspeed = { 2,  3,  4,  5,  7, 12, 15, 30, 40,  50};
        for (int lvl = 1; lvl <= 10; lvl++) {
            LegendGeneralCharacteristic row = saveChar(lg, 1, lvl);
            addEffect(row, StatType.SKILL_DAMAGE_PERCENT, Element.NONE, BuffValueType.PERCENT_ADD, BuffTarget.SELF, sd[lvl-1]);
            addEffect(row, StatType.ATTACK_SPEED,         Element.NONE, BuffValueType.PERCENT_ADD, BuffTarget.ALLY, aspeed[lvl-1]);
        }
    }

    private void seedHwamokran() {
        LegendGeneral lg = saveLg("화목란", Nature.FIRE, LegendGeneralType.TYPE_B);

        // 특성 0: 마법저항 ENEMY (음수)
        float[] c0 = {-1, -2, -3, -4, -5, -6, -7, -8, -9, -10};
        for (int lvl = 1; lvl <= 10; lvl++) {
            LegendGeneralCharacteristic row = saveChar(lg, 0, lvl);
            addEffect(row, StatType.MAGIC_RESISTANCE, Element.NONE, BuffValueType.PERCENT_ADD, BuffTarget.ENEMY, c0[lvl-1]);
        }

        // 특성 1: 스킬 데미지 SELF
        float[] c1 = {10, 20, 30, 40, 50, 60, 75, 90, 120, 150};
        for (int lvl = 1; lvl <= 10; lvl++) {
            LegendGeneralCharacteristic row = saveChar(lg, 1, lvl);
            addEffect(row, StatType.SKILL_DAMAGE_PERCENT, Element.NONE, BuffValueType.PERCENT_ADD, BuffTarget.SELF, c1[lvl-1]);
        }
    }

    private void seedManseonya() {
        LegendGeneral lg = saveLg("만선야", Nature.THUNDER, LegendGeneralType.TYPE_B);

        // 특성 0: 데미지증가 THUNDER ALLY
        float[] c0 = {1, 1, 2, 2, 3, 3, 4, 5, 6, 7};
        for (int lvl = 1; lvl <= 10; lvl++) {
            LegendGeneralCharacteristic row = saveChar(lg, 0, lvl);
            addEffect(row, StatType.DAMAGE_PERCENT, Element.THUNDER, BuffValueType.PERCENT_ADD, BuffTarget.ALLY, c0[lvl-1]);
        }

        // 특성 1: 스킬 데미지 SELF
        float[] c1 = {10, 20, 30, 40, 50, 60, 75, 90, 120, 150};
        for (int lvl = 1; lvl <= 10; lvl++) {
            LegendGeneralCharacteristic row = saveChar(lg, 1, lvl);
            addEffect(row, StatType.SKILL_DAMAGE_PERCENT, Element.NONE, BuffValueType.PERCENT_ADD, BuffTarget.SELF, c1[lvl-1]);
        }
    }

    private void seedMajo() {
        LegendGeneral lg = saveLg("마조", Nature.WATER, LegendGeneralType.TYPE_B);

        // 특성 0: 마법저항 ENEMY (음수)
        float[] c0 = {-1, -2, -3, -4, -5, -6, -8, -10, -12, -15};
        for (int lvl = 1; lvl <= 10; lvl++) {
            LegendGeneralCharacteristic row = saveChar(lg, 0, lvl);
            addEffect(row, StatType.MAGIC_RESISTANCE, Element.NONE, BuffValueType.PERCENT_ADD, BuffTarget.ENEMY, c0[lvl-1]);
        }

        // 특성 1: 스킬 데미지 SELF
        float[] c1 = {10, 20, 30, 40, 50, 60, 75, 90, 120, 150};
        for (int lvl = 1; lvl <= 10; lvl++) {
            LegendGeneralCharacteristic row = saveChar(lg, 1, lvl);
            addEffect(row, StatType.SKILL_DAMAGE_PERCENT, Element.NONE, BuffValueType.PERCENT_ADD, BuffTarget.SELF, c1[lvl-1]);
        }
    }

    private void seedChoimuseon() {
        LegendGeneral lg = saveLg("최무선", Nature.THUNDER, LegendGeneralType.TYPE_B);

        // 특성 0: 마법저항 ENEMY (음수)
        float[] c0 = {-1, -2, -3, -4, -5, -6, -8, -10, -12, -15};
        for (int lvl = 1; lvl <= 10; lvl++) {
            LegendGeneralCharacteristic row = saveChar(lg, 0, lvl);
            addEffect(row, StatType.MAGIC_RESISTANCE, Element.NONE, BuffValueType.PERCENT_ADD, BuffTarget.ENEMY, c0[lvl-1]);
        }

        // 특성 1: 스킬 데미지 SELF (최무선은 레벨 8 값이 95로 다름)
        float[] c1 = {10, 20, 30, 40, 50, 60, 75, 95, 120, 150};
        for (int lvl = 1; lvl <= 10; lvl++) {
            LegendGeneralCharacteristic row = saveChar(lg, 1, lvl);
            addEffect(row, StatType.SKILL_DAMAGE_PERCENT, Element.NONE, BuffValueType.PERCENT_ADD, BuffTarget.SELF, c1[lvl-1]);
        }
    }

    // ── 헬퍼 ────────────────────────────────────────────────────────────────────

    private LegendGeneral saveLg(String name, Nature nature, LegendGeneralType type) {
        Mercenary mercenary = mercenaryRepository.save(
                Mercenary.builder()
                        .name(name)
                        .nature(nature)
                        .mercenaryType(MercenaryType.LEGEND_GENERAL)
                        .category(MercenaryCategory.LEGENDARY_GENERAL)
                        .comingSoon(false)
                        .build()
        );
        return legendGeneralRepository.save(
                LegendGeneral.builder()
                        .mercenary(mercenary)
                        .type(type)
                        .build()
        );
    }

    private LegendGeneralCharacteristic saveChar(LegendGeneral lg, int charIndex, int level) {
        return characteristicRepository.save(
                LegendGeneralCharacteristic.builder()
                        .legendGeneral(lg)
                        .characteristicIndex(charIndex)
                        .level(level)
                        .build()
        );
    }

    private void addPassive(LegendGeneral lg, StatType statType, Element element,
                             BuffValueType valueType, BuffTarget target,
                             Integer startLevel, Float startValue,
                             Integer incrementPerLevels, Float incrementValue, Float maxValue) {
        lg.getPassives().add(
                LegendGeneralPassive.builder()
                        .legendGeneral(lg)
                        .statType(statType)
                        .element(element)
                        .valueType(valueType)
                        .target(target)
                        .startLevel(startLevel)
                        .startValue(startValue)
                        .incrementPerLevels(incrementPerLevels)
                        .incrementValue(incrementValue)
                        .maxValue(maxValue)
                        .build()
        );
    }

    private void addEffect(LegendGeneralCharacteristic row, StatType statType, Element element,
                            BuffValueType valueType, BuffTarget target, float value) {
        row.getEffects().add(
                CharacteristicEffect.builder()
                        .characteristic(row)
                        .statType(statType)
                        .element(element)
                        .valueType(valueType)
                        .target(target)
                        .value(value)
                        .build()
        );
    }
}
