package org.example.gersangtrade.domain.trade;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.gersangtrade.domain.chat.ChatRoom;
import org.example.gersangtrade.domain.chat.enums.ListingType;
import org.example.gersangtrade.domain.user.User;

import java.time.LocalDateTime;

/**
 * 거래 확정(Confirmed) 기록 엔티티.
 * 거래가 완료될 때 생성되는 불변 레코드로, 가격 통계 집계의 원천 데이터(Source of Truth)이다.
 * 리스팅/채팅방/사용자가 삭제·숨김 처리되더라도 거래 기록은 보존되도록 FK가 nullable로 설계되었다.
 * BaseEntity를 상속하지 않으며, confirmedAt으로 생성 시각을 직접 관리한다.
 * cancelled=true인 레코드는 통계 집계 시 제외된다.
 */
@Entity
@Table(name = "trade_confirmed")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TradeConfirmed {

    /** 거래 확정 기록 고유 식별자 (자동 증가) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 거래가 완료된 채팅방 참조.
     * 채팅방이 삭제된 후에도 확정 기록은 유지되므로 nullable.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id")
    private ChatRoom chatRoom;

    /**
     * 게시물 종류 스냅샷 (SELL: 판매 게시물 / BUY: 구매 게시물).
     * 채팅방 삭제 후에도 어떤 종류의 게시물에서 거래가 이루어졌는지 보존한다.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "listing_type", nullable = false, length = 10)
    private ListingType listingType;

    /**
     * 판매자 사용자 참조.
     * User 삭제 후에도 거래 기록을 보존하기 위해 nullable.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id")
    private User seller;

    /**
     * 구매자 사용자 참조.
     * User 삭제 후에도 거래 기록을 보존하기 위해 nullable.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id")
    private User buyer;

    /** 거래 확정 당시의 서버명 스냅샷 (리스팅 변경·삭제 후에도 보존) */
    @Column(name = "server_snapshot", nullable = false, length = 30)
    private String serverSnapshot;

    /** 거래 확정 당시의 가격 스냅샷 (리스팅 변경·삭제 후에도 보존) */
    @Column(name = "confirmed_price", nullable = false)
    private Long confirmedPrice;

    /**
     * 통계 집계 키 스냅샷.
     * 예: "ITEM:1", "SET:2" — 어떤 아이템/세트에 대한 거래인지를 나타내는 키.
     * TradeStatDaily/Monthly 집계 시 group-by 키로 사용된다.
     *
     * <p>TODO: MVP(1단계)는 단순 문자열로 유지. 주술 조합·세트 부분 주술 집계(2단계)로 확장 시
     * stat_item_id / stat_ritual_mark / stat_set_id / stat_ritual_count 복합 컬럼 방식으로
     * 전환 필요. 전환 전 기존 데이터 마이그레이션 필수.
     */
    @Column(name = "stat_key_snapshot", nullable = false, length = 255)
    private String statKeySnapshot;

    /** 거래 확정 시각 */
    @Column(name = "confirmed_at", nullable = false)
    private LocalDateTime confirmedAt;

    /**
     * 거래 취소 여부.
     * true: 취소된 확정 거래 — 통계 재산출 시 제외 대상.
     * false: 정상 확정 거래.
     *
     * <p>TODO: MVP 이후 cancelled(boolean) → cancelledBy(BUYER | SELLER | ADMIN) Enum으로 확장 예정.
     * 분쟁 처리·패널티 정책 도입 시 취소 주체 추적이 필요해짐.
     */
    @Column(name = "cancelled", nullable = false)
    private boolean cancelled;

    /** 거래 취소 시각 — 취소 전에는 null */
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Builder
    public TradeConfirmed(ChatRoom chatRoom, ListingType listingType,
                           User seller, User buyer,
                           String serverSnapshot, Long confirmedPrice,
                           String statKeySnapshot, LocalDateTime confirmedAt) {
        this.chatRoom = chatRoom;
        this.listingType = listingType;
        this.seller = seller;
        this.buyer = buyer;
        this.serverSnapshot = serverSnapshot;
        this.confirmedPrice = confirmedPrice;
        this.statKeySnapshot = statKeySnapshot;
        this.confirmedAt = (confirmedAt != null) ? confirmedAt : LocalDateTime.now();
        this.cancelled = false;
    }

    /** 거래 취소 처리 — 통계 집계 시 이 레코드는 제외된다 */
    public void cancel() {
        this.cancelled = true;
        this.cancelledAt = LocalDateTime.now();
    }
}
