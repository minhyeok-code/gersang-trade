package org.example.gersangtrade.calculator.service;

import org.example.gersangtrade.catalog.repository.MercenaryCharacteristicLevelRepository;
import org.example.gersangtrade.catalog.repository.MercenaryCharacteristicRepository;
import org.example.gersangtrade.domain.catalog.Mercenary;
import org.example.gersangtrade.domain.catalog.MercenaryCharacteristic;
import org.example.gersangtrade.domain.catalog.MercenaryCharacteristicLevel;
import org.example.gersangtrade.domain.catalog.enums.Nature;
import org.example.gersangtrade.domain.catalog.enums.StatType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

/**
 * HeavenlyKingBuffCalculator 단위 테스트.
 *
 * 주의: builder로 생성된 Mercenary/MercenaryCharacteristic은 getId()=null이므로
 * repository mock은 any() 매처를 사용한다.
 * 여러 특성이 관여하는 테스트는 sequential willReturn으로 호출 순서를 제어한다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("HeavenlyKingBuffCalculator")
class HeavenlyKingBuffCalculatorTest {

    @Mock
    private MercenaryCharacteristicRepository characteristicRepository;

    @Mock
    private MercenaryCharacteristicLevelRepository levelRepository;

    @InjectMocks
    private HeavenlyKingBuffCalculator calculator;

    // ── 일반 사천왕 ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("일반 사천왕")
    class NormalHeavenlyKing {

        @Test
        @DisplayName("지국천왕 화상 레벨 10 → 마법저항 -15%")
        void 지국천왕_레벨10_마법저항() {
            MercenaryCharacteristic hwasang = char_("heavenlyKing-jiguk-hwasang");

            given(characteristicRepository.findByMercenaryId(any())).willReturn(List.of(hwasang));
            given(levelRepository.findByCharacteristicId(any())).willReturn(List.of(
                    level(hwasang, "화상 데미지", 10, 200f,  null),
                    level(hwasang, "마법 저항력", 10, -15f,  StatType.MAGIC_RESISTANCE)
            ));

            Map<StatType, Float> result = calculator.calculate(
                    mercenary(Nature.FIRE),
                    Map.of("heavenlyKing-jiguk-hwasang", 10));

            assertThat(result).containsEntry(StatType.MAGIC_RESISTANCE, -15f);
            assertThat(result).doesNotContainKey(StatType.HITTING_RESISTANCE);
        }

        @Test
        @DisplayName("광목천왕 충돌 레벨 7 → 타격저항 -9%")
        void 광목천왕_레벨7_타격저항() {
            MercenaryCharacteristic chungdol = char_("heavenlyKing-gwangmok-chungdol");

            given(characteristicRepository.findByMercenaryId(any())).willReturn(List.of(chungdol));
            given(levelRepository.findByCharacteristicId(any())).willReturn(List.of(
                    level(chungdol, "타격 저항력", 7, -9f, StatType.HITTING_RESISTANCE)
            ));

            Map<StatType, Float> result = calculator.calculate(
                    mercenary(Nature.WIND),
                    Map.of("heavenlyKing-gwangmok-chungdol", 7));

            assertThat(result).containsEntry(StatType.HITTING_RESISTANCE, -9f);
        }

        @Test
        @DisplayName("증장천왕 — 덱버프 없음, 빈 Map")
        void 증장천왕_덱버프없음() {
            MercenaryCharacteristic gamjeon    = char_("heavenlyKing-jeungjang-gamjeon");
            MercenaryCharacteristic chunggyeok = char_("heavenlyKing-jeungjang-chunggyeokpa");

            given(characteristicRepository.findByMercenaryId(any()))
                    .willReturn(List.of(gamjeon, chunggyeok));
            // 두 특성 모두 statType=null → 순서대로 반환
            given(levelRepository.findByCharacteristicId(any()))
                    .willReturn(List.of(level(gamjeon,    "뇌룡격 데미지",  10, 150f, null)))
                    .willReturn(List.of(level(chunggyeok, "3배 피해 확률", 10,  70f, null)));

            Map<StatType, Float> result = calculator.calculate(
                    mercenary(Nature.THUNDER),
                    Map.of("heavenlyKing-jeungjang-gamjeon", 10,
                           "heavenlyKing-jeungjang-chunggyeokpa", 10));

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("레벨 0이면 해당 특성 집계 제외")
        void 레벨0_집계제외() {
            MercenaryCharacteristic hwasang = char_("heavenlyKing-jiguk-hwasang");
            given(characteristicRepository.findByMercenaryId(any())).willReturn(List.of(hwasang));

            Map<StatType, Float> result = calculator.calculate(
                    mercenary(Nature.FIRE),
                    Map.of("heavenlyKing-jiguk-hwasang", 0));

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("특성 키가 Map에 없으면 레벨 0으로 처리 → 집계 제외")
        void 키없으면_레벨0처리() {
            MercenaryCharacteristic hwasang = char_("heavenlyKing-jiguk-hwasang");
            given(characteristicRepository.findByMercenaryId(any())).willReturn(List.of(hwasang));

            Map<StatType, Float> result = calculator.calculate(
                    mercenary(Nature.FIRE), Map.of());

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("화상 데미지 label(statType=null)은 집계에서 제외")
        void statType_null_행은_집계제외() {
            MercenaryCharacteristic hwasang = char_("heavenlyKing-jiguk-hwasang");

            given(characteristicRepository.findByMercenaryId(any())).willReturn(List.of(hwasang));
            given(levelRepository.findByCharacteristicId(any())).willReturn(List.of(
                    level(hwasang, "화상 데미지", 5, 100f, null),               // 스킵 대상
                    level(hwasang, "마법 저항력", 5,  -5f, StatType.MAGIC_RESISTANCE)
            ));

            Map<StatType, Float> result = calculator.calculate(
                    mercenary(Nature.FIRE),
                    Map.of("heavenlyKing-jiguk-hwasang", 5));

            assertThat(result).hasSize(1);
            assertThat(result).containsEntry(StatType.MAGIC_RESISTANCE, -5f);
        }
    }

    // ── 각성 사천왕 ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("각성 사천왕")
    class AwakenedHeavenlyKing {

        @Test
        @DisplayName("각성 지국천왕 화상 레벨 5 → 마법저항 -15%")
        void 각성지국천왕_레벨5_마법저항() {
            MercenaryCharacteristic hwasang = char_("awakenedHK-jiguk-hwasang");

            given(characteristicRepository.findByMercenaryId(any())).willReturn(List.of(hwasang));
            given(levelRepository.findByCharacteristicId(any())).willReturn(List.of(
                    level(hwasang, "화상 데미지", 5, 200f, null),
                    level(hwasang, "마법저항력",  5, -15f, StatType.MAGIC_RESISTANCE)
            ));

            Map<StatType, Float> result = calculator.calculate(
                    mercenary(Nature.FIRE),
                    Map.of("awakenedHK-jiguk-hwasang", 5));

            assertThat(result).containsEntry(StatType.MAGIC_RESISTANCE, -15f);
        }

        @Test
        @DisplayName("각성 광목천왕 충돌 레벨 3 → 타격저항 -6%")
        void 각성광목천왕_레벨3_타격저항() {
            MercenaryCharacteristic chungdol = char_("awakenedHK-gwangmok-chungdol");

            given(characteristicRepository.findByMercenaryId(any())).willReturn(List.of(chungdol));
            given(levelRepository.findByCharacteristicId(any())).willReturn(List.of(
                    level(chungdol, "타격저항력", 3, -6f, StatType.HITTING_RESISTANCE)
            ));

            Map<StatType, Float> result = calculator.calculate(
                    mercenary(Nature.WIND),
                    Map.of("awakenedHK-gwangmok-chungdol", 3));

            assertThat(result).containsEntry(StatType.HITTING_RESISTANCE, -6f);
        }

        @Test
        @DisplayName("각성 증장천왕 충격파 레벨 5 → 마법저항 -15% (공중 몬스터 항상 적용)")
        void 각성증장천왕_충격파_레벨5_마법저항() {
            MercenaryCharacteristic chunggyeok = char_("awakenedHK-jeungjang-chunggyeokpa");

            given(characteristicRepository.findByMercenaryId(any())).willReturn(List.of(chunggyeok));
            given(levelRepository.findByCharacteristicId(any())).willReturn(List.of(
                    level(chunggyeok, "3배 피해 확률",          5,  70f, null),
                    level(chunggyeok, "마법저항 (공중 몬스터)", 5, -15f, StatType.MAGIC_RESISTANCE)
            ));

            Map<StatType, Float> result = calculator.calculate(
                    mercenary(Nature.THUNDER),
                    Map.of("awakenedHK-jeungjang-chunggyeokpa", 5));

            assertThat(result).containsEntry(StatType.MAGIC_RESISTANCE, -15f);
        }

        @Test
        @DisplayName("각성 다문천왕 강화 레벨 5 → 마법저항 -15%")
        void 각성다문천왕_강화_레벨5_마법저항() {
            MercenaryCharacteristic ganghwa = char_("awakenedHK-damun-ganghwa");

            given(characteristicRepository.findByMercenaryId(any())).willReturn(List.of(ganghwa));
            given(levelRepository.findByCharacteristicId(any())).willReturn(List.of(
                    level(ganghwa, "소환수 스탯", 5, 150f, null),
                    level(ganghwa, "마법저항력",  5, -15f, StatType.MAGIC_RESISTANCE)
            ));

            Map<StatType, Float> result = calculator.calculate(
                    mercenary(Nature.WATER),
                    Map.of("awakenedHK-damun-ganghwa", 5));

            assertThat(result).containsEntry(StatType.MAGIC_RESISTANCE, -15f);
        }

        @Test
        @DisplayName("두 특성에서 같은 StatType 감소 — 합산")
        void 두특성_같은StatType_합산() {
            // 가상 시나리오: 두 특성 모두 MAGIC_RESISTANCE 감소
            MercenaryCharacteristic c1 = char_("key-c1");
            MercenaryCharacteristic c2 = char_("key-c2");

            given(characteristicRepository.findByMercenaryId(any())).willReturn(List.of(c1, c2));
            // 첫 번째 characteristic 조회 → c1 레벨, 두 번째 조회 → c2 레벨
            given(levelRepository.findByCharacteristicId(any()))
                    .willReturn(List.of(level(c1, "마법저항력", 3, -6f, StatType.MAGIC_RESISTANCE)))
                    .willReturn(List.of(level(c2, "마법저항력", 3, -6f, StatType.MAGIC_RESISTANCE)));

            Map<StatType, Float> result = calculator.calculate(
                    mercenary(Nature.FIRE),
                    Map.of("key-c1", 3, "key-c2", 3));

            assertThat(result).containsEntry(StatType.MAGIC_RESISTANCE, -12f);
        }
    }

    // ── 픽스처 헬퍼 ─────────────────────────────────────────────────────────

    private static Mercenary mercenary(Nature nature) {
        return Mercenary.builder()
                .name("테스트용병")
                .nature(nature)
                .comingSoon(false)
                .build();
    }

    private static MercenaryCharacteristic char_(String key) {
        return MercenaryCharacteristic.builder()
                .key(key)
                .name("특성")
                .point(1)
                .build();
    }

    private static MercenaryCharacteristicLevel level(
            MercenaryCharacteristic characteristic, String label,
            int lvl, float value, StatType statType) {
        return MercenaryCharacteristicLevel.builder()
                .characteristic(characteristic)
                .label(label)
                .level(lvl)
                .amount((int) value + "%")
                .amountValue(value)
                .statType(statType)
                .build();
    }
}
