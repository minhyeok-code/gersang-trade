package org.example.gersangtrade.watchlist.service;

import org.example.gersangtrade.domain.user.enums.SetComposition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WatchKeyBuilderTest {

    // ── itemKey ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("itemKey_주술없음_ITEM:{id}")
    void itemKey_주술없음() {
        assertThat(WatchKeyBuilder.itemKey(42L)).isEqualTo("ITEM:42");
    }

    @Test
    @DisplayName("itemKey_ritualMark_있음_ITEM:{id}:RITUAL:{mark}")
    void itemKey_주술있음() {
        assertThat(WatchKeyBuilder.itemKey(7L, "<개양>")).isEqualTo("ITEM:7:RITUAL:<개양>");
    }

    @Test
    @DisplayName("itemKey_ritualMark_null이면_주술없는키")
    void itemKey_ritualMark_null() {
        assertThat(WatchKeyBuilder.itemKey(7L, null)).isEqualTo("ITEM:7");
    }

    @Test
    @DisplayName("itemKey_ritualMark_빈문자열이면_주술없는키")
    void itemKey_ritualMark_blank() {
        assertThat(WatchKeyBuilder.itemKey(7L, "  ")).isEqualTo("ITEM:7");
    }

    // ── setKey ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("setKey_주술있음_마크포함")
    void setKey_주술있음() {
        assertThat(WatchKeyBuilder.setKey(1L, SetComposition.FULL, 3, "<개양>"))
                .isEqualTo("SET:1:COMP:FULL:RC:3:MARK:<개양>");
    }

    @Test
    @DisplayName("setKey_주술없음_MARK:NONE")
    void setKey_주술없음_MARK_NONE() {
        assertThat(WatchKeyBuilder.setKey(1L, SetComposition.FULL, 0, null))
                .isEqualTo("SET:1:COMP:FULL:RC:0:MARK:NONE");
    }

    @Test
    @DisplayName("setKey_RC있지만mark_null이면_MARK:NONE")
    void setKey_RC있지만mark_null() {
        // ritualCount > 0이지만 mark가 null → NONE
        assertThat(WatchKeyBuilder.setKey(1L, SetComposition.FULL, 3, null))
                .isEqualTo("SET:1:COMP:FULL:RC:3:MARK:NONE");
    }

    @Test
    @DisplayName("setKey_BANSSANG이면_ritual_강제초기화")
    void setKey_BANSSANG_ritual_초기화() {
        assertThat(WatchKeyBuilder.setKey(5L, SetComposition.BANSSANG, 3, "<개양>"))
                .isEqualTo("SET:5:COMP:BANSSANG:RC:0:MARK:NONE");
    }

    @Test
    @DisplayName("setKey_FULL_BANSSANG은_ritual_유지")
    void setKey_FULL_BANSSANG_ritual_유지() {
        assertThat(WatchKeyBuilder.setKey(5L, SetComposition.FULL_BANSSANG, 3, "<북두칠성_개양>"))
                .isEqualTo("SET:5:COMP:FULL_BANSSANG:RC:3:MARK:<북두칠성_개양>");
    }

    @Test
    @DisplayName("setKey_FULL_BANSSANG_주술없음_MARK:NONE")
    void setKey_FULL_BANSSANG_주술없음() {
        assertThat(WatchKeyBuilder.setKey(5L, SetComposition.FULL_BANSSANG, 0, null))
                .isEqualTo("SET:5:COMP:FULL_BANSSANG:RC:0:MARK:NONE");
    }
}
