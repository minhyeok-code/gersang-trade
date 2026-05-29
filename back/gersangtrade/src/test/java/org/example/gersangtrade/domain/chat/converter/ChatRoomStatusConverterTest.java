package org.example.gersangtrade.domain.chat.converter;

import org.example.gersangtrade.domain.chat.enums.ChatRoomStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChatRoomStatusConverterTest {

    private final ChatRoomStatusConverter converter = new ChatRoomStatusConverter();

    @Test
    @DisplayName("legacy_POSTER_CONFIRMED_읽기_AWAITING_PARTNER로_매핑")
    void legacy_POSTER_CONFIRMED_읽기_AWAITING_PARTNER로_매핑() {
        assertThat(converter.convertToEntityAttribute("POSTER_CONFIRMED"))
                .isEqualTo(ChatRoomStatus.AWAITING_PARTNER);
    }

    @Test
    @DisplayName("저장시_AWAITING_PARTNER_문자열_사용")
    void 저장시_AWAITING_PARTNER_문자열_사용() {
        assertThat(converter.convertToDatabaseColumn(ChatRoomStatus.AWAITING_PARTNER))
                .isEqualTo("AWAITING_PARTNER");
    }
}
