package org.example.gersangtrade.calculator.service;

import org.example.gersangtrade.calculator.dto.request.ResistanceType;
import org.example.gersangtrade.domain.catalog.Mercenary;
import org.example.gersangtrade.domain.catalog.enums.MercenaryType;
import org.example.gersangtrade.domain.catalog.enums.Nature;
import org.example.gersangtrade.domain.deck.UserDeckMember;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DeckResistanceTypeResolver")
class DeckResistanceTypeResolverTest {

    @Test
    @DisplayName("풍속성 사천왕 편성 시 HITTING")
    void windHeavenlyKing_returnsHitting() {
        UserDeckMember member = member(MercenaryType.HEAVENLY_KING, Nature.WIND);
        assertThat(DeckResistanceTypeResolver.resolve(List.of(member))).isEqualTo(ResistanceType.HITTING);
    }

    @Test
    @DisplayName("풍속성 각성사천왕 편성 시 HITTING")
    void windAwakenedHeavenlyKing_returnsHitting() {
        UserDeckMember member = member(MercenaryType.AWAKENED_HEAVENLY_KING, Nature.WIND);
        assertThat(DeckResistanceTypeResolver.resolve(List.of(member))).isEqualTo(ResistanceType.HITTING);
    }

    @Test
    @DisplayName("화속성 사천왕만 있으면 MAGIC")
    void fireHeavenlyKing_returnsMagic() {
        UserDeckMember member = member(MercenaryType.HEAVENLY_KING, Nature.FIRE);
        assertThat(DeckResistanceTypeResolver.resolve(List.of(member))).isEqualTo(ResistanceType.MAGIC);
    }

    @Test
    @DisplayName("사천왕 없으면 MAGIC")
    void noHeavenlyKing_returnsMagic() {
        UserDeckMember member = member(MercenaryType.NORMAL_MERCENARY, Nature.WIND);
        assertThat(DeckResistanceTypeResolver.resolve(List.of(member))).isEqualTo(ResistanceType.MAGIC);
    }

    private static UserDeckMember member(MercenaryType type, Nature nature) {
        Mercenary mercenary = Mercenary.builder()
                .name("test")
                .mercenaryType(type)
                .nature(nature)
                .build();
        return UserDeckMember.builder()
                .deck(null)
                .mercenary(mercenary)
                .build();
    }
}
