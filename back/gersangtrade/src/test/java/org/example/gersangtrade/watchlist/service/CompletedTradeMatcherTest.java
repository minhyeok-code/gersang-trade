package org.example.gersangtrade.watchlist.service;

import org.example.gersangtrade.domain.catalog.EquipmentItem;
import org.example.gersangtrade.domain.catalog.EquipmentSet;
import org.example.gersangtrade.domain.catalog.Item;
import org.example.gersangtrade.domain.catalog.enums.EquipmentSlot;
import org.example.gersangtrade.domain.trade.TradeConfirmed;
import org.example.gersangtrade.domain.user.UserWatchTarget;
import org.example.gersangtrade.domain.user.enums.SetComposition;
import org.example.gersangtrade.domain.user.enums.WatchTargetType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CompletedTradeMatcherTest {

    @Test
    @DisplayName("SET_GAMTU_watchKey와_SET_statKey_유연매칭")
    void setGamtuFlexibleMatch() {
        EquipmentSet set = mock(EquipmentSet.class);
        when(set.getId()).thenReturn(245L);

        UserWatchTarget target = UserWatchTarget.builder()
                .targetType(WatchTargetType.SET)
                .watchKey("SET:245:COMP:GAMTU:RC:2:MARK:<북두칠성_천선>")
                .equipmentSet(set)
                .composition(SetComposition.GAMTU)
                .ritualCount(2)
                .ritualMark("<북두칠성_천선>")
                .build();

        TradeConfirmed trade = tradeWithKey("SET:245:COMP:GAMTU:RC:2:MARK:<북두칠성_천선>");

        assertThat(CompletedTradeMatcher.matches(target, trade, Map.of())).isTrue();
    }

    @Test
    @DisplayName("ITEM_단품관심_SET_세트거래_statKey_불일치")
    void itemWatchDoesNotMatchSetBundleTrade() {
        Item armorItem = mock(Item.class);
        when(armorItem.getId()).thenReturn(1002L);

        EquipmentSet set = mock(EquipmentSet.class);
        when(set.getId()).thenReturn(245L);

        EquipmentItem armorPiece = mock(EquipmentItem.class);
        when(armorPiece.getItemId()).thenReturn(1002L);
        when(armorPiece.getSlot()).thenReturn(EquipmentSlot.ARMOR);

        UserWatchTarget target = UserWatchTarget.builder()
                .targetType(WatchTargetType.ITEM)
                .watchKey("ITEM:1002")
                .item(armorItem)
                .build();

        TradeConfirmed gamtu = tradeWithKey("SET:245:COMP:GAMTU:RC:2:MARK:<북두칠성_천선>");
        TradeConfirmed fullBanssang = tradeWithKey("SET:245:COMP:FULL_BANSSANG:RC:0:MARK:NONE");

        assertThat(CompletedTradeMatcher.matches(
                target, gamtu, Map.of(245L, List.of(armorPiece)))).isFalse();
        assertThat(CompletedTradeMatcher.matches(
                target, fullBanssang, Map.of(245L, List.of(armorPiece)))).isFalse();
    }

    @Test
    @DisplayName("ITEM_단품관심_ITEM_statKey_일치")
    void itemWatchMatchesItemStatKey() {
        Item armorItem = mock(Item.class);
        when(armorItem.getId()).thenReturn(1002L);

        UserWatchTarget target = UserWatchTarget.builder()
                .targetType(WatchTargetType.ITEM)
                .watchKey("ITEM:1002")
                .item(armorItem)
                .build();

        assertThat(CompletedTradeMatcher.matches(
                target, tradeWithKey("ITEM:1002"), Map.of())).isTrue();
    }

    @Test
    @DisplayName("SET_GAMTU_watch_주술지정_legacy_ITEM_statKey_불일치")
    void setWatchDoesNotMatchLegacyItemKeyWhenRitualSpecified() {
        EquipmentSet set = mock(EquipmentSet.class);
        when(set.getId()).thenReturn(245L);

        UserWatchTarget target = UserWatchTarget.builder()
                .targetType(WatchTargetType.SET)
                .watchKey("SET:245:COMP:GAMTU:RC:2:MARK:<북두칠성_천선>")
                .equipmentSet(set)
                .composition(SetComposition.GAMTU)
                .ritualCount(2)
                .ritualMark("<북두칠성_천선>")
                .build();

        TradeConfirmed legacy = tradeWithKey("ITEM:1001");
        EquipmentItem helmetPiece = mock(EquipmentItem.class);
        when(helmetPiece.getSlot()).thenReturn(EquipmentSlot.HELMET);
        when(helmetPiece.getItemId()).thenReturn(1001L);

        assertThat(CompletedTradeMatcher.matches(
                target, legacy, Map.of(245L, List.of(helmetPiece)))).isFalse();
    }

    private static TradeConfirmed tradeWithKey(String statKey) {
        TradeConfirmed trade = mock(TradeConfirmed.class);
        when(trade.getId()).thenReturn(1L);
        when(trade.getStatKeySnapshot()).thenReturn(statKey);
        when(trade.getConfirmedAt()).thenReturn(LocalDateTime.now());
        return trade;
    }
}
