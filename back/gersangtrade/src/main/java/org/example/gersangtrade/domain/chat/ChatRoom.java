package org.example.gersangtrade.domain.chat;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
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
 * 거래완료 순서: 게시자(poster)가 먼저 confirm → 상대방(counterparty)이 confirm → COMPLETED.
 * 상세 흐름: docs/trade-flow-design.ko.md 참고.
 */
@Entity
@Table(
        name = "chat_rooms",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_chat_rooms_listing_counterparty_status",
                columnNames = {"listing_type", "listing_id", "counterparty_id", "status"}
        )
)
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
    @Enumerated(EnumType.STRING)
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
     * 게시자 거래완료 확인 (1단계).
     * finalPrice가 null이면 게시물 가격을 그대로 사용한다.
     *
     * @param finalPrice 실제 거래가 (null 허용 — null이면 게시물 가격 사용)
     */
    public void posterConfirm(Long finalPrice) {
        this.finalPrice = finalPrice;
        this.posterConfirmedAt = LocalDateTime.now();
        this.status = ChatRoomStatus.POSTER_CONFIRMED;
    }

    /**
     * 상대방 거래완료 확인 (2단계).
     * POSTER_CONFIRMED 상태에서만 호출 가능하다.
     */
    public void counterpartyConfirm() {
        this.counterpartyConfirmedAt = LocalDateTime.now();
        this.completedAt = LocalDateTime.now();
        this.status = ChatRoomStatus.COMPLETED;
    }

    /** 채팅방 강제 종료 — 취소 또는 다른 거래 완료로 인한 종료 */
    public void close() {
        this.status = ChatRoomStatus.CLOSED;
    }
}
