package org.example.gersangtrade.watchlist.service;

import org.example.gersangtrade.domain.catalog.EquipmentItem;
import org.example.gersangtrade.domain.catalog.EquipmentSet;
import org.example.gersangtrade.domain.catalog.Item;
import org.example.gersangtrade.domain.catalog.Ritual;
import org.example.gersangtrade.domain.catalog.enums.EquipmentSlot;
import org.example.gersangtrade.domain.listing.BundleEquipmentDetail;
import org.example.gersangtrade.domain.listing.BundleEquipmentRitual;
import org.example.gersangtrade.domain.listing.BundleLine;
import org.example.gersangtrade.domain.listing.enums.RitualOutcome;
import org.example.gersangtrade.domain.wanted.WantedEquipmentCondition;
import org.example.gersangtrade.domain.wanted.WantedItem;
import org.example.gersangtrade.domain.wanted.WantedListing;
import org.example.gersangtrade.domain.wanted.WantedRitualCondition;
import org.example.gersangtrade.domain.wanted.enums.PreferredOutcome;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ItemWatchMatcherTest {

    @Test
    @DisplayName("matchesBuy_세트구성삽니다는_단품관심에서_제외")
    void matchesBuy_세트구성삽니다는_단품관심에서_제외() {
        WantedListing listing = mock(WantedListing.class);
        when(listing.getId()).thenReturn(1L);

        EquipmentSet set = mock(EquipmentSet.class);
        when(set.getId()).thenReturn(100L);
        when(set.getName()).thenReturn("금강야차명왕");

        Item helmet = item(10L, "투구");
        Item armor = item(20L, "갑옷");
        WantedItem wi1 = wantedItem(1L, listing, helmet, 0);
        WantedItem wi2 = wantedItem(2L, listing, armor, 1);

        EquipmentItem ei1 = equipment(10L, set, EquipmentSlot.HELMET);
        EquipmentItem ei2 = equipment(20L, set, EquipmentSlot.ARMOR);

        assertThat(ItemWatchMatcher.matchesBuy(
                10L, null,
                List.of(wi1, wi2),
                Map.of(),
                Map.of(),
                Map.of(10L, ei1, 20L, ei2)))
                .isFalse();
    }

    @Test
    @DisplayName("matchesBuy_단품1건_일치")
    void matchesBuy_단품1건_일치() {
        WantedListing listing = mock(WantedListing.class);
        Item helmet = item(10L, "투구");
        WantedItem wi = wantedItem(1L, listing, helmet, 0);

        assertThat(ItemWatchMatcher.matchesBuy(
                10L, null,
                List.of(wi),
                Map.of(),
                Map.of(),
                Map.of()))
                .isTrue();
    }

    @Test
    @DisplayName("matchesSell_주술마크_일치")
    void matchesSell_주술마크_일치() {
        Item armor = item(20L, "금강야차명왕의갑옷");
        BundleLine line = mock(BundleLine.class);
        when(line.getId()).thenReturn(1L);
        when(line.getItem()).thenReturn(armor);

        BundleEquipmentDetail detail = mock(BundleEquipmentDetail.class);
        when(detail.isHasRitual()).thenReturn(true);

        Ritual ritual = mock(Ritual.class);
        when(ritual.getSuccessMark()).thenReturn("<천선>");

        BundleEquipmentRitual equipmentRitual = mock(BundleEquipmentRitual.class);
        when(equipmentRitual.getRitual()).thenReturn(ritual);
        when(equipmentRitual.getOutcome()).thenReturn(RitualOutcome.SUCCESS);

        assertThat(ItemWatchMatcher.matchesSell(
                20L, "<천선>",
                List.of(line),
                Map.of(1L, detail),
                Map.of(1L, List.of(equipmentRitual))))
                .isTrue();
    }

    @Test
    @DisplayName("matchesSell_주술마크_불일치_false")
    void matchesSell_주술마크_불일치_false() {
        Item armor = item(20L, "금강야차명왕의갑옷");
        BundleLine line = mock(BundleLine.class);
        when(line.getId()).thenReturn(1L);
        when(line.getItem()).thenReturn(armor);

        BundleEquipmentDetail detail = mock(BundleEquipmentDetail.class);
        when(detail.isHasRitual()).thenReturn(true);

        Ritual ritual = mock(Ritual.class);
        when(ritual.getSuccessMark()).thenReturn("<개양>");

        BundleEquipmentRitual equipmentRitual = mock(BundleEquipmentRitual.class);
        when(equipmentRitual.getRitual()).thenReturn(ritual);
        when(equipmentRitual.getOutcome()).thenReturn(RitualOutcome.SUCCESS);

        assertThat(ItemWatchMatcher.matchesSell(
                20L, "<천선>",
                List.of(line),
                Map.of(1L, detail),
                Map.of(1L, List.of(equipmentRitual))))
                .isFalse();
    }

    @Test
    @DisplayName("matchesSell_주술없는관심은_주술없는단품만_일치")
    void matchesSell_주술없는관심() {
        Item armor = item(20L, "금강야차명왕의갑옷");
        BundleLine line = mock(BundleLine.class);
        when(line.getId()).thenReturn(1L);
        when(line.getItem()).thenReturn(armor);

        assertThat(ItemWatchMatcher.matchesSell(
                20L, null,
                List.of(line),
                Map.of(),
                Map.of()))
                .isTrue();
    }

    @Test
    @DisplayName("matchesSell_주술없는관심은_주술있는단품_불일치")
    void matchesSell_주술없는관심_주술있는단품_false() {
        Item armor = item(20L, "금강야차명왕의갑옷");
        BundleLine line = mock(BundleLine.class);
        when(line.getId()).thenReturn(1L);
        when(line.getItem()).thenReturn(armor);

        BundleEquipmentDetail detail = mock(BundleEquipmentDetail.class);
        when(detail.isHasRitual()).thenReturn(true);

        Ritual ritual = mock(Ritual.class);
        when(ritual.getSuccessMark()).thenReturn("<천선>");

        BundleEquipmentRitual equipmentRitual = mock(BundleEquipmentRitual.class);
        when(equipmentRitual.getRitual()).thenReturn(ritual);
        when(equipmentRitual.getOutcome()).thenReturn(RitualOutcome.SUCCESS);

        assertThat(ItemWatchMatcher.matchesSell(
                20L, null,
                List.of(line),
                Map.of(1L, detail),
                Map.of(1L, List.of(equipmentRitual))))
                .isFalse();
    }

    @Test
    @DisplayName("matchesSell_displayName기반_주술마크_일치")
    void matchesSell_displayName기반_주술마크_일치() {
        Item armor = item(20L, "대위덕명왕의갑옷");
        BundleLine line = mock(BundleLine.class);
        when(line.getId()).thenReturn(1L);
        when(line.getItem()).thenReturn(armor);

        BundleEquipmentDetail detail = mock(BundleEquipmentDetail.class);
        when(detail.isHasRitual()).thenReturn(true);

        Ritual ritual = mock(Ritual.class);
        when(ritual.getSuccessMark()).thenReturn(null);
        when(ritual.getDisplayName()).thenReturn("천기");
        when(ritual.getGreatSuccessMark()).thenReturn(null);

        BundleEquipmentRitual equipmentRitual = mock(BundleEquipmentRitual.class);
        when(equipmentRitual.getRitual()).thenReturn(ritual);
        when(equipmentRitual.getOutcome()).thenReturn(RitualOutcome.SUCCESS);

        assertThat(ItemWatchMatcher.matchesSell(
                20L, "<천기>",
                List.of(line),
                Map.of(1L, detail),
                Map.of(1L, List.of(equipmentRitual))))
                .isTrue();
    }

    @Test
    @DisplayName("matchesBuy_주술마크_불일치_false")
    void matchesBuy_주술마크_불일치_false() {
        WantedListing listing = mock(WantedListing.class);
        Item helmet = item(10L, "투구");
        WantedItem wi = wantedItem(1L, listing, helmet, 0);

        WantedEquipmentCondition cond = mock(WantedEquipmentCondition.class);
        when(cond.isHasRitual()).thenReturn(true);

        Ritual ritual = mock(Ritual.class);
        when(ritual.getSuccessMark()).thenReturn("00");
        when(ritual.getGreatSuccessMark()).thenReturn("**");

        WantedRitualCondition rc = mock(WantedRitualCondition.class);
        when(rc.getRitual()).thenReturn(ritual);
        when(rc.getPreferredOutcome()).thenReturn(PreferredOutcome.SUCCESS);

        assertThat(ItemWatchMatcher.matchesBuy(
                10L, "<북두칠성_천선>",
                List.of(wi),
                Map.of(1L, cond),
                Map.of(1L, List.of(rc)),
                Map.of()))
                .isFalse();
    }

    private static Item item(Long id, String name) {
        Item item = mock(Item.class);
        when(item.getId()).thenReturn(id);
        when(item.getName()).thenReturn(name);
        return item;
    }

    private static WantedItem wantedItem(Long id, WantedListing listing, Item item, int sortOrder) {
        WantedItem wi = mock(WantedItem.class);
        when(wi.getId()).thenReturn(id);
        when(wi.getWantedListing()).thenReturn(listing);
        when(wi.getItem()).thenReturn(item);
        when(wi.getSortOrder()).thenReturn(sortOrder);
        return wi;
    }

    private static EquipmentItem equipment(Long itemId, EquipmentSet set, EquipmentSlot slot) {
        EquipmentItem ei = mock(EquipmentItem.class);
        when(ei.getItemId()).thenReturn(itemId);
        when(ei.getEquipmentSet()).thenReturn(set);
        when(ei.getSlot()).thenReturn(slot);
        return ei;
    }
}
