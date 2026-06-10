package org.example.gersangtrade.listing.service;

import org.example.gersangtrade.domain.catalog.Item;
import org.example.gersangtrade.domain.catalog.Ritual;
import org.example.gersangtrade.domain.listing.BundleEquipmentDetail;
import org.example.gersangtrade.domain.listing.BundleEquipmentRitual;
import org.example.gersangtrade.domain.listing.BundleLine;
import org.example.gersangtrade.domain.listing.enums.RitualOutcome;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SingleItemTitleResolverTest {

    @Test
    @DisplayName("주술있는_단품_마크접두_제목생성")
    void ritualSingleItemTitle() {
        BundleLine line = mock(BundleLine.class);
        when(line.getId()).thenReturn(1L);
        Item item = mock(Item.class);
        when(item.getName()).thenReturn("금강야차명왕의갑옷");
        when(line.getItem()).thenReturn(item);

        BundleEquipmentDetail detail = mock(BundleEquipmentDetail.class);
        when(detail.isHasRitual()).thenReturn(true);

        Ritual ritual = mock(Ritual.class);
        when(ritual.getSuccessMark()).thenReturn("<천선>");
        when(ritual.getGreatSuccessMark()).thenReturn("<북두칠성>");

        BundleEquipmentRitual equipmentRitual = mock(BundleEquipmentRitual.class);
        when(equipmentRitual.getRitual()).thenReturn(ritual);
        when(equipmentRitual.getOutcome()).thenReturn(RitualOutcome.SUCCESS);

        var title = SingleItemTitleResolver.resolve(
                List.of(line),
                Map.of(1L, detail),
                Map.of(1L, List.of(equipmentRitual)));

        assertThat(title).contains("<천선>금강야차명왕의갑옷");
    }

    @Test
    @DisplayName("주술없으면_empty")
    void noRitualReturnsEmpty() {
        BundleLine line = mock(BundleLine.class);
        when(line.getId()).thenReturn(1L);

        BundleEquipmentDetail detail = mock(BundleEquipmentDetail.class);
        when(detail.isHasRitual()).thenReturn(false);

        assertThat(SingleItemTitleResolver.resolve(
                List.of(line),
                Map.of(1L, detail),
                Map.of())).isEmpty();
    }
}
