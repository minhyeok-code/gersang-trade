package org.example.gersangtrade.domain.chat.enums;

/**
 * 채팅방이 연결된 게시물 종류.
 * ChatRoom이 TradeListing(SELL) 또는 WantedListing(BUY) 중 어디서 시작됐는지 구분한다.
 */
public enum ListingType {

    /** 판매 게시물 (TradeListing) */
    SELL,

    /** 구매 게시물 (WantedListing) */
    BUY
}
