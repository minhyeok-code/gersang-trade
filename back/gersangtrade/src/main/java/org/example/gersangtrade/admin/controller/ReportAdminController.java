package org.example.gersangtrade.admin.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.auth.security.CustomOAuth2UserDetails;
import org.example.gersangtrade.domain.report.enums.ReportStatus;
import org.example.gersangtrade.report.dto.request.ReportProcessRequest;
import org.example.gersangtrade.report.dto.request.UserBlockRequest;
import org.example.gersangtrade.report.dto.response.ReportResponse;
import org.example.gersangtrade.report.service.ReportService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 신고 처리·사용자 제재·메시지 관리 관리자 API 컨트롤러.
 *
 * <pre>
 * GET    /admin/reports                       — 신고 목록 조회 (상태 필터)
 * PATCH  /admin/reports/{id}/review           — 신고 검토 시작 (PENDING → REVIEWING)
 * PATCH  /admin/reports/{id}/process          — 신고 처리 완료 (REVIEWING → PROCESSED)
 * PATCH  /admin/reports/{id}/dismiss          — 신고 기각 (REVIEWING → DISMISSED)
 * POST   /admin/users/{userId}/block          — 사용자 차단
 * POST   /admin/users/{userId}/unblock        — 사용자 차단 해제
 * PATCH  /admin/messages/{messageId}/hide     — 채팅 메시지 숨김
 * PATCH  /admin/messages/{messageId}/unhide   — 채팅 메시지 숨김 해제
 * </pre>
 *
 * <p>모든 엔드포인트는 ADMIN 권한이 필요하다.
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ReportAdminController {

    private final ReportService reportService;

    /**
     * 신고 목록 조회.
     * status 파라미터가 없으면 전체 신고를 반환한다.
     *
     * @param status   신고 상태 필터 (선택)
     * @param pageable 페이징 정보
     * @return 신고 응답 DTO 페이지
     */
    @GetMapping("/reports")
    public ResponseEntity<Page<ReportResponse>> listReports(
            @RequestParam(required = false) ReportStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(reportService.listReports(status, pageable));
    }

    /**
     * 신고 검토 시작 — PENDING → REVIEWING.
     *
     * @param principal 관리자 인증 정보
     * @param reportId  신고 ID
     * @return 업데이트된 신고 응답 DTO
     */
    @PatchMapping("/reports/{reportId}/review")
    public ResponseEntity<ReportResponse> startReview(
            @AuthenticationPrincipal CustomOAuth2UserDetails principal,
            @PathVariable Long reportId) {
        Long adminId = principal.getUser().getId();
        return ResponseEntity.ok(reportService.startReview(adminId, reportId));
    }

    /**
     * 신고 처리 완료 — REVIEWING → PROCESSED.
     *
     * @param principal 관리자 인증 정보
     * @param reportId  신고 ID
     * @param request   처리 사유
     * @return 업데이트된 신고 응답 DTO
     */
    @PatchMapping("/reports/{reportId}/process")
    public ResponseEntity<ReportResponse> processReport(
            @AuthenticationPrincipal CustomOAuth2UserDetails principal,
            @PathVariable Long reportId,
            @Valid @RequestBody ReportProcessRequest request) {
        Long adminId = principal.getUser().getId();
        return ResponseEntity.ok(reportService.processReport(adminId, reportId, request));
    }

    /**
     * 신고 기각 — REVIEWING → DISMISSED.
     *
     * @param principal 관리자 인증 정보
     * @param reportId  신고 ID
     * @param request   기각 사유
     * @return 업데이트된 신고 응답 DTO
     */
    @PatchMapping("/reports/{reportId}/dismiss")
    public ResponseEntity<ReportResponse> dismissReport(
            @AuthenticationPrincipal CustomOAuth2UserDetails principal,
            @PathVariable Long reportId,
            @Valid @RequestBody ReportProcessRequest request) {
        Long adminId = principal.getUser().getId();
        return ResponseEntity.ok(reportService.dismissReport(adminId, reportId, request));
    }

    /**
     * 사용자 차단.
     *
     * @param targetUserId 차단할 사용자 ID
     * @param request      차단 사유 및 만료 일시
     * @return 204 No Content
     */
    @PostMapping("/users/{userId}/block")
    public ResponseEntity<Void> blockUser(
            @PathVariable("userId") Long targetUserId,
            @Valid @RequestBody UserBlockRequest request) {
        reportService.blockUser(targetUserId, request);
        return ResponseEntity.noContent().build();
    }

    /**
     * 사용자 차단 해제.
     *
     * @param targetUserId 차단 해제할 사용자 ID
     * @return 204 No Content
     */
    @PostMapping("/users/{userId}/unblock")
    public ResponseEntity<Void> unblockUser(
            @PathVariable("userId") Long targetUserId) {
        reportService.unblockUser(targetUserId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 채팅 메시지 숨김 처리.
     *
     * @param messageId 숨길 메시지 ID
     * @return 204 No Content
     */
    @PatchMapping("/messages/{messageId}/hide")
    public ResponseEntity<Void> hideMessage(@PathVariable Long messageId) {
        reportService.hideMessage(messageId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 채팅 메시지 숨김 해제.
     *
     * @param messageId 숨김 해제할 메시지 ID
     * @return 204 No Content
     */
    @PatchMapping("/messages/{messageId}/unhide")
    public ResponseEntity<Void> unhideMessage(@PathVariable Long messageId) {
        reportService.unhideMessage(messageId);
        return ResponseEntity.noContent().build();
    }
}
