package org.example.gersangtrade.calculator.service;

import org.example.gersangtrade.calculator.service.LegendGeneralBuffCalculator.BuffKey;
import org.example.gersangtrade.domain.catalog.CharacteristicEffect;
import org.example.gersangtrade.domain.catalog.LegendGeneral;
import org.example.gersangtrade.domain.catalog.LegendGeneralCharacteristic;
import org.example.gersangtrade.domain.catalog.LegendGeneralPassive;
import org.example.gersangtrade.domain.catalog.Mercenary;
import org.example.gersangtrade.domain.catalog.enums.BuffTarget;
import org.example.gersangtrade.domain.catalog.enums.BuffValueType;
import org.example.gersangtrade.domain.catalog.enums.Element;
import org.example.gersangtrade.domain.catalog.enums.LegendGeneralType;
import org.example.gersangtrade.domain.catalog.enums.Nature;
import org.example.gersangtrade.domain.catalog.enums.StatType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * LegendGeneralBuffCalculator 단위 테스트.
 * DB·Spring 컨텍스트 없이 순수 로직만 검증한다.
 */
@DisplayName("LegendGeneralBuffCalculator")
class LegendGeneralBuffCalculatorTest {

    private final LegendGeneralBuffCalculator calculator = new LegendGeneralBuffCalculator();

    // ── 타입 A 패시브 ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("타입 A 패시브 계산")
    class TypeAPassive {

        @Test
        @DisplayName("주몽 레벨 100 → 마법저항 +10%")
        void 주몽_레벨100_마법저항() {
            LegendGeneral lg = lgWithPassive(
                    StatType.MAGIC_RESISTANCE, Element.NONE, BuffValueType.PERCENT_ADD, BuffTarget.ALLY,
                    100, 10f, 2, 1f, null);

            Map<BuffKey, Float> result = calculator.calculate(lg, 100, Map.of());

            assertThat(result).containsEntry(
                    new BuffKey(StatType.MAGIC_RESISTANCE, Element.NONE, BuffTarget.ALLY), 10f);
        }

        @Test
        @DisplayName("주몽 레벨 102 → 마법저항 +11%")
        void 주몽_레벨102_마법저항() {
            LegendGeneral lg = lgWithPassive(
                    StatType.MAGIC_RESISTANCE, Element.NONE, BuffValueType.PERCENT_ADD, BuffTarget.ALLY,
                    100, 10f, 2, 1f, null);

            Map<BuffKey, Float> result = calculator.calculate(lg, 102, Map.of());

            assertThat(result).containsEntry(
                    new BuffKey(StatType.MAGIC_RESISTANCE, Element.NONE, BuffTarget.ALLY), 11f);
        }

