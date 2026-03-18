package org.example.gersangtrade.domain.trade;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.gersangtrade.domain.common.BaseEntity;
import org.example.gersangtrade.domain.listing.TradeListing;
import org.example.gersangtrade.domain.trade.enums.ApplicationStatus;
import org.example.gersangtrade.domain.user.User;

import java.time.LocalDateTime;

/**
 * 거래 신청(Application) 엔티티.
 * 구매자가 거래 등록글에 대해 신청을 남기면 생성된다.
 * 판매자가 수락(ACCEPTED) 시 거래가 IN_TRADE 상태로 전환된다.
 * 상태 흐름: PENDING → ACCEPTED | REJECTED | CANCELLED
 */
@Entity
@Table(name = "trade_applications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TradeApplication extends BaseEntity {

    /** 거래 신청 고유 식별자 (자동 증가) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 신청 대상 거래 등록글 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = false)
    private TradeListing listing;

    /** 거래 신청자(구매자) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    /** 신청 처리 상태 — PENDING / ACCEPTED / REJECTED / CANCELLED */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ApplicationStatus status;

    /** 구매자가 남기는 메모 또는 연락 메시지 (선택 입력) */
    @Column(name = "message", length = 500)
    private String message;

    /** 판매자가 수락/거절한 시각 — 응답 전에는 null */
    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Builder
    public TradeApplication(TradeListing listing, User buyer, String message) {
        this.listing = listing;
        this.buyer = buyer;
        this.message = message;
        // 초기 상태는 항상 PENDING
        this.status = ApplicationStatus.PENDING;
    }

    /** 판매자 수락 처리 */
    public void accept() {
        this.status = ApplicationStatus.ACCEPTED;
        this.respondedAt = LocalDateTime.now();
    }

    /** 판매자 거절 처리 */
    public void reject() {
        this.status = ApplicationStatus.REJECTED;
        this.respondedAt = LocalDateTime.now();
    }

    /** 구매자 취소 처리 */
    public void cancel() {
        this.status = ApplicationStatus.CANCELLED;
    }
}
