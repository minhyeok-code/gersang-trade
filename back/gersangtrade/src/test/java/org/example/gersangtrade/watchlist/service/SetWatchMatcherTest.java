package org.example.gersangtrade.watchlist.service;

import org.example.gersangtrade.domain.user.enums.SetComposition;
import org.example.gersangtrade.listing.service.SetTitleGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SetWatchMatcherTest {

    private static SetTitleGenerator.WatchInfo info(SetComposition comp, int rc, String mark) {
        return new SetTitleGenerator.WatchInfo(comp, rc, mark);
    }

    // ── matchesSell / matchesBuy (동일 규칙) ─────────────────────────────────

    @Test
    @DisplayName("matchesSell_구성일치_주술없음_일치")
    void matchesSell_구성일치_주술없음_일치() {
        assertThat(SetWatchMatcher.matchesSell(
                SetComposition.FULL, 0, null,
                info(SetComposition.FULL, 0, null)))
                .isTrue();
    }

    @Test
    @DisplayName("matchesSell_구성일치_주술수와마크일치")
    void matchesSell_구성일치_주술수와마크일치() {
        assertThat(SetWatchMatcher.matchesSell(
                SetComposition.FULL, 3, "<개양>",
                info(SetComposition.FULL, 3, "<개양>")))
                .isTrue();
    }

    @Test
    @DisplayName("matchesSell_구성불일치_false")
    void matchesSell_구성불일치_false() {
        assertThat(SetWatchMatcher.matchesSell(
                SetComposition.FULL, 0, null,
                info(SetComposition.BYEON, 0, null)))
                .isFalse();
    }

    @Test
    @DisplayName("matchesSell_주술수불일치_false")
    void matchesSell_주술수불일치_false() {
        assertThat(SetWatchMatcher.matchesSell(
                SetComposition.FULL, 3, "<개양>",
                info(SetComposition.FULL, 2, "<개양>")))
                .isFalse();
    }

    @Test
    @DisplayName("matchesSell_주술마크불일치_false")
    void matchesSell_주술마크불일치_false() {
        assertThat(SetWatchMatcher.matchesSell(
                SetComposition.FULL, 3, "<개양>",
                info(SetComposition.FULL, 3, "<북두칠성_개양>")))
                .isFalse();
    }

    @Test
    @DisplayName("matchesSell_watchRitualCount0이지만번들은주술있음_false")
    void matchesSell_watch주술없음_번들주술있음_false() {
        assertThat(SetWatchMatcher.matchesSell(
                SetComposition.FULL, 0, null,
                info(SetComposition.FULL, 3, "<개양>")))
                .isFalse();
    }

    @Test
    @DisplayName("matchesSell_bundleInfo_null이면_false")
    void matchesSell_bundleInfo_null이면_false() {
        assertThat(SetWatchMatcher.matchesSell(SetComposition.FULL, 0, null, null))
                .isFalse();
    }

    @Test
    @DisplayName("matchesBuy_GAMTU와FULL_BANSSANG_구분")
    void matchesBuy_GAMTU와FULL_BANSSANG_구분() {
        assertThat(SetWatchMatcher.matchesBuy(
                SetComposition.GAMTU, 2, "<북두칠성_천선>",
                info(SetComposition.FULL_BANSSANG, 0, null)))
                .isFalse();

        assertThat(SetWatchMatcher.matchesBuy(
                SetComposition.GAMTU, 2, "<북두칠성_천선>",
                info(SetComposition.GAMTU, 2, "<북두칠성_천선>")))
                .isTrue();
    }
}
