package org.example.gersangtrade.chat.repository;

import jakarta.persistence.LockModeType;
import org.example.gersangtrade.domain.chat.ChatRoom;
import org.example.gersangtrade.domain.chat.enums.ChatRoomStatus;
import org.example.gersangtrade.domain.chat.enums.ListingType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

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
     * @deprecated {@link #existsActiveByListingTypeAndListingIdAndCounterpartyId} 사용
     */
    @Deprecated
    boolean existsByListingTypeAndListingIdAndCounterpartyIdAndStatus(
            ListingType listingType, Long listingId, Long counterpartyId, ChatRoomStatus status);

    /**
     * 동일 게시물·상대방 조합의 진행 중(OPEN/AWAITING_PARTNER) 채팅방 존재 여부.
     * COMPLETED/CLOSED 이력과 무관하게 활성 채팅방 1개만 허용한다.
     */
    @org.springframework.data.jpa.repository.Query("""
            SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END
            FROM ChatRoom r
            WHERE r.listingType = :listingType
              AND r.listingId = :listingId
              AND r.counterparty.id = :counterpartyId
              AND r.status IN (org.example.gersangtrade.domain.chat.enums.ChatRoomStatus.OPEN,
                               org.example.gersangtrade.domain.chat.enums.ChatRoomStatus.AWAITING_PARTNER)
            """)
    boolean existsActiveByListingTypeAndListingIdAndCounterpartyId(
            @org.springframework.data.repository.query.Param("listingType") ListingType listingType,
            @org.springframework.data.repository.query.Param("listingId") Long listingId,
            @org.springframework.data.repository.query.Param("counterpartyId") Long counterpartyId);

    /**
     * 동일 게시물·상대방 조합에서 다른 채팅방이 이미 COMPLETED인지 확인한다.
     * native query — AttributeConverter 적용 필드의 JPQL 비교 이슈 방지.
     * MySQL SELECT EXISTS(...)는 JDBC에서 Long(0/1)을 반환하므로 Long 타입으로 수신 후 서비스에서 > 0 비교.
     */
    @Query(value = """
        SELECT COUNT(1)
        FROM chat_rooms
        WHERE listing_type = :listingType
          AND listing_id = :listingId
          AND counterparty_id = :counterpartyId
          AND status = 'COMPLETED'
          AND id <> :excludeRoomId
        """, nativeQuery = true)
    long countOtherCompletedRoom(
            @Param("listingType") String listingType,
            @Param("listingId") Long listingId,
            @Param("counterpartyId") Long counterpartyId,
            @Param("excludeRoomId") Long excludeRoomId);

    /**
     * 거래완료 확인 시 비관적 쓰기 잠금으로 채팅방 조회.
     * 동시에 두 사용자가 confirmTrade를 호출할 경우 lost-update 방지.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM ChatRoom r WHERE r.id = :id")
    Optional<ChatRoom> findWithLockById(@Param("id") Long id);

    /**
     * 사용자가 게시자(poster)로 참여 중인 채팅방 목록 조회.
     */
    List<ChatRoom> findByPosterId(Long posterId);

    /**
     * 사용자가 상대방(counterparty)으로 참여 중인 채팅방 목록 조회.
     */
    List<ChatRoom> findByCounterpartyId(Long counterpartyId);
}
