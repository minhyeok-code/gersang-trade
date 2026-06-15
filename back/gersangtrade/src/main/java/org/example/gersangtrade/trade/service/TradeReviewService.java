package org.example.gersangtrade.trade.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gersangtrade.domain.notification.enums.NotificationType;
import org.example.gersangtrade.domain.trade.TradeReview;
import org.example.gersangtrade.domain.trade.enums.TradeRating;
import org.example.gersangtrade.domain.user.User;
import org.example.gersangtrade.notification.service.NotificationService;
import org.example.gersangtrade.trade.dto.request.TradeReviewSubmitRequest;
import org.example.gersangtrade.trade.dto.response.PendingReviewResponse;
import org.example.gersangtrade.trade.dto.response.TradeReviewResponse;
import org.example.gersangtrade.trade.repository.TradeReviewRepository;
import org.example.gersangtrade.trade.util.TradeReviewTiming;
import org.example.gersangtrade.user.util.ExpGradeCalculator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 거래 평가 서비스.
 *
 * 평가 흐름:
 *  1. 거래 확정 시 ChatService가 TradeReview 2건 자동 생성 (revealAt = null)
 *  2. 양측이 3일 이내에 평가 제출 (GOOD / NEUTRAL / BAD)
 *  3. 생성 2일 경과 배치가 revealAt(3일째 이후 첫 02:00) 설정
 *  4. revealAt 경과 배치가 일괄 공개 + EXP·매너점수 반영
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TradeReviewService {

    private final TradeReviewRepository tradeReviewRepository;
    private final NotificationService notificationService;

    // ──────────────────────────────────────────────────────────────────────
    // 평가 제출
    // ──────────────────────────────────────────────────────────────────────

    /**
     * 거래 평가를 제출한다.
     *
     * 검증 조건:
     *  - reviewId에 해당하는 평가의 reviewer가 userId와 일치해야 함
     *  - 생성 후 3일 이내(평가 기간)에만 제출 가능
     *  - 이미 제출된 평가(rating non-null)는 재제출 불가
     */
    @Transactional
    public void submit(Long userId, Long reviewId, TradeReviewSubmitRequest request) {
        TradeReview review = tradeReviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 평가입니다."));

        // 본인 평가 건인지 확인
        if (!review.getReviewer().getId().equals(userId)) {
            throw new IllegalStateException("본인의 평가만 제출할 수 있습니다.");
        }

        // 평가 기간 확인 (생성 후 3일)
        if (!TradeReviewTiming.isEvaluationOpen(review.getCreatedAt(), LocalDateTime.now())) {
            throw new IllegalStateException("평가 기간이 만료되었습니다.");
        }

        // 중복 제출 방지
        if (review.getRating() != null) {
            throw new IllegalStateException("이미 평가를 제출했습니다.");
        }

        review.submit(request.rating());
    }

    // ──────────────────────────────────────────────────────────────────────
    // 내가 받은 평가 조회
    // ──────────────────────────────────────────────────────────────────────

    /**
     * 내가 받은 공개된 평가 목록을 반환한다 (published=true만).
     */
    public List<TradeReviewResponse> getMyReceivedReviews(Long userId) {
        return tradeReviewRepository.findByTargetIdAndPublishedTrue(userId).stream()
                .map(TradeReviewResponse::of)
                .toList();
    }

    /**
     * 특정 유저가 받은 공개된 평가 목록을 반환한다 (published=true만).
     * 타 유저 프로필 화면에서 사용된다.
     */
    public List<TradeReviewResponse> getPublishedReviews(Long targetUserId) {
        return tradeReviewRepository.findByTargetIdAndPublishedTrue(targetUserId).stream()
                .map(TradeReviewResponse::of)
                .toList();
    }

    // ──────────────────────────────────────────────────────────────────────
    // 내가 제출해야 할 대기 평가 조회
    // ──────────────────────────────────────────────────────────────────────

    /**
     * 내가 아직 제출하지 않은 대기 중인 평가 목록을 반환한다.
     * rating IS NULL이고 생성 후 3일이 지나지 않은 것만 포함한다.
     */
    public List<PendingReviewResponse> getMyPendingReviews(Long userId) {
        LocalDateTime evaluationStartCutoff = LocalDateTime.now()
                .minusDays(TradeReviewTiming.EVALUATION_DAYS);
        return tradeReviewRepository.findPendingByReviewerId(userId, evaluationStartCutoff)
                .stream()
                .map(PendingReviewResponse::of)
                .toList();
    }

    // ──────────────────────────────────────────────────────────────────────
    // 배치 Job — revealAt 설정 · 평가 공개
    // ──────────────────────────────────────────────────────────────────────

    /**
     * 생성 후 2일이 지난 미공개 평가에 revealAt(3일째 이후 첫 02:00)을 설정한다.
     * 매일 새벽 2시 배치 스케줄러가 publishPendingReviews보다 먼저 호출한다.
     */
    @Transactional
    public void scheduleRevealAt() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime scheduleCutoff = now.minusDays(TradeReviewTiming.REVEAL_AT_SCHEDULE_AFTER_DAYS);
        List<TradeReview> targets = tradeReviewRepository.findPendingRevealAtSchedule(scheduleCutoff);
        log.info("평가 revealAt 설정 배치: 대상 {}건", targets.size());

        for (TradeReview review : targets) {
            LocalDateTime revealAt = TradeReviewTiming.computeRevealAt(review.getCreatedAt());
            review.scheduleRevealAt(revealAt);
            log.debug("평가 revealAt 설정: reviewId={}, revealAt={}", review.getId(), revealAt);
        }
    }

    /**
     * revealAt이 경과한 미공개 평가를 일괄 공개 처리한다.
     * 매일 새벽 2시 배치 스케줄러가 scheduleRevealAt 이후 호출한다.
     *
     * 처리 순서 (건별):
     *  1. review.publish() → published=true
     *  2. rating이 있으면 target 사용자에게 EXP·매너점수 반영
     *  3. REVIEW_PUBLISHED 알림 전송 (reviewer + target 각 1건)
     */
    @Transactional
    public void publishPendingReviews() {
        List<TradeReview> pending = tradeReviewRepository.findPendingPublish(LocalDateTime.now());
        log.info("평가 공개 배치 시작: 대상 {}건", pending.size());

        for (TradeReview review : pending) {
            review.publish();

            // rating이 있을 때만 EXP·매너점수 반영
            TradeRating rating = review.getRating();
            if (rating != null) {
                applyRatingToTarget(review.getTarget(), rating);
            }

            // 공개 알림 전송
            String message = "거래 평가가 공개되었습니다.";
            notificationService.send(review.getReviewer(), NotificationType.REVIEW_PUBLISHED,
                    null, message);
            notificationService.send(review.getTarget(), NotificationType.REVIEW_PUBLISHED,
                    null, message);
        }

        log.info("평가 공개 배치 완료: {}건 처리", pending.size());
    }

    // ──────────────────────────────────────────────────────────────────────
    // private 헬퍼
    // ──────────────────────────────────────────────────────────────────────

    /** 평가 결과를 target 사용자의 EXP·매너점수에 반영한다 */
    private void applyRatingToTarget(User target, TradeRating rating) {
        // EXP 반영 (음수 가능)
        long expDelta = rating.getExpDelta();
        if (expDelta != 0) {
            ExpGradeCalculator.GradeAndStep result =
                    ExpGradeCalculator.calculate(target.getTotalExp(), expDelta);
            target.applyExp(expDelta, result.grade(), result.step());
        }

        // 매너점수 반영 (0~100 클램핑은 User.applyMannerScore 내부 처리)
        int mannerDelta = rating.getMannerScoreDelta();
        if (mannerDelta != 0) {
            target.applyMannerScore(mannerDelta);
        }
    }
}
