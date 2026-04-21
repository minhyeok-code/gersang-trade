package org.example.gersangtrade.report.service;

import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.chat.repository.ChatMessageRepository;
import org.example.gersangtrade.domain.chat.ChatMessage;
import org.example.gersangtrade.domain.report.Report;
import org.example.gersangtrade.domain.report.enums.ReportStatus;
import org.example.gersangtrade.domain.report.enums.ReporterType;
import org.example.gersangtrade.domain.user.User;
import org.example.gersangtrade.domain.user.UserRepository;
import org.example.gersangtrade.domain.user.enums.UserStatus;
import org.example.gersangtrade.report.dto.request.ReportCreateRequest;
import org.example.gersangtrade.report.dto.request.ReportProcessRequest;
import org.example.gersangtrade.report.dto.request.UserBlockRequest;
import org.example.gersangtrade.report.dto.response.ReportResponse;
import org.example.gersangtrade.report.repository.ReportRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

/**
 * 신고 접수·처리 서비스.
 * 사용자 신고 접수, 관리자 검토·처리·기각, 사용자 차단/해제를 담당한다.
 */
@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final ChatMessageRepository chatMessageRepository;

    // ──────────────────────────────────────────────────────────────────────
    // 사용자 신고 접수
    // ──────────────────────────────────────────────────────────────────────

    /**
     * 사용자 신고 접수.
     * 자기 자신에 대한 신고는 허용하지 않는다.
     *
     * @param reporterId 신고자 사용자 ID
     * @param request    신고 요청 DTO
     * @return 생성된 신고 응답 DTO
     */
    @Transactional
    public ReportResponse fileReport(Long reporterId, ReportCreateRequest request) {
        User reporter = loadUser(reporterId);

        // 자기 자신 신고 방지 (USER 대상일 때)
        if (request.targetType().name().equals("USER") &&
                request.targetId().equals(reporterId)) {
            throw new IllegalArgumentException("자기 자신을 신고할 수 없습니다.");
        }

        Report report = Report.builder()
                .reporterType(ReporterType.USER)
                .reporter(reporter)
                .targetType(request.targetType())
                .targetId(request.targetId())
                .reasonCategory(request.reason())
                .description(request.description())
                .evidenceUrl(request.evidenceUrl())
                .chatRoomId(request.chatRoomId())
                .build();

        return ReportResponse.from(reportRepository.save(report));
    }

    // ──────────────────────────────────────────────────────────────────────
    // 관리자 신고 처리
    // ──────────────────────────────────────────────────────────────────────

    /**
     * 신고 목록 페이징 조회 (관리자용).
     * status가 null이면 전체 조회한다.
     *
     * @param status   신고 상태 필터 (null이면 전체)
     * @param pageable 페이징 정보
     * @return 신고 응답 DTO 페이지
     */
    @Transactional(readOnly = true)
    public Page<ReportResponse> listReports(ReportStatus status, Pageable pageable) {
        Page<Report> page = (status != null)
                ? reportRepository.findByStatus(status, pageable)
                : reportRepository.findAllWithDetails(pageable);
        return page.map(ReportResponse::from);
    }

    /**
     * 신고 검토 시작 — PENDING → REVIEWING.
     * 이미 다른 관리자가 선점한 경우(REVIEWING 이상) 예외를 발생시킨다.
     *
     * @param adminId  검토를 시작하는 관리자 ID
     * @param reportId 신고 ID
     * @return 업데이트된 신고 응답 DTO
     */
    @Transactional
    public ReportResponse startReview(Long adminId, Long reportId) {
        User admin = loadUser(adminId);
        Report report = loadReport(reportId);

        if (report.getStatus() != ReportStatus.PENDING) {
            throw new IllegalStateException("이미 검토 중이거나 처리 완료된 신고입니다.");
        }
        report.startReview(admin);
        return ReportResponse.from(report);
    }

    /**
     * 신고 처리 완료 — REVIEWING → PROCESSED.
     *
     * @param adminId  처리하는 관리자 ID
     * @param reportId 신고 ID
     * @param request  처리 사유 메모
     * @return 업데이트된 신고 응답 DTO
     */
    @Transactional
    public ReportResponse processReport(Long adminId, Long reportId, ReportProcessRequest request) {
        loadUser(adminId); // 관리자 존재 확인
        Report report = loadReport(reportId);

        if (report.getStatus() != ReportStatus.REVIEWING) {
            throw new IllegalStateException("검토 중 상태의 신고만 처리할 수 있습니다.");
        }
        report.process(request.adminNote());
        return ReportResponse.from(report);
    }

    /**
     * 신고 기각 — REVIEWING → DISMISSED.
     *
     * @param adminId  기각하는 관리자 ID
     * @param reportId 신고 ID
     * @param request  기각 사유 메모
     * @return 업데이트된 신고 응답 DTO
     */
    @Transactional
    public ReportResponse dismissReport(Long adminId, Long reportId, ReportProcessRequest request) {
        loadUser(adminId); // 관리자 존재 확인
        Report report = loadReport(reportId);

        if (report.getStatus() != ReportStatus.REVIEWING) {
            throw new IllegalStateException("검토 중 상태의 신고만 기각할 수 있습니다.");
        }
        report.dismiss(request.adminNote());
        return ReportResponse.from(report);
    }

    // ──────────────────────────────────────────────────────────────────────
    // 사용자 차단/해제
    // ──────────────────────────────────────────────────────────────────────

    /**
     * 사용자 차단.
     * blockedUntil이 null이면 영구 차단이다.
     *
     * @param targetUserId 차단할 사용자 ID
     * @param request      차단 사유 및 만료 일시
     */
    @Transactional
    public void blockUser(Long targetUserId, UserBlockRequest request) {
        User user = loadUser(targetUserId);
        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new IllegalStateException("이미 차단된 사용자입니다.");
        }
        user.block(request.reason(), request.blockedUntil());
    }

    /**
     * 사용자 차단 해제.
     *
     * @param targetUserId 차단 해제할 사용자 ID
     */
    @Transactional
    public void unblockUser(Long targetUserId) {
        User user = loadUser(targetUserId);
        if (user.getStatus() != UserStatus.BLOCKED) {
            throw new IllegalStateException("차단 상태가 아닌 사용자입니다.");
        }
        user.unblock();
    }

    // ──────────────────────────────────────────────────────────────────────
    // 채팅 메시지 숨김/해제
    // ──────────────────────────────────────────────────────────────────────

    /**
     * 채팅 메시지 숨김 처리.
     * 사용자 화면에 "[삭제된 메시지입니다]"로 표시된다.
     *
     * @param messageId 숨길 메시지 ID
     */
    @Transactional
    public void hideMessage(Long messageId) {
        ChatMessage message = loadChatMessage(messageId);
        if (message.isHidden()) {
            throw new IllegalStateException("이미 숨김 처리된 메시지입니다.");
        }
        message.hide();
    }

    /**
     * 채팅 메시지 숨김 해제 (오탐 DISMISS 시 복원).
     *
     * @param messageId 숨김 해제할 메시지 ID
     */
    @Transactional
    public void unhideMessage(Long messageId) {
        ChatMessage message = loadChatMessage(messageId);
        if (!message.isHidden()) {
            throw new IllegalStateException("숨김 처리되지 않은 메시지입니다.");
        }
        message.unhide();
    }

    // ──────────────────────────────────────────────────────────────────────
    // 내부 헬퍼
    // ──────────────────────────────────────────────────────────────────────

    private User loadUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다."));
    }

    private Report loadReport(Long reportId) {
        return reportRepository.findById(reportId)
                .orElseThrow(() -> new NoSuchElementException("신고를 찾을 수 없습니다."));
    }

    private ChatMessage loadChatMessage(Long messageId) {
        return chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new NoSuchElementException("메시지를 찾을 수 없습니다."));
    }
}
