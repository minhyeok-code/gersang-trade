package org.example.gersangtrade.chat.dto.request;

import jakarta.validation.constraints.NotNull;
import org.example.gersangtrade.domain.chat.enums.InitiationType;
import org.example.gersangtrade.domain.chat.enums.ListingType;

/**
 * 채팅방 생성 요청 DTO.
 * 흥정하기(NEGOTIATE) 또는 거래신청(APPLY) 버튼 클릭 시 전송된다.
 */
public record ChatRoomCreateRequest(

        /** 게시물 종류 (SELL: 판매 게시물 / BUY: 구매 게시물) */
        @NotNull(message = "게시물 종류는 필수입니다.")
        ListingType listingType,

        /** 대상 게시물 ID */
        @NotNull(message = "게시물 ID는 필수입니다.")
        Long listingId,

        /** 채팅방 개설 방식 (NEGOTIATE: 흥정하기 / APPLY: 거래신청) */
        @NotNull(message = "채팅 개설 방식은 필수입니다.")
        InitiationType initiationType
) {}
