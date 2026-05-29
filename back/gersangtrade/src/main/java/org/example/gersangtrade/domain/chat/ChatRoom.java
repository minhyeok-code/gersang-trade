package org.example.gersangtrade.domain.chat;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;
import org.example.gersangtrade.domain.chat.converter.ChatRoomStatusConverter;
import org.example.gersangtrade.domain.chat.enums.ChatRoomStatus;
import org.example.gersangtrade.domain.chat.enums.InitiationType;
import org.example.gersangtrade.domain.chat.enums.ListingType;
import org.example.gersangtrade.domain.common.BaseEntity;
import org.example.gersangtrade.domain.user.User;

import java.time.LocalDateTime;

/**
 * 1:1 채팅방 엔티티.
 * 흥정하기(NEGOTIATE) 또는 거래신청(APPLY) 버튼을 누를 때 생성된다.
 * 하나의 게시물에 여러 상대방이 각자 채팅방을 열 수 있다.
 *
 * 거래완료: 양측 중 누구든 먼저 확인 가능 → 상대방도 확인하면 COMPLETED.
 * 상세 흐름: docs/trade-flow-design.ko.md 참고.
 */
@Entity
@Table(
        name = "chat_rooms",
        indexes = @Index(
                name = "idx_chat_rooms_listing_counterparty",
                columnList = "listing_type, listing_id, counterparty_id"
        )
)
@DynamicUpdate
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom extends BaseEntity {

    /** 채팅방 고유 식별자 (자동 증가) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 게시물 종류 (SELL: 판매 게시물 / BUY: 구매 게시물) */
    @Enumerated(EnumType.STRING)
    @Column(name = "listing_type", nullable = false, length = 10)
    private ListingType listingType;

    /**
     * 게시물 ID — listingType에 따라 TradeListing.id 또는 WantedListing.id 참조.
     * 다형 참조이므로 FK 제약 없이 ID만 저장한다.
     */
    @Column(name = "listing_id", nullable = false)
    private Long listingId;

    /** 채팅방 개설 방식 (NEGOTIATE: 흥정하기 / APPLY: 거래신청) */
    @Enumerated(EnumType.STRING)
    @Column(name = "initiation_type", nullable = false, length = 15)
    private InitiationType initiationType;

    /** 게시물 작성자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poster_id", nullable = false)
    private User poster;

    /** 흥정하기/거래신청을 누른 상대방 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "counterparty_id", nullable = false)
    private User counterparty;

    /** 채팅방 진행 상태 */
    @Convert(converter = ChatRoomStatusConverter.class)
    @Column(name = "status", nullable = false, length = 20)
    private ChatRoomStatus status;

    /**
     * 실제 거래가 — 게시자가 거래완료 버튼 누를 때 입력.
     * null이면 게시물의 가격(listing.price 또는 wantedListing.offeredPrice)을 사용한다.
     */
    @Column(name = "final_price")
    private Long finalPrice;

    /** 게시자가 거래완료 버튼을 누른 시각 */
    @Column(name = "poster_confirmed_at")
    private LocalDateTime posterConfirmedAt;

    /** 상대방이 거래완료 버튼을 누른 시각 */
    @Column(name = "counterparty_confirmed_at")
    private LocalDateTime counterpartyConfirmedAt;

    /** 양측 모두 거래완료 확인된 시각 (= 거래 확정 시각) */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /** 게시자가 채팅방을 마지막으로 읽은 시각 (미읽음 판별 기준) */
    @Column(name = "poster_last_read_at")
    private LocalDateTime posterLastReadAt;

    /** 상대방이 채팅방을 마지막으로 읽은 시각 (미읽음 판별 기준) */
    @Column(name = "counterparty_last_read_at")
    private LocalDateTime counterpartyLastReadAt;

    @Builder
    public ChatRoom(ListingType listingType, Long listingId, InitiationType initiationType,
                    User poster, User counterparty) {
        this.listingType = listingType;
        this.listingId = listingId;
        this.initiationType = initiationType;
        this.poster = poster;
        this.counterparty = counterparty;
        this.status = ChatRoomStatus.OPEN;
    }

    /**
     * 참여자 1명의 거래완료 확인.
     * 양측 모두 확인되면 COMPLETED, 한쪽만 확인되면 AWAITING_PARTNER(상대 확인 대기).
     *
     * @param isPoster   true=게시자, false=상대방
     * @param finalPrice 첫 확인 시 선택 입력 가능 (null이면 게시물 가격 사용)
     */
    public void recordParticipantConfirm(boolean isPoster, Long finalPrice) {
        if (status == ChatRoomStatus.COMPLETED || status == ChatRoomStatus.CLOSED) {
            throw new IllegalStateException("종료된 채팅방에서는 거래완료할 수 없습니다.");
        }
        if (isPoster) {
            if (posterConfirmedAt != null) {
                throw new IllegalStateException("이미 거래완료를 확인했습니다.");
            }
            if (finalPrice != null) {
                this.finalPrice = finalPrice;
            }
            this.posterConfirmedAt = LocalDateTime.now();
        } else {
            if (counterpartyConfirmedAt != null) {
                throw new IllegalStateException("이미 거래완료를 확인했습니다.");
            }
            this.counterpartyConfirmedAt = LocalDateTime.now();
        }

        if (posterConfirmedAt != null && counterpartyConfirmedAt != null) {
            this.completedAt = LocalDateTime.now();
            this.status = ChatRoomStatus.COMPLETED;
        } else {
            this.status = ChatRoomStatus.AWAITING_PARTNER;
        }
    }

    /** @deprecated {@link #recordParticipantConfirm(boolean, Long)} 사용 */
    @Deprecated
    public void posterConfirm(Long finalPrice) {
        recordParticipantConfirm(true, finalPrice);
    }

    /** @deprecated {@link #recordParticipantConfirm(boolean, Long)} 사용 */
    @Deprecated
    public void counterpartyConfirm() {
        recordParticipantConfirm(false, null);
    }

    /** 채팅방 강제 종료 — 취소 또는 다른 거래 완료로 인한 종료 */
    public void close() {
        this.status = ChatRoomStatus.CLOSED;
    }

    /** 참여자의 마지막 읽음 시각을 현재 시각으로 갱신한다 */
    public void markReadBy(Long userId) {
        if (poster.getId().equals(userId)) {
            this.posterLastReadAt = LocalDateTime.now();
        } else if (counterparty.getId().equals(userId)) {
            this.counterpartyLastReadAt = LocalDateTime.now();
        } else {
            throw new IllegalStateException("채팅방 참여자만 읽음 처리할 수 있습니다.");
        }
    }

    /** viewerId 기준 마지막 읽음 시각 */
    public LocalDateTime lastReadAtFor(Long viewerId) {
        if (poster.getId().equals(viewerId)) {
            return posterLastReadAt;
        }
        if (counterparty.getId().equals(viewerId)) {
            return counterpartyLastReadAt;
        }
        return null;
    }
}
