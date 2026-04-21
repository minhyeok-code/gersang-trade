package org.example.gersangtrade.report.service;

import org.example.gersangtrade.chat.repository.ChatMessageRepository;
import org.example.gersangtrade.domain.chat.ChatMessage;
import org.example.gersangtrade.domain.report.Report;
import org.example.gersangtrade.domain.report.enums.ReportReason;
import org.example.gersangtrade.domain.report.enums.ReportStatus;
import org.example.gersangtrade.domain.report.enums.ReportTargetType;
import org.example.gersangtrade.domain.report.enums.ReporterType;
import org.example.gersangtrade.domain.user.User;
import org.example.gersangtrade.domain.user.UserRepository;
import org.example.gersangtrade.domain.user.enums.Role;
import org.example.gersangtrade.domain.user.enums.UserStatus;
import org.example.gersangtrade.report.dto.request.ReportCreateRequest;
import org.example.gersangtrade.report.dto.request.ReportProcessRequest;
import org.example.gersangtrade.report.dto.request.UserBlockRequest;
import org.example.gersangtrade.report.dto.response.ReportResponse;
import org.example.gersangtrade.report.repository.ReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * ReportService 단위 테스트.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ReportService")
class ReportServiceTest {

    @Mock private ReportRepository reportRepository;
    @Mock private UserRepository userRepository;
    @Mock private ChatMessageRepository chatMessageRepository;

    @InjectMocks
    private ReportService reportService;

    private User reporter;
    private User admin;
    private User targetUser;

    @BeforeEach
    void setUp() {
        reporter = spy(User.builder()
                .oauthProvider("google").oauthId("r1")
                .nickname("신고자").email("r@test.com")
                .role(Role.USER).status(UserStatus.ACTIVE)
                .build());
        doReturn(1L).when(reporter).getId();

        admin = spy(User.builder()
                .oauthProvider("google").oauthId("a1")
                .nickname("관리자").email("a@test.com")
                .role(Role.ADMIN).status(UserStatus.ACTIVE)
                .build());
        doReturn(2L).when(admin).getId();

        targetUser = spy(User.builder()
                .oauthProvider("google").oauthId("t1")
                .nickname("피신고자").email("t@test.com")
                .role(Role.USER).status(UserStatus.ACTIVE)
                .build());
        doReturn(3L).when(targetUser).getId();
    }

    // ──────────────────────────────────────────────────────────────────────
    // fileReport
    // ──────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("fileReport — 신고 접수")
    class FileReport {

