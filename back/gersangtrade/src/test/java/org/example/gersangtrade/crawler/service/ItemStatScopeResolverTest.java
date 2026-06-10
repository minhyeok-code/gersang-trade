package org.example.gersangtrade.crawler.service;

import org.example.gersangtrade.domain.catalog.enums.BuffTarget;
import org.example.gersangtrade.domain.catalog.enums.EquipmentSlot;
import org.example.gersangtrade.domain.catalog.enums.StatType;
import org.example.gersangtrade.domain.catalog.enums.StatUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ItemStatScopeResolver 단위 테스트.
 */
class ItemStatScopeResolverTest {

    @Test
    @DisplayName("명왕부_속성값_TALISMAN_ALLY_HEAVENLY_KING")
    void 명왕부_속성값_TALISMAN_ALLY_HEAVENLY_KING() {
        assertThat(ItemStatScopeResolver.resolve(
                "항삼세명왕부", EquipmentSlot.TALISMAN, StatType.ELEMENT_VALUE))
                .isEqualTo(BuffTarget.ALLY_HEAVENLY_KING);
    }

    @Test
    @DisplayName("고급명왕검_불속성값_WEAPON_ALLY_HEAVENLY_KING")
    void 고급명왕검_불속성값_WEAPON_ALLY_HEAVENLY_KING() {
        assertThat(ItemStatScopeResolver.resolve(
                "고급명왕검", EquipmentSlot.WEAPON, StatType.ELEMENT_VALUE, StatUnit.FLAT))
                .isEqualTo(BuffTarget.ALLY_HEAVENLY_KING);
    }

    @Test
    @DisplayName("각성명왕장_동속아군속성_PERCENT_ALLY_SAME_ELEMENT")
    void 각성명왕장_동속아군속성_PERCENT_ALLY_SAME_ELEMENT() {
        assertThat(ItemStatScopeResolver.resolve(
                "각성명왕장", EquipmentSlot.WEAPON, StatType.ELEMENT_VALUE, StatUnit.PERCENT))
                .isEqualTo(BuffTarget.ALLY_SAME_ELEMENT);
    }

    @Test
    @DisplayName("명왕검_힘_WEAPON_SELF")
    void 명왕검_힘_WEAPON_SELF() {
        assertThat(ItemStatScopeResolver.resolve(
                "명왕검", EquipmentSlot.WEAPON, StatType.STRENGTH))
                .isEqualTo(BuffTarget.SELF);
    }

    @Test
    @DisplayName("부동명왕부_속성값_SELF")
    void 부동명왕부_속성값_SELF() {
        assertThat(ItemStatScopeResolver.resolve(
                "부동명왕부", EquipmentSlot.TALISMAN, StatType.ELEMENT_VALUE))
                .isEqualTo(BuffTarget.SELF);
    }

    @Test
    @DisplayName("고급명왕월_속성값_SELF")
    void 고급명왕월_속성값_SELF() {
        assertThat(ItemStatScopeResolver.resolve(
                "고급명왕월", EquipmentSlot.WEAPON, StatType.ELEMENT_VALUE))
                .isEqualTo(BuffTarget.SELF);
    }

    @Test
    @DisplayName("사천왕부_속성값_SELF")
    void 사천왕부_속성값_SELF() {
        assertThat(ItemStatScopeResolver.resolve(
                "증장천왕부", EquipmentSlot.TALISMAN, StatType.ELEMENT_VALUE))
                .isEqualTo(BuffTarget.SELF);
    }

    @Test
    @DisplayName("명왕부_비속성값스탯_SELF")
    void 명왕부_비속성값스탯_SELF() {
        assertThat(ItemStatScopeResolver.resolve(
                "항삼세명왕부", EquipmentSlot.TALISMAN, StatType.VITALITY))
                .isEqualTo(BuffTarget.SELF);
    }

    @Test
    @DisplayName("슬롯_null_SELF")
    void 슬롯_null_SELF() {
        assertThat(ItemStatScopeResolver.resolve(
                "항삼세명왕부", null, StatType.ELEMENT_VALUE))
                .isEqualTo(BuffTarget.SELF);
    }
}
