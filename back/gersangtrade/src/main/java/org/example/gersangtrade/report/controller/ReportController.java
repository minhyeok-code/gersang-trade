package org.example.gersangtrade.report.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.auth.security.CustomOAuth2UserDetails;
import org.example.gersangtrade.report.dto.request.ReportCreateRequest;
import org.example.gersangtrade.report.dto.response.ReportResponse;
import org.example.gersangtrade.report.service.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

/**
 * 사용자 신고 접수 API 컨트롤러.
 *
 * <pre>
 * POST /api/reports  — 신고 접수 (로그인 필수)
 * </pre>
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /**
     * 신고 접수.
     * 로그인한 사용자가 다른 사용자·게시물·채팅 메시지를 신고한다.
     *
     * @param principal 로그인된 사용자 인증 정보
     * @param request   신고 요청 DTO
     * @return 201 Created + 생성된 신고 응답 DTO
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReportResponse> fileReport(
            @AuthenticationPrincipal CustomOAuth2UserDetails principal,
            @Valid @RequestBody ReportCreateRequest request) {
        Long reporterId = principal.getUser().getId();
        ReportResponse response = reportService.fileReport(reporterId, request);
        return ResponseEntity.created(URI.create("/api/reports/" + response.id()))
                .body(response);
    }
}