        @Test
        @DisplayName("정상 신고 접수 — USER 신고 생성")
        void success() {
            // given
            given(userRepository.findById(1L)).willReturn(Optional.of(reporter));
            given(reportRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            ReportCreateRequest req = new ReportCreateRequest(
                    ReportTargetType.USER, 3L,
                    ReportReason.FRAUD, "사기 의심", null, null);

            // when
            ReportResponse resp = reportService.fileReport(1L, req);

            // then
            assertThat(resp.reporterType()).isEqualTo(ReporterType.USER);
            assertThat(resp.targetType()).isEqualTo(ReportTargetType.USER);
            assertThat(resp.targetId()).isEqualTo(3L);
            assertThat(resp.status()).isEqualTo(ReportStatus.PENDING);
        }

        @Test
        @DisplayName("자기 자신 신고 — IllegalArgumentException")
        void selfReport_throws() {
            // given
            given(userRepository.findById(1L)).willReturn(Optional.of(reporter));

            ReportCreateRequest req = new ReportCreateRequest(
                    ReportTargetType.USER, 1L, // 자기 자신
                    ReportReason.ABUSE, "설명", null, null);

            // when & then
            assertThatThrownBy(() -> reportService.fileReport(1L, req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("자기 자신");
        }

        @Test
        @DisplayName("존재하지 않는 신고자 — NoSuchElementException")
        void reporterNotFound_throws() {
            // given
            given(userRepository.findById(99L)).willReturn(Optional.empty());

            ReportCreateRequest req = new ReportCreateRequest(
                    ReportTargetType.USER, 3L,
                    ReportReason.FRAUD, "설명", null, null);

            // when & then
            assertThatThrownBy(() -> reportService.fileReport(99L, req))
                    .isInstanceOf(NoSuchElementException.class);
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // startReview
    // ──────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("startReview — 검토 시작")
    class StartReview {

        @Test
        @DisplayName("PENDING 신고 → REVIEWING 전이 성공")
        void success() {
            // given
            Report report = pendingReport();
            given(userRepository.findById(2L)).willReturn(Optional.of(admin));
            given(reportRepository.findById(100L)).willReturn(Optional.of(report));

            // when
            ReportResponse resp = reportService.startReview(2L, 100L);

            // then
            assertThat(resp.status()).isEqualTo(ReportStatus.REVIEWING);
        }

        @Test
        @DisplayName("이미 REVIEWING 상태 — IllegalStateException")
        void alreadyReviewing_throws() {
            // given
            Report report = pendingReport();
            report.startReview(admin); // 이미 검토 시작
            given(userRepository.findById(2L)).willReturn(Optional.of(admin));
            given(reportRepository.findById(100L)).willReturn(Optional.of(report));

            // when & then
            assertThatThrownBy(() -> reportService.startReview(2L, 100L))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // processReport / dismissReport
    // ──────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("processReport / dismissReport")
    class ProcessDismiss {

        @Test
        @DisplayName("REVIEWING 신고 처리 완료 → PROCESSED")
        void processSuccess() {
            // given
            Report report = reviewingReport();
            given(userRepository.findById(2L)).willReturn(Optional.of(admin));
            given(reportRepository.findById(100L)).willReturn(Optional.of(report));

            // when
            ReportResponse resp = reportService.processReport(
                    2L, 100L, new ReportProcessRequest("처리 완료"));

            // then
            assertThat(resp.status()).isEqualTo(ReportStatus.PROCESSED);
            assertThat(resp.adminNote()).isEqualTo("처리 완료");
        }

        @Test
        @DisplayName("REVIEWING 신고 기각 → DISMISSED")
        void dismissSuccess() {
            // given
            Report report = reviewingReport();
            given(userRepository.findById(2L)).willReturn(Optional.of(admin));
            given(reportRepository.findById(100L)).willReturn(Optional.of(report));

            // when
            ReportResponse resp = reportService.dismissReport(
                    2L, 100L, new ReportProcessRequest("오탐 기각"));

            // then
            assertThat(resp.status()).isEqualTo(ReportStatus.DISMISSED);
        }

        @Test
        @DisplayName("PENDING 신고 처리 시도 → IllegalStateException")
        void processPending_throws() {
            // given
            Report report = pendingReport(); // PENDING 상태
            given(userRepository.findById(2L)).willReturn(Optional.of(admin));
            given(reportRepository.findById(100L)).willReturn(Optional.of(report));

            // when & then
            assertThatThrownBy(() -> reportService.processReport(
                    2L, 100L, new ReportProcessRequest("메모")))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // blockUser / unblockUser
    // ──────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("blockUser / unblockUser")
    class BlockUnblock {

        @Test
        @DisplayName("ACTIVE 사용자 차단 성공")
        void blockSuccess() {
            // given
            given(userRepository.findById(3L)).willReturn(Optional.of(targetUser));

            // when
            reportService.blockUser(3L, new UserBlockRequest("불량 사용자", null));

            // then
            assertThat(targetUser.getStatus()).isEqualTo(UserStatus.BLOCKED);
        }

        @Test
        @DisplayName("이미 BLOCKED 사용자 차단 시도 → IllegalStateException")
        void blockAlreadyBlocked_throws() {
            // given
            targetUser.block("기존 차단", null);
            given(userRepository.findById(3L)).willReturn(Optional.of(targetUser));

            // when & then
            assertThatThrownBy(() -> reportService.blockUser(
                    3L, new UserBlockRequest("재차단", null)))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("BLOCKED 사용자 차단 해제 성공")
        void unblockSuccess() {
            // given
            targetUser.block("차단", null);
            given(userRepository.findById(3L)).willReturn(Optional.of(targetUser));

            // when
            reportService.unblockUser(3L);

            // then
            assertThat(targetUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
        }

        @Test
        @DisplayName("차단 상태 아닌 사용자 해제 시도 → IllegalStateException")
        void unblockActiveUser_throws() {
            // given
            given(userRepository.findById(3L)).willReturn(Optional.of(targetUser));

            // when & then
            assertThatThrownBy(() -> reportService.unblockUser(3L))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // hideMessage / unhideMessage
    // ──────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("hideMessage / unhideMessage")
    class HideUnhide {

        @Test
        @DisplayName("메시지 숨김 처리 성공")
        void hideSuccess() {
            // given
            ChatMessage msg = mock(ChatMessage.class);
            given(msg.isHidden()).willReturn(false);
            given(chatMessageRepository.findById(50L)).willReturn(Optional.of(msg));

            // when
            reportService.hideMessage(50L);

            // then
            verify(msg).hide();
        }

        @Test
        @DisplayName("이미 숨김 처리된 메시지 — IllegalStateException")
        void hideAlreadyHidden_throws() {
            // given
            ChatMessage msg = mock(ChatMessage.class);
            given(msg.isHidden()).willReturn(true);
            given(chatMessageRepository.findById(50L)).willReturn(Optional.of(msg));

            // when & then
            assertThatThrownBy(() -> reportService.hideMessage(50L))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("메시지 숨김 해제 성공")
        void unhideSuccess() {
            // given
            ChatMessage msg = mock(ChatMessage.class);
            given(msg.isHidden()).willReturn(true);
            given(chatMessageRepository.findById(50L)).willReturn(Optional.of(msg));

            // when
            reportService.unhideMessage(50L);

            // then
            verify(msg).unhide();
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // 헬퍼 — 테스트용 Report 생성
    // ──────────────────────────────────────────────────────────────────────

    private Report pendingReport() {
        return Report.builder()
                .reporterType(ReporterType.USER)
                .reporter(reporter)
                .targetType(ReportTargetType.USER)
                .targetId(3L)
                .reasonCategory(ReportReason.FRAUD)
                .description("테스트 신고")
                .build();
    }

    private Report reviewingReport() {
        Report report = pendingReport();
        report.startReview(admin);
        return report;
    }
}
