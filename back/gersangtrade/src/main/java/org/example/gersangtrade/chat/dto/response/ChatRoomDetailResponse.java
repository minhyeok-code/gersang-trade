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
        String listingDisplayName,
        InitiationType initiationType,
        /** 내 기준 상대방 유저 ID */
        Long partnerId,
        /** 내 기준 상대방 닉네임 */
        String partnerNickname,
        String posterNickname,
        String counterpartyNickname,
        ChatRoomStatus status,
        /** 게시물 등록 가격 (거래가 입력 기본값) */
        Long listingPrice,
        Long finalPrice,
        LocalDateTime posterConfirmedAt,
        LocalDateTime counterpartyConfirmedAt,
        LocalDateTime completedAt,
        /** 현재 사용자가 거래완료를 눌렀는지 */
        boolean myTradeConfirmed,
        /** 상대방이 거래완료를 눌렀는지 */
        boolean partnerTradeConfirmed,
        LocalDateTime createdAt,
        List<ChatMessageResponse> messages
) {
    public static ChatRoomDetailResponse of(
            ChatRoom room,
            Long viewerId,
            String listingDisplayName,
            Long listingPrice,
            List<ChatMessageResponse> messages
    ) {
        boolean isPoster = room.getPoster().getId().equals(viewerId);
        boolean myTradeConfirmed = isPoster
                ? room.getPosterConfirmedAt() != null
                : room.getCounterpartyConfirmedAt() != null;
        boolean partnerTradeConfirmed = isPoster
                ? room.getCounterpartyConfirmedAt() != null
                : room.getPosterConfirmedAt() != null;

        Long partnerId = isPoster
                ? room.getCounterparty().getId()
                : room.getPoster().getId();
        String partnerNickname = isPoster
                ? room.getCounterparty().getNickname()
                : room.getPoster().getNickname();

        return new ChatRoomDetailResponse(
                room.getId(),
                room.getListingType(),
                room.getListingId(),
                listingDisplayName,
                room.getInitiationType(),
                partnerId,
                partnerNickname,
                room.getPoster().getNickname(),
                room.getCounterparty().getNickname(),
                room.getStatus(),
                listingPrice,
                room.getFinalPrice(),
                room.getPosterConfirmedAt(),
                room.getCounterpartyConfirmedAt(),
                room.getCompletedAt(),
                myTradeConfirmed,
                partnerTradeConfirmed,
                room.getCreatedAt(),
                messages
        );
    }
}
