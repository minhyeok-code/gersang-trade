package org.example.gersangtrade.domain.chat.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.example.gersangtrade.domain.chat.enums.ChatRoomStatus;

/**
 * chat_rooms.status 컬럼 ↔ ChatRoomStatus enum 변환.
 * 구 status 값 POSTER_CONFIRMED 를 AWAITING_PARTNER 로 읽기 호환한다.
 */
@Converter
public class ChatRoomStatusConverter implements AttributeConverter<ChatRoomStatus, String> {

    /** enum 개명 이전 DB에 남아 있을 수 있는 legacy 값 */
    private static final String LEGACY_AWAITING_PARTNER = "POSTER_CONFIRMED";

    @Override
    public String convertToDatabaseColumn(ChatRoomStatus attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.name();
    }

    @Override
    public ChatRoomStatus convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        if (LEGACY_AWAITING_PARTNER.equals(dbData)) {
            return ChatRoomStatus.AWAITING_PARTNER;
        }
        return ChatRoomStatus.valueOf(dbData);
    }
}
