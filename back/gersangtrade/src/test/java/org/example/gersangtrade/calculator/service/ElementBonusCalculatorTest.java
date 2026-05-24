package org.example.gersangtrade.calculator.service;

import org.example.gersangtrade.domain.catalog.enums.Element;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ElementBonusCalculatorTest {

    @Test
    @DisplayName("무속성_몬스터_element_null_보정_0")
    void 무속성_몬스터_element_null_보정_0() {
        assertThat(ElementBonusCalculator.calcElementBonus(20, 0, null)).isZero();
    }

    @Test
    @DisplayName("무속성_몬스터_element_NONE_보정_0")
    void 무속성_몬스터_element_NONE_보정_0() {
        assertThat(ElementBonusCalculator.calcElementBonus(20, 0, Element.NONE)).isZero();
    }

    @Test
    @DisplayName("속성_몬스터_보정_공식_적용")
    void 속성_몬스터_보정_공식_적용() {
        // (3×20 - 10) / 2 = 25
        assertThat(ElementBonusCalculator.calcElementBonus(20, 10, Element.FIRE)).isEqualTo(25.0);
    }
}
