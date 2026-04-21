package org.example.gersangtrade.chat.repository;

import org.example.gersangtrade.domain.chat.ChatRoom;
import org.example.gersangtrade.domain.chat.enums.ChatRoomStatus;
import org.example.gersangtrade.domain.chat.enums.ListingType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 채팅방 레포지토리.
 * 게시물별·사용자별 채팅방 조회 및 상태 필터링에 사용된다.
 */
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    /**
     * 특정 게시물에 열린 채팅방 목록 조회.
     * 게시자가 자신의 게시물에 대한 모든 채팅 요청을 확인할 때 사용.
     */
    List<ChatRoom> findByListingTypeAndListingId(ListingType listingType, Long listingId);

    /**
     * 특정 게시물에서 상대방(counterparty)이 연 채팅방 중 주어진 상태인 것 조회.
     * 중복 채팅방 생성 방지(UNIQUE 제약 보조 쿼리)로 사용.
     */
    boolean existsByListingTypeAndListingIdAndCounterpartyIdAndStatus(
            ListingType listingType, Long listingId, Long counterpartyId, ChatRoomStatus status);

    /**
     * 사용자가 게시자(poster)로 참여 중인 채팅방 목록 조회.
     */
    List<ChatRoom> findByPosterId(Long posterId);

    /**
     * 사용자가 상대방(counterparty)으로 참여 중인 채팅방 목록 조회.
     */
    List<ChatRoom> findByCounterpartyId(Long counterpartyId);
}
