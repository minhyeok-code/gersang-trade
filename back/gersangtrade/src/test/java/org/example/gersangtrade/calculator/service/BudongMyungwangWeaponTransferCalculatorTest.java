package org.example.gersangtrade.calculator.service;

import org.example.gersangtrade.calculator.dto.request.BonusStatTarget;
import org.example.gersangtrade.calculator.dto.request.MemberDpsInput;
import org.example.gersangtrade.domain.catalog.enums.MercenaryCategory;
import org.example.gersangtrade.calculator.overlay.LoadedMember;
import org.example.gersangtrade.domain.catalog.Mercenary;
import org.example.gersangtrade.domain.catalog.enums.Nature;
import org.example.gersangtrade.domain.catalog.enums.StatType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * BudongMyungwangWeaponTransferCalculator 단위 테스트.
 */
class BudongMyungwangWeaponTransferCalculatorTest {

    private final BudongMyungwangWeaponTransferCalculator calculator =
            new BudongMyungwangWeaponTransferCalculator();

    @Test
    @DisplayName("명왕월_10퍼센트_고급명왕월_15퍼센트")
    void weaponRate() {
        assertThat(BudongMyungwangWeaponTransferCalculator.resolveTransferRatePercent("명왕월"))
                .isEqualTo(OptionalInt.of(10));
        assertThat(BudongMyungwangWeaponTransferCalculator.resolveTransferRatePercent("고급명왕월"))
                .isEqualTo(OptionalInt.of(15));
    }

    @Test
    @DisplayName("올스텟합_기본장비레벨보너스")
    void sourceTotal() {
        int total = BudongMyungwangWeaponTransferCalculator.computeSourceTotal(
                Map.of(StatType.STRENGTH, 850, StatType.DEXTERITY, 100,
                        StatType.VITALITY, 700, StatType.INTELLECT, 100),
                Map.of(StatType.STRENGTH, 50),
                Map.of(),
                MemberBuildStatCalculator.LEVEL_STAT_250,
                200);
        assertThat(total).isEqualTo(850 + 100 + 700 + 100 + 50 + 2256 + 200);
    }

    @Test
    @DisplayName("이전_본인제외_속성용병만_주스텟에_가산")
    void transferRecipients() {
        LoadedMember budong = loadedMember(1L, "부동명왕", MercenaryCategory.MYEONG_KING, Nature.EARTH);
        LoadedMember fireKing = loadedMember(2L, "지국천왕", MercenaryCategory.FOUR_HEAVENLY_KINGS, Nature.FIRE);
        LoadedMember noNature = loadedMember(3L, "무속성", MercenaryCategory.LEGENDARY_GENERAL, null);

        int sourceTotal = 10_000;
        var weapon = new BudongMyungwangWeaponTransferCalculator.WeaponContext(1L, 10);
        Map<Long, MemberDpsInput> inputs = Map.of(
                1L, new MemberDpsInput(1L, 250, BonusStatTarget.MAIN_STAT, 0),
                2L, new MemberDpsInput(2L, 250, BonusStatTarget.MAIN_STAT, 0));

        var result = calculator.compute(
                List.of(budong, fireKing, noNature),
                weapon,
                sourceTotal,
                inputs,
                m -> m.memberId().equals(2L) ? StatType.STRENGTH : StatType.VITALITY);

        assertThat(result.receivedByMemberId()).doesNotContainKey(1L);
        assertThat(result.receivedByMemberId()).doesNotContainKey(3L);
        assertThat(result.receivedByMemberId().get(2L).get(StatType.STRENGTH)).isEqualTo(1000);
    }

    private static LoadedMember loadedMember(
            Long id, String name, MercenaryCategory category, Nature nature) {
        Mercenary merc = mock(Mercenary.class);
        when(merc.getName()).thenReturn(name);
        when(merc.getCategory()).thenReturn(category);
        when(merc.getNature()).thenReturn(nature);
        return new LoadedMember(id, merc, List.of(), List.of());
    }
}
