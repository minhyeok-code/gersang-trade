package org.example.gersangtrade.domain.wanted;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.gersangtrade.domain.common.BaseEntity;
import org.example.gersangtrade.domain.user.User;
import org.example.gersangtrade.domain.wanted.enums.WantedStatus;

import java.time.LocalDateTime;

/**
 * 구매 희망 등록글 엔티티.
 * 구매자가 원하는 아이템 조건과 제시 가격을 등록하는 게시물이다.
 * 상태 흐름: OPEN → IN_TRADE → PURCHASED | CANCELLED
 * 소프트 삭제(deletedAt) 방식을 사용하며, 1년 후 배치 하드딜리트된다.
 */
@Entity
@Table(name = "wanted_listings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WantedListing extends BaseEntity {

    /** 구매 희망 등록글 고유 식별자 (자동 증가) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 구매자 사용자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    /** 거래 대상 서버명 */
    @Column(name = "server", nullable = false, length = 30)
    private String server;

    /** 구매 희망 등록글 상태 */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private WantedStatus status;

    /** 구매자 제시 가격 (게임 내 재화 기준) */
    @Column(name = "offered_price", nullable = false)
    private Long offeredPrice;

    /** 구매자 요청 메모 또는 연락처 (선택 입력) */
    @Column(name = "note", length = 500)
    private String note;

    /**
     * 관리자 숨김 여부.
     * true: 관리자가 정책 위반 등의 이유로 숨긴 상태.
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
    public WantedListing(User buyer, String server, Long offeredPrice, String note) {
        this.buyer = buyer;
        this.server = server;
        this.offeredPrice = offeredPrice;
        this.note = note;
        // 초기 상태는 항상 OPEN
        this.status = WantedStatus.OPEN;
        this.hidden = false;
    }

    /** 거래 진행 중 상태로 전환 */
    public void startTrade() {
        this.status = WantedStatus.IN_TRADE;
    }

    /** 구매 완료 처리 */
    public void completePurchase() {
        this.status = WantedStatus.PURCHASED;
    }

    /** 구매 희망 취소 처리 */
    public void cancel() {
        this.status = WantedStatus.CANCELLED;
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
