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
        LocalDateTime createdAt,

        /** 상대방 TEXT 메시지 미읽음 여부 */
        boolean hasUnread,

        /** 현재 사용자가 거래완료를 눌렀는지 */
        boolean myTradeConfirmed,
        /** 상대방이 거래완료를 눌렀는지 */
        boolean partnerTradeConfirmed
) {
    /**
     * 채팅방 엔티티에서 DTO 생성.
     * viewerId를 기준으로 상대방 닉네임을 결정한다.
     */
    public static ChatRoomSummaryResponse of(ChatRoom room, Long viewerId, String listingDisplayName,
                                             boolean hasUnread) {
        boolean isViewerPoster = room.getPoster().getId().equals(viewerId);
        String partnerNickname = isViewerPoster
                ? room.getCounterparty().getNickname()
                : room.getPoster().getNickname();
        boolean myTradeConfirmed = isViewerPoster
                ? room.getPosterConfirmedAt() != null
                : room.getCounterpartyConfirmedAt() != null;
        boolean partnerTradeConfirmed = isViewerPoster
                ? room.getCounterpartyConfirmedAt() != null
                : room.getPosterConfirmedAt() != null;

        return new ChatRoomSummaryResponse(
                room.getId(),
                room.getListingType(),
                room.getListingId(),
                listingDisplayName,
                room.getInitiationType(),
                partnerNickname,
                room.getStatus(),
                room.getFinalPrice(),
                room.getCreatedAt(),
                hasUnread,
                myTradeConfirmed,
                partnerTradeConfirmed
        );
    }
}
