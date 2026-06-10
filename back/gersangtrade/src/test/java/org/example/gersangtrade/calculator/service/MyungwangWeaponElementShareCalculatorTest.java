package org.example.gersangtrade.calculator.service;

import org.example.gersangtrade.domain.catalog.Item;
import org.example.gersangtrade.domain.catalog.ItemStat;
import org.example.gersangtrade.domain.catalog.Mercenary;
import org.example.gersangtrade.domain.catalog.enums.BuffTarget;
import org.example.gersangtrade.domain.catalog.enums.Element;
import org.example.gersangtrade.domain.catalog.enums.EquipmentSlot;
import org.example.gersangtrade.domain.catalog.enums.MercenaryCategory;
import org.example.gersangtrade.domain.catalog.enums.Nature;
import org.example.gersangtrade.domain.catalog.enums.StatType;
import org.example.gersangtrade.domain.catalog.enums.StatUnit;
import org.example.gersangtrade.domain.deck.UserDeckMember;
import org.example.gersangtrade.domain.deck.UserDeckMemberSlot;
import org.example.gersangtrade.domain.catalog.EquipmentItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 각성 명왕 무기 속성값 % 공유 계산기 단위 테스트.
 */
class MyungwangWeaponElementShareCalculatorTest {

    private final MyungwangWeaponElementShareCalculator calculator =
            new MyungwangWeaponElementShareCalculator();

    @Test
    @DisplayName("소스속성값_기본장비만_특성제외")
    void sourceElementValue() {
        int ev = MyungwangWeaponElementShareCalculator.computeSourceElementValue(
                Map.of(StatType.ELEMENT_VALUE, 100),
                Map.of(StatType.ELEMENT_VALUE, 50),
                Map.of(StatType.ELEMENT_VALUE, 20));
        assertThat(ev).isEqualTo(130);
    }

    @Test
    @DisplayName("20퍼센트_동속성아군_본인제외")
    void shareRecipients() {
        UserDeckMember wearer = deckMember(1L, MercenaryCategory.MYEONG_KING_AWAKENING, Nature.FIRE);
        UserDeckMember fireKing = deckMember(2L, MercenaryCategory.FOUR_HEAVENLY_KINGS, Nature.FIRE);
        UserDeckMember thunderKing = deckMember(3L, MercenaryCategory.FOUR_HEAVENLY_KINGS, Nature.THUNDER);
        UserDeckMember fireLegend = deckMember(4L, MercenaryCategory.LEGENDARY_GENERAL, Nature.FIRE);

        var ctx = new MyungwangWeaponElementShareCalculator.ShareContext(
                1L, Nature.FIRE, 20, "각성명왕장");

        var result = calculator.computeForDeckMembers(
                List.of(wearer, fireKing, thunderKing, fireLegend),
                List.of(ctx),
                Map.of(1L, 500));

        assertThat(result.receivedElementValueByMemberId()).doesNotContainKey(1L);
        assertThat(result.receivedElementValueByMemberId()).doesNotContainKey(3L);
        assertThat(result.receivedElementValueByMemberId().get(2L)).isEqualTo(100);
        assertThat(result.receivedElementValueByMemberId().get(4L)).isEqualTo(100);
    }

    @Test
    @DisplayName("각성무기_이전율_item_stats에서_조회")
    void resolveRateFromItemStats() {
        UserDeckMember wearer = deckMember(1L, MercenaryCategory.MYEONG_KING_AWAKENING, Nature.FIRE);
        Item item = mock(Item.class);
        when(item.getName()).thenReturn("각성명왕장");
        when(item.getId()).thenReturn(99L);

        EquipmentItem equip = mock(EquipmentItem.class);
        when(equip.getSlot()).thenReturn(EquipmentSlot.WEAPON);
        when(equip.getItem()).thenReturn(item);
        when(equip.getItemId()).thenReturn(99L);

        UserDeckMemberSlot slot = mock(UserDeckMemberSlot.class);
        when(slot.getEquipmentItem()).thenReturn(equip);

        ItemStat shareStat = ItemStat.builder()
                .item(item)
                .statType(StatType.ELEMENT_VALUE)
                .element(Element.ADAPTIVE)
                .value(20)
                .statUnit(StatUnit.PERCENT)
                .scope(BuffTarget.ALLY_SAME_ELEMENT)
                .build();

        var ctx = calculator.findShareContext(
                wearer, List.of(slot), Map.of(99L, List.of(shareStat)));

        assertThat(ctx).isPresent();
        assertThat(ctx.get().ratePercent()).isEqualTo(20);
    }

    private static UserDeckMember deckMember(Long id, MercenaryCategory category, Nature nature) {
        Mercenary merc = mock(Mercenary.class);
        when(merc.getCategory()).thenReturn(category);
        when(merc.getNature()).thenReturn(nature);

        UserDeckMember member = mock(UserDeckMember.class);
        when(member.getId()).thenReturn(id);
        when(member.getMercenary()).thenReturn(merc);
        return member;
    }
}
