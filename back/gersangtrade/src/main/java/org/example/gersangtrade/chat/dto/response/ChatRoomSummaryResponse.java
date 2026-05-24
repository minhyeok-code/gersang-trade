package org.example.gersangtrade.chat.dto.response;

import org.example.gersangtrade.domain.chat.ChatRoom;
import org.example.gersangtrade.domain.chat.enums.ChatRoomStatus;
import org.example.gersangtrade.domain.chat.enums.InitiationType;
import org.example.gersangtrade.domain.chat.enums.ListingType;

import java.time.LocalDateTime;

/**
 * 채팅방 목록 조회 응답 DTO.
 * 상대방 닉네임과 채팅방 상태를 포함한다.
 */
public record ChatRoomSummaryResponse(

        Long id,
        ListingType listingType,
        Long listingId,
        String listingDisplayName,
        InitiationType initiationType,

        /** 내 기준 상대방 닉네임 */
        String partnerNickname,

        ChatRoomStatus status,
        Long finalPrice,
        LocalDateTime createdAt
) {
    /**
     * 채팅방 엔티티에서 DTO 생성.
     * viewerId를 기준으로 상대방 닉네임을 결정한다.
     */
    public static ChatRoomSummaryResponse of(ChatRoom room, Long viewerId, String listingDisplayName) {
        boolean isViewer = room.getPoster().getId().equals(viewerId);
        String partnerNickname = isViewer
                ? room.getCounterparty().getNickname()
                : room.getPoster().getNickname();

        return new ChatRoomSummaryResponse(
                room.getId(),
                room.getListingType(),
                room.getListingId(),
                listingDisplayName,
                room.getInitiationType(),
                partnerNickname,
                room.getStatus(),
                room.getFinalPrice(),
                room.getCreatedAt()
        );
    }
}
