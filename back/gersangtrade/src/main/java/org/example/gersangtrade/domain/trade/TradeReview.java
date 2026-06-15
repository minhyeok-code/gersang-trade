package org.example.gersangtrade.domain.trade;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.gersangtrade.domain.trade.enums.TradeRating;
import org.example.gersangtrade.domain.user.User;

import java.time.LocalDateTime;

/**
 * 거래 평가 엔티티 — 블라인드 방식.
 * 거래 확정(TradeConfirmed) 시 양측에 대해 각 1건씩 총 2건 자동 생성된다.
 *
 * 공개 정책:
 *   - 평가 제출 중 상대방의 제출 여부·내용 비공개
 *   - 생성 후 2일 경과 시 배치가 revealAt(3일째 이후 첫 02:00)을 설정
 *   - revealAt에 배치 Job이 isPublished=true로 전환하며 일괄 공개
 *   - 공개 시 rating의 EXP·매너점수 효과를 targetUser에게 반영
 *
 * 상세 정책: docs/gersang-grade-policy.md 4절 참고.
 */
@Entity
@Table(
        name = "trade_reviews",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_trade_reviews_confirmed_reviewer",
                columnNames = {"trade_confirmed_id", "reviewer_id"}
        ),
        indexes = @Index(name = "idx_trade_reviews_reveal_at", columnList = "reveal_at, is_published")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TradeReview {

    /** 거래 평가 고유 식별자 (자동 증가) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 연관된 거래 확정 기록.
     * TradeConfirmed 삭제 시에도 평가 기록 보존을 위해 nullable.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trade_confirmed_id")
    private TradeConfirmed tradeConfirmed;

    /** 평가를 제출한 사용자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id", nullable = false)
    private User reviewer;

    /** 평가 대상 사용자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_id", nullable = false)
    private User target;

    /**
     * 평가 선택지 — GOOD / NEUTRAL / BAD.
     * null: 아직 미제출. 3일 만료 후에도 null이면 효과 없음으로 처리.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "rating", length = 10)
    private TradeRating rating;

    /**
     * 평가 공개 예정 시각 — 생성 후 2일 경과 배치가 설정(3일째 이후 첫 02:00).
     * null: 아직 공개 일정 미확정. 이 시각이 지나면 배치가 isPublished=true로 전환한다.
     */
    @Column(name = "reveal_at")
    private LocalDateTime revealAt;

    /**
     * 공개 여부.
     * false: 아직 비공개 (평가 기간 진행 중).
     * true: 배치 Job이 3일 만료 후 전환 — EXP·매너점수 반영 완료.
     */
    @Column(name = "is_published", nullable = false)
    private boolean published;

    /** 평가 제출 시각 — 미제출이면 null */
    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    /** 생성 시각 */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public TradeReview(TradeConfirmed tradeConfirmed, User reviewer, User target, LocalDateTime revealAt) {
        this.tradeConfirmed = tradeConfirmed;
        this.reviewer = reviewer;
        this.target = target;
        this.revealAt = revealAt;
        this.published = false;
        this.createdAt = LocalDateTime.now();
    }

    /** 생성 후 2일 경과 배치가 공개 예정 시각을 설정한다 */
    public void scheduleRevealAt(LocalDateTime revealAt) {
        this.revealAt = revealAt;
    }

    /**
     * 평가 제출 — 생성 후 3일 이내에만 가능(서비스에서 검증).
     *
     * @param rating 선택한 평가 (GOOD / NEUTRAL / BAD)
     */
    public void submit(TradeRating rating) {
        this.rating = rating;
        this.submittedAt = LocalDateTime.now();
    }

    /**
     * 배치 Job이 revealAt 경과 후 호출 — 공개 처리.
     * 이후 target 사용자에게 rating 효과(EXP·매너점수)를 반영한다.
     */
    public void publish() {
        this.published = true;
    }
}
