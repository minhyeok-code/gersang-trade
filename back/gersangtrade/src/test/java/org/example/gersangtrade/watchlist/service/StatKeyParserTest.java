package org.example.gersangtrade.watchlist.service;

import org.example.gersangtrade.domain.user.enums.SetComposition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StatKeyParserTest {

    @Test
    @DisplayName("parseSet_GAMTU_북두칠성_천선")
    void parseSet_GAMTU() {
        var parsed = StatKeyParser.parseSet("SET:245:COMP:GAMTU:RC:2:MARK:<북두칠성_천선>");
        assertThat(parsed).isPresent();
        assertThat(parsed.get().setId()).isEqualTo(245L);
        assertThat(parsed.get().composition()).isEqualTo(SetComposition.GAMTU);
        assertThat(parsed.get().ritualCount()).isEqualTo(2);
        assertThat(parsed.get().mark()).isEqualTo("<북두칠성_천선>");
    }

    @Test
    @DisplayName("parseItem_주술없음")
    void parseItem_plain() {
        var parsed = StatKeyParser.parseItem("ITEM:42");
        assertThat(parsed).isPresent();
        assertThat(parsed.get().itemId()).isEqualTo(42L);
        assertThat(parsed.get().ritualMark()).isNull();
    }
}
