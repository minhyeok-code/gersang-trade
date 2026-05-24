package org.example.gersangtrade.user.controller;

import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.domain.user.enums.GradeLevel;
import org.example.gersangtrade.user.dto.response.GradePolicyResponse;
import org.example.gersangtrade.user.dto.response.MyGradeResponse;
import org.example.gersangtrade.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

/**
 * 유저 등급 API 컨트롤러.
 *
 * <pre>
 * GET /api/grades        — 등급 정책 목록 (툴팁용, 인증 불필요)
 * GET /api/users/me/grade — 내 등급·경험치 조회 (로그인 필수)
 * </pre>
 */
@RestController
@RequiredArgsConstructor
public class GradeController {

    private final UserService userService;

    /**
     * 등급 정책 목록 조회.
     * 낮은 등급(행상)부터 높은 등급(거상) 순으로 반환한다.
     */
    @GetMapping("/api/grades")
    public ResponseEntity<List<GradePolicyResponse>> getGradePolicies() {
        List<GradePolicyResponse> policies = Arrays.stream(GradeLevel.values())
                .sorted((a, b) -> Integer.compare(b.getGradeNumber(), a.getGradeNumber()))
                .map(GradePolicyResponse::from)
                .toList();
        return ResponseEntity.ok(policies);
    }

    /**
     * 내 등급·경험치 조회.
     * GET /api/users/me 응답과 중복되나 등급 패널 전용으로 분리한다.
     */
    @GetMapping("/api/users/me/grade")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MyGradeResponse> getMyGrade(
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(userService.getMyGrade(userId));
    }
}