        @Test
        @DisplayName("레벨 99 → startLevel 미달, 패시브 없음")
        void 레벨99_패시브없음() {
            LegendGeneral lg = lgWithPassive(
                    StatType.MAGIC_RESISTANCE, Element.NONE, BuffValueType.PERCENT_ADD, BuffTarget.ALLY,
                    100, 10f, 2, 1f, null);

            Map<BuffKey, Float> result = calculator.calculate(lg, 99, Map.of());

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("startValue null → 미확정 패시브 스킵")
        void 미확정_패시브_스킵() {
            LegendGeneral lg = lgWithPassive(
                    StatType.MIN_POWER, Element.NONE, BuffValueType.FLAT, BuffTarget.ALLY,
                    100, null, null, null, null);

            Map<BuffKey, Float> result = calculator.calculate(lg, 150, Map.of());

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("바지라오 레벨 200 → DAMAGE_PERCENT 0.5%")
        void 바지라오_레벨200_데미지증가() {
            LegendGeneral lg = lgWithPassive(
                    StatType.DAMAGE_PERCENT, Element.NONE, BuffValueType.PERCENT_ADD, BuffTarget.ALLY,
                    200, 0.5f, 10, 0.5f, null);

            Map<BuffKey, Float> result = calculator.calculate(lg, 200, Map.of());

            BuffKey key = new BuffKey(StatType.DAMAGE_PERCENT, Element.NONE, BuffTarget.ALLY);
            assertThat(result.get(key)).isCloseTo(0.5f, within(0.001f));
        }

        @Test
        @DisplayName("바지라오 레벨 210 → DAMAGE_PERCENT 1.0%")
        void 바지라오_레벨210_데미지증가() {
            LegendGeneral lg = lgWithPassive(
                    StatType.DAMAGE_PERCENT, Element.NONE, BuffValueType.PERCENT_ADD, BuffTarget.ALLY,
                    200, 0.5f, 10, 0.5f, null);

            Map<BuffKey, Float> result = calculator.calculate(lg, 210, Map.of());

            BuffKey key = new BuffKey(StatType.DAMAGE_PERCENT, Element.NONE, BuffTarget.ALLY);
            assertThat(result.get(key)).isCloseTo(1.0f, within(0.001f));
        }
    }

    // ── 특성 계산 ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("특성 효과 계산")
    class CharacteristicCalc {

        @Test
        @DisplayName("포인트 5 배분 → 레벨5 행만 적용")
        void 포인트5_레벨5행적용() {
            LegendGeneral lg = emptyLg();
            addCharEffect(lg, 0, 5, StatType.MAGIC_RESISTANCE, Element.NONE,
                    BuffValueType.PERCENT_ADD, BuffTarget.ALLY, 5f);
            addCharEffect(lg, 0, 4, StatType.MAGIC_RESISTANCE, Element.NONE,
                    BuffValueType.PERCENT_ADD, BuffTarget.ALLY, 4f);

            Map<BuffKey, Float> result = calculator.calculate(lg, 1, Map.of(0, 5));

            assertThat(result).containsEntry(
                    new BuffKey(StatType.MAGIC_RESISTANCE, Element.NONE, BuffTarget.ALLY), 5f);
        }

        @Test
        @DisplayName("포인트 0 → 해당 특성 버프 없음")
        void 포인트0_버프없음() {
            LegendGeneral lg = emptyLg();
            addCharEffect(lg, 0, 5, StatType.MAGIC_RESISTANCE, Element.NONE,
                    BuffValueType.PERCENT_ADD, BuffTarget.ALLY, 5f);

            Map<BuffKey, Float> result = calculator.calculate(lg, 1, Map.of(0, 0));

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("SELF 타깃은 calculate()에서 제외")
        void SELF_타깃_제외() {
            LegendGeneral lg = emptyLg();
            addCharEffect(lg, 1, 5, StatType.SKILL_DAMAGE_PERCENT, Element.NONE,
                    BuffValueType.PERCENT_ADD, BuffTarget.SELF, 50f);

            Map<BuffKey, Float> result = calculator.calculate(lg, 1, Map.of(1, 5));

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("SELF 타깃은 calculateSelfBuffs()에서 반환")
        void SELF_타깃_selfBuffs_반환() {
            LegendGeneral lg = emptyLg();
            addCharEffect(lg, 1, 5, StatType.SKILL_DAMAGE_PERCENT, Element.NONE,
                    BuffValueType.PERCENT_ADD, BuffTarget.SELF, 50f);

            Map<BuffKey, Float> result = calculator.calculateSelfBuffs(lg, Map.of(1, 5));

            assertThat(result).containsEntry(
                    new BuffKey(StatType.SKILL_DAMAGE_PERCENT, Element.NONE, BuffTarget.SELF), 50f);
        }

        @Test
        @DisplayName("여포 특성1 레벨10 — SKILL_DAMAGE + MAGIC_RESISTANCE(음수)")
        void 여포_특성1_레벨10() {
            LegendGeneral lg = emptyLg();
            addCharEffect(lg, 1, 10, StatType.SKILL_DAMAGE_PERCENT, Element.NONE,
                    BuffValueType.PERCENT_ADD, BuffTarget.SELF,  100f);
            addCharEffect(lg, 1, 10, StatType.MAGIC_RESISTANCE,     Element.NONE,
                    BuffValueType.PERCENT_ADD, BuffTarget.ENEMY, -15f);

            Map<BuffKey, Float> allBuffs  = calculator.calculate(lg, 1, Map.of(1, 10));
            Map<BuffKey, Float> selfBuffs = calculator.calculateSelfBuffs(lg, Map.of(1, 10));

            assertThat(allBuffs).containsEntry(
                    new BuffKey(StatType.MAGIC_RESISTANCE, Element.NONE, BuffTarget.ENEMY), -15f);
            assertThat(allBuffs).doesNotContainKey(
                    new BuffKey(StatType.SKILL_DAMAGE_PERCENT, Element.NONE, BuffTarget.SELF));
            assertThat(selfBuffs).containsEntry(
                    new BuffKey(StatType.SKILL_DAMAGE_PERCENT, Element.NONE, BuffTarget.SELF), 100f);
        }

        @Test
        @DisplayName("악바르 특성0+1 — ENEMY 음수 / ALLY 양수 각각 집계")
        void 악바르_두특성_집계() {
            LegendGeneral lg = emptyLg();
            addCharEffect(lg, 0, 5, StatType.ELEMENT_VALUE, Element.ADAPTIVE,
                    BuffValueType.FLAT, BuffTarget.ENEMY, -2f);
            addCharEffect(lg, 1, 5, StatType.ELEMENT_VALUE, Element.ADAPTIVE,
                    BuffValueType.FLAT, BuffTarget.ALLY, 2f);

            Map<BuffKey, Float> result = calculator.calculate(lg, 1, Map.of(0, 5, 1, 5));

            assertThat(result).containsEntry(
                    new BuffKey(StatType.ELEMENT_VALUE, Element.ADAPTIVE, BuffTarget.ENEMY), -2f);
            assertThat(result).containsEntry(
                    new BuffKey(StatType.ELEMENT_VALUE, Element.ADAPTIVE, BuffTarget.ALLY), 2f);
        }

        @Test
        @DisplayName("레지나 특성0 레벨10 — 지상+공중 데미지 두 effect 집계")
        void 레지나_지상공중_효과() {
            LegendGeneral lg = emptyLg();
            addCharEffect(lg, 0, 10, StatType.DAMAGE_PERCENT_GROUND, Element.NONE,
                    BuffValueType.PERCENT_ADD, BuffTarget.ALLY, 7f);
            addCharEffect(lg, 0, 10, StatType.DAMAGE_PERCENT_AIR, Element.NONE,
                    BuffValueType.PERCENT_ADD, BuffTarget.ALLY, 10f);

            Map<BuffKey, Float> result = calculator.calculate(lg, 1, Map.of(0, 10));

            assertThat(result).containsEntry(
                    new BuffKey(StatType.DAMAGE_PERCENT_GROUND, Element.NONE, BuffTarget.ALLY), 7f);
            assertThat(result).containsEntry(
                    new BuffKey(StatType.DAMAGE_PERCENT_AIR, Element.NONE, BuffTarget.ALLY), 10f);
        }

        @Test
        @DisplayName("같은 BuffKey 효과가 여러 특성에서 오면 합산")
        void 두특성_같은BuffKey_합산() {
            LegendGeneral lg = emptyLg();
            addCharEffect(lg, 0, 3, StatType.MAGIC_RESISTANCE, Element.NONE,
                    BuffValueType.PERCENT_ADD, BuffTarget.ENEMY, -3f);
            addCharEffect(lg, 1, 3, StatType.MAGIC_RESISTANCE, Element.NONE,
                    BuffValueType.PERCENT_ADD, BuffTarget.ENEMY, -3f);

            Map<BuffKey, Float> result = calculator.calculate(lg, 1, Map.of(0, 3, 1, 3));

            assertThat(result).containsEntry(
                    new BuffKey(StatType.MAGIC_RESISTANCE, Element.NONE, BuffTarget.ENEMY), -6f);
        }

        @Test
        @DisplayName("pointsMap에 없는 특성 인덱스는 포인트 0으로 처리 → 버프 없음")
        void 포인트맵에없는인덱스_버프없음() {
            LegendGeneral lg = emptyLg();
            addCharEffect(lg, 0, 5, StatType.MAGIC_RESISTANCE, Element.NONE,
                    BuffValueType.PERCENT_ADD, BuffTarget.ALLY, 5f);

            Map<BuffKey, Float> result = calculator.calculate(lg, 1, Map.of());

            assertThat(result).isEmpty();
        }
    }

    // ── 픽스처 헬퍼 ─────────────────────────────────────────────────────────────

    private static LegendGeneral emptyLg() {
        Mercenary merc = Mercenary.builder().name("테스트").nature(Nature.FIRE).comingSoon(false).build();
        return LegendGeneral.builder().mercenary(merc).type(LegendGeneralType.TYPE_B).build();
    }

    private static LegendGeneral lgWithPassive(StatType statType, Element element,
                                                BuffValueType valueType, BuffTarget target,
                                                Integer startLevel, Float startValue,
                                                Integer incrementPerLevels, Float incrementValue,
                                                Float maxValue) {
        Mercenary merc = Mercenary.builder().name("테스트").nature(Nature.FIRE).comingSoon(false).build();
        LegendGeneral lg = LegendGeneral.builder().mercenary(merc).type(LegendGeneralType.TYPE_A).build();
        lg.getPassives().add(
                LegendGeneralPassive.builder()
                        .legendGeneral(lg)
                        .statType(statType).element(element).valueType(valueType).target(target)
                        .startLevel(startLevel).startValue(startValue)
                        .incrementPerLevels(incrementPerLevels).incrementValue(incrementValue)
                        .maxValue(maxValue)
                        .build());
        return lg;
    }

    private static void addCharEffect(LegendGeneral lg, int charIndex, int level,
                                       StatType statType, Element element,
                                       BuffValueType valueType, BuffTarget target, float value) {
        LegendGeneralCharacteristic row = LegendGeneralCharacteristic.builder()
                .legendGeneral(lg).characteristicIndex(charIndex).level(level).build();
        row.getEffects().add(
                CharacteristicEffect.builder()
                        .characteristic(row)
                        .statType(statType).element(element).valueType(valueType)
                        .target(target).value(value)
                        .build());
        lg.getCharacteristics().add(row);
    }
}
