package org.example.gersangtrade.deck.service;

import org.example.gersangtrade.domain.catalog.MercenaryCharacteristic;
import org.example.gersangtrade.domain.catalog.enums.CharacteristicApplyType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CharacteristicPointUtilTest {

    @Test
    @DisplayName("일반용병_point2_레벨3_소모6")
    void 일반용병_곱셈() {
        MercenaryCharacteristic ch = mock(MercenaryCharacteristic.class);
        when(ch.getPoint()).thenReturn(2);

        assertThat(CharacteristicPointUtil.selectionCost(ch, 3)).isEqualTo(6);
    }

    @Test
    @DisplayName("전설장수_point_null_레벨5_소모5")
    void 전설장수_레벨합() {
        MercenaryCharacteristic ch = mock(MercenaryCharacteristic.class);
        when(ch.getPoint()).thenReturn(null);

        assertThat(CharacteristicPointUtil.selectionCost(ch, 5)).isEqualTo(5);
    }

    @Test
    @DisplayName("전설장수_point_0_레벨7_소모7")
    void 전설장수_point0() {
        MercenaryCharacteristic ch = MercenaryCharacteristic.builder()
                .mercenary(mock(org.example.gersangtrade.domain.catalog.Mercenary.class))
                .key("lg_char0")
                .name("속사")
                .point(0)
                .applyType(CharacteristicApplyType.NORMAL)
                .build();

        assertThat(CharacteristicPointUtil.selectionCost(ch, 7)).isEqualTo(7);
    }
}
