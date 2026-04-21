package org.example.gersangtrade.trade.service;

import org.example.gersangtrade.domain.trade.TradeReview;
import org.example.gersangtrade.domain.trade.enums.TradeRating;
import org.example.gersangtrade.domain.user.User;
import org.example.gersangtrade.domain.user.enums.GradeLevel;
import org.example.gersangtrade.domain.user.enums.Role;
import org.example.gersangtrade.domain.user.enums.UserStatus;
import org.example.gersangtrade.notification.service.NotificationService;
import org.example.gersangtrade.trade.dto.request.TradeReviewSubmitRequest;
import org.example.gersangtrade.trade.dto.response.TradeReviewResponse;
import org.example.gersangtrade.trade.repository.TradeReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TradeReviewService 단위 테스트.
 * 평가 제출·공개 배치·내가 받은 평가 조회 로직을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TradeReviewServiceTest {

    @Mock
    private TradeReviewRepository tradeReviewRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private TradeReviewService tradeReviewService;

    private User reviewer;
    private User target;

    @BeforeEach
    void setUp() {
        reviewer = spy(User.builder()
                .oauthProvider("google").oauthId("google-reviewer")
                .nickname("평가자").email("reviewer@test.com")
                .role(Role.USER).status(UserStatus.ACTIVE)
                .build());
        doReturn(1L).when(reviewer).getId();

        target = spy(User.builder()
                .oauthProvider("google").oauthId("google-target")
                .nickname("대상자").email("target@test.com")
                .role(Role.USER).status(UserStatus.ACTIVE)
                .build());
        doReturn(2L).when(target).getId();
    }

    // ── submit() 테스트 ───────────────────────────────────────────────────────

    @Test
    @DisplayName("submit_정상_평가기간내_미제출_GOOD평가저장")
    void submit_정상_평가기간내_미제출_GOOD평가저장() {
        TradeReview review = mockReview(1L, reviewer, target,
                LocalDateTime.now().plusDays(2), null);
        when(tradeReviewRepository.findById(1L)).thenReturn(Optional.of(review));

        tradeReviewService.submit(1L, 1L, new TradeReviewSubmitRequest(TradeRating.GOOD));

        verify(review).submit(TradeRating.GOOD);
    }

    @Test
    @DisplayName("submit_본인평가아닌경우_예외발생")
    void submit_본인평가아닌경우_예외발생() {
        // reviewer id=1인데 userId=99로 호출
        TradeReview review = mockReview(1L, reviewer, target,
                LocalDateTime.now().plusDays(2), null);
        when(tradeReviewRepository.findById(1L)).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> tradeReviewService.submit(99L, 1L,
                new TradeReviewSubmitRequest(TradeRating.GOOD)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("본인의 평가만 제출할 수 있습니다");
    }

    @Test
    @DisplayName("submit_평가기간만료_예외발생")
    void submit_평가기간만료_예외발생() {
        // revealAt이 과거 → 이미 만료
        TradeReview review = mockReview(1L, reviewer, target,
                LocalDateTime.now().minusDays(1), null);
        when(tradeReviewRepository.findById(1L)).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> tradeReviewService.submit(1L, 1L,
                new TradeReviewSubmitRequest(TradeRating.NEUTRAL)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("평가 기간이 만료되었습니다");
    }

    @Test
    @DisplayName("submit_이미제출된평가_예외발생")
    void submit_이미제출된평가_예외발생() {
        // 이미 GOOD으로 제출된 상태
        TradeReview review = mockReview(1L, reviewer, target,
                LocalDateTime.now().plusDays(2), TradeRating.GOOD);
        when(tradeReviewRepository.findById(1L)).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> tradeReviewService.submit(1L, 1L,
                new TradeReviewSubmitRequest(TradeRating.BAD)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 평가를 제출했습니다");
    }

    // ── publishPendingReviews() 테스트 ────────────────────────────────────────

    @Test
    @DisplayName("publishPendingReviews_GOOD평가_EXP매너점수반영")
    void publishPendingReviews_GOOD평가_EXP매너점수반영() {
        TradeReview review = mockReview(1L, reviewer, target,
                LocalDateTime.now().minusHours(1), TradeRating.GOOD);
        when(tradeReviewRepository.findPendingPublish(any(LocalDateTime.class)))
                .thenReturn(List.of(review));

        tradeReviewService.publishPendingReviews();

        // publish() 호출 확인
        verify(review).publish();
        // target에게 EXP 반영 (GOOD = +15 EXP)
        verify(target).applyExp(eq(15L), any(GradeLevel.class), any());
        // target에게 매너점수 반영 (GOOD = +2)
        verify(target).applyMannerScore(2);
        // 알림 2건 (reviewer + target)
        verify(notificationService, times(2)).send(any(), any(), any(), any());
    }

    @Test
    @DisplayName("publishPendingReviews_BAD평가_EXP차감및매너점수차감")
    void publishPendingReviews_BAD평가_EXP차감및매너점수차감() {
        TradeReview review = mockReview(1L, reviewer, target,
                LocalDateTime.now().minusHours(1), TradeRating.BAD);
        when(tradeReviewRepository.findPendingPublish(any(LocalDateTime.class)))
                .thenReturn(List.of(review));

        tradeReviewService.publishPendingReviews();

        verify(review).publish();
        // BAD = -20 EXP
        verify(target).applyExp(eq(-20L), any(GradeLevel.class), any());
        // BAD = -3 매너점수
        verify(target).applyMannerScore(-3);
    }

    @Test
    @DisplayName("publishPendingReviews_NEUTRAL평가_EXP매너점수변화없음")
    void publishPendingReviews_NEUTRAL평가_EXP매너점수변화없음() {
        TradeReview review = mockReview(1L, reviewer, target,
                LocalDateTime.now().minusHours(1), TradeRating.NEUTRAL);
        when(tradeReviewRepository.findPendingPublish(any(LocalDateTime.class)))
                .thenReturn(List.of(review));

        tradeReviewService.publishPendingReviews();

        verify(review).publish();
        // NEUTRAL = EXP·매너점수 변화 없음
        verify(target, never()).applyExp(anyLong(), any(), any());
        verify(target, never()).applyMannerScore(anyInt());
    }

    @Test
    @DisplayName("publishPendingReviews_미제출평가_EXP매너점수영향없음")
    void publishPendingReviews_미제출평가_EXP매너점수영향없음() {
        // 미제출(rating=null) 평가 공개 시 효과 없음
        TradeReview review = mockReview(1L, reviewer, target,
                LocalDateTime.now().minusHours(1), null);
        when(tradeReviewRepository.findPendingPublish(any(LocalDateTime.class)))
                .thenReturn(List.of(review));

        tradeReviewService.publishPendingReviews();

        verify(review).publish();
        verify(target, never()).applyExp(anyLong(), any(), any());
        verify(target, never()).applyMannerScore(anyInt());
    }

    @Test
    @DisplayName("publishPendingReviews_대상없음_아무것도처리안함")
    void publishPendingReviews_대상없음_아무것도처리안함() {
        when(tradeReviewRepository.findPendingPublish(any(LocalDateTime.class)))
                .thenReturn(List.of());

        tradeReviewService.publishPendingReviews();

        verify(notificationService, never()).send(any(), any(), any(), any());
    }

    // ── getMyReceivedReviews() 테스트 ─────────────────────────────────────────

    @Test
    @DisplayName("getMyReceivedReviews_공개된평가목록반환")
    void getMyReceivedReviews_공개된평가목록반환() {
        TradeReview review = mockReview(1L, reviewer, target,
                LocalDateTime.now().minusDays(1), TradeRating.GOOD);
        when(tradeReviewRepository.findByTargetIdAndPublishedTrue(2L))
                .thenReturn(List.of(review));

        List<TradeReviewResponse> result = tradeReviewService.getMyReceivedReviews(2L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).rating()).isEqualTo(TradeRating.GOOD);
        assertThat(result.get(0).reviewerNickname()).isEqualTo("평가자");
    }

    @Test
    @DisplayName("getMyReceivedReviews_평가없음_빈목록반환")
    void getMyReceivedReviews_평가없음_빈목록반환() {
        when(tradeReviewRepository.findByTargetIdAndPublishedTrue(2L))
                .thenReturn(List.of());

        List<TradeReviewResponse> result = tradeReviewService.getMyReceivedReviews(2L);

        assertThat(result).isEmpty();
    }

    // ── 헬퍼 ─────────────────────────────────────────────────────────────────

    private TradeReview mockReview(Long id, User reviewer, User target,
                                   LocalDateTime revealAt, TradeRating rating) {
        TradeReview review = mock(TradeReview.class);
        when(review.getId()).thenReturn(id);
        when(review.getReviewer()).thenReturn(reviewer);
        when(review.getTarget()).thenReturn(target);
        when(review.getRevealAt()).thenReturn(revealAt);
        when(review.getRating()).thenReturn(rating);
        when(review.getCreatedAt()).thenReturn(LocalDateTime.now());
        return review;
    }
}
