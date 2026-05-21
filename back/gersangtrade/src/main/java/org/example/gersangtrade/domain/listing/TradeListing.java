package org.example.gersangtrade.domain.listing;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.gersangtrade.domain.common.BaseEntity;
import org.example.gersangtrade.domain.listing.enums.ListingStatus;
import org.example.gersangtrade.domain.user.User;

import java.time.LocalDateTime;

/**
 * 거래 등록글(Listing) 엔티티.
 * 판매자가 게임 아이템을 거래하기 위해 등록하는 게시물이다.
 * 상태 흐름: ACTIVE → IN_TRADE → SOLD | CANCELLED
 * 소프트 삭제(deletedAt) 방식을 사용하며, 1년 후 배치 하드딜리트된다.
 * 관리자 숨김(hidden)은 판매자 취소(CANCELLED)와 별도로 관리된다.
 */
@Entity
@Table(name = "trade_listings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TradeListing extends BaseEntity {

    /** 거래 등록글 고유 식별자 (자동 증가) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 판매자 사용자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    /** 거래 대상 서버명 — 추후 Enum 전환 예정 */
    @Column(name = "server", nullable = false, length = 30)
    private String server;

    /** 거래 등록글 상태 */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ListingStatus status;

    /** 판매자 희망 가격 (게임 내 재화 기준) */
    @Column(name = "price", nullable = false)
    private Long price;

    /** 판매자 연락처 또는 메모 (선택 입력) */
    @Column(name = "note", length = 500)
    private String note;

    /**
     * 관리자 숨김 여부.
     * true: 관리자가 정책 위반 등의 이유로 숨긴 상태.
     * 판매자의 CANCELLED 상태와 별도로 관리되어, 이력 추적이 가능하다.
     */
    @Column(name = "hidden", nullable = false)
    private boolean hidden;

    /**
     * 소프트 삭제 시각.
     * null: 활성 등록글, non-null: 삭제된 등록글 (1년 후 배치 하드딜리트 대상).
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    public TradeListing(User seller, String server, Long price, String note) {
        this.seller = seller;
        this.server = server;
        this.price = price;
        this.note = note;
        // 초기 상태는 항상 ACTIVE
        this.status = ListingStatus.ACTIVE;
        this.hidden = false;
    }

    /** 가격·메모 수정 — ACTIVE 상태에서만 호출해야 한다 */
    public void updatePriceAndNote(Long price, String note) {
        this.price = price;
        this.note = note;
    }

    /** 거래 신청 수락 시 IN_TRADE 상태로 전환 */
    public void startTrade() {
        this.status = ListingStatus.IN_TRADE;
    }

    /** 거래 확정 시 SOLD 상태로 전환 */
    public void completeTrade() {
        this.status = ListingStatus.SOLD;
    }

    /** 거래 취소 처리 */
    public void cancel() {
        this.status = ListingStatus.CANCELLED;
    }

    /** 관리자 숨김 처리 */
    public void hide() {
        this.hidden = true;
    }

    /** 관리자 숨김 해제 */
    public void unhide() {
        this.hidden = false;
    }

    /** 소프트 삭제 처리 */
    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }
}
