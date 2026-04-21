package org.example.gersangtrade.chat.dto.response;

import org.example.gersangtrade.domain.chat.ChatRoom;
import org.example.gersangtrade.domain.chat.enums.ChatRoomStatus;
import org.example.gersangtrade.domain.chat.enums.InitiationType;
import org.example.gersangtrade.domain.chat.enums.ListingType;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 채팅방 상세(메시지 목록 포함) 응답 DTO.
 */
public record ChatRoomDetailResponse(

        Long id,
        ListingType listingType,
        Long listingId,
        InitiationType initiationType,
        String posterNickname,
        String counterpartyNickname,
        ChatRoomStatus status,
        Long finalPrice,
        LocalDateTime posterConfirmedAt,
        LocalDateTime counterpartyConfirmedAt,
        LocalDateTime completedAt,
        LocalDateTime createdAt,
        List<ChatMessageResponse> messages
) {
    public static ChatRoomDetailResponse of(ChatRoom room, List<ChatMessageResponse> messages) {
        return new ChatRoomDetailResponse(
                room.getId(),
                room.getListingType(),
                room.getListingId(),
                room.getInitiationType(),
                room.getPoster().getNickname(),
                room.getCounterparty().getNickname(),
                room.getStatus(),
                room.getFinalPrice(),
                room.getPosterConfirmedAt(),
                room.getCounterpartyConfirmedAt(),
                room.getCompletedAt(),
                room.getCreatedAt(),
                messages
        );
    }
}
