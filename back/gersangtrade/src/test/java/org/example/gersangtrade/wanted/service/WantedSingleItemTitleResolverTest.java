package org.example.gersangtrade.wanted.service;

import org.example.gersangtrade.domain.catalog.Item;
import org.example.gersangtrade.domain.catalog.Ritual;
import org.example.gersangtrade.domain.wanted.WantedEquipmentCondition;
import org.example.gersangtrade.domain.wanted.WantedItem;
import org.example.gersangtrade.domain.wanted.WantedRitualCondition;
import org.example.gersangtrade.domain.wanted.enums.PreferredOutcome;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WantedSingleItemTitleResolverTest {

    @Test
    @DisplayName("주술있는_단품_마크접두_제목생성")
    void ritualSingleItemTitle() {
        Item item = mock(Item.class);
        when(item.getName()).thenReturn("대위덕명왕의갑옷");

        WantedItem wantedItem = mock(WantedItem.class);
        when(wantedItem.getItem()).thenReturn(item);

        WantedEquipmentCondition condition = mock(WantedEquipmentCondition.class);
        when(condition.isHasRitual()).thenReturn(true);

        Ritual ritual = mock(Ritual.class);
        when(ritual.getSuccessMark()).thenReturn("<천기>");

        WantedRitualCondition ritualCondition = mock(WantedRitualCondition.class);
        when(ritualCondition.getRitual()).thenReturn(ritual);
        when(ritualCondition.getPreferredOutcome()).thenReturn(PreferredOutcome.SUCCESS);

        assertThat(WantedSingleItemTitleResolver.resolve(
                wantedItem, condition, List.of(ritualCondition)))
                .contains("<천기>대위덕명왕의갑옷");
    }
}
