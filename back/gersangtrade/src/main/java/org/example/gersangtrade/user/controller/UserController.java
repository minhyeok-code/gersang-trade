package org.example.gersangtrade.user.controller;

import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.example.gersangtrade.listing.dto.response.ListingSummaryResponse;
import org.example.gersangtrade.trade.dto.response.TradeHistoryResponse;
import org.example.gersangtrade.trade.dto.response.TradeReviewResponse;
import org.example.gersangtrade.trade.service.TradeReviewService;
import org.example.gersangtrade.user.dto.request.ClearTimeRequest;
import org.example.gersangtrade.user.dto.request.UserProfileUpdateRequest;
import org.example.gersangtrade.user.dto.request.UserServerUpdateRequest;
import org.example.gersangtrade.user.dto.response.ClearTimeResponse;
import org.example.gersangtrade.user.dto.response.PublicUserProfileResponse;
import org.example.gersangtrade.user.dto.response.UserProfileResponse;
import org.example.gersangtrade.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 사용자 프로필·마이페이지 API 컨트롤러.
 *
 * <pre>
 * GET    /api/users/me                — 내 프로필 조회 (로그인 필수)
 * PATCH  /api/users/me                — 내 프로필 수정 (로그인 필수)
 * GET    /api/users/me/listings       — 내 등록글 목록 조회 (로그인 필수)
 * GET    /api/users/me/trades        — 내 거래 확정 내역 조회 (로그인 필수)
 * PATCH  /api/users/me/server         — 기본 서버 변경 (로그인 필수)
 * DELETE /api/users/me               — 회원 탈퇴 (소프트 삭제, 로그인 필수)
 * GET    /api/users/{userId}          — 타 유저 공개 프로필 (인증 불필요)
 * GET    /api/users/{userId}/reviews  — 타 유저 공개 평가 목록 (인증 불필요)
 * </pre>
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final TradeReviewService tradeReviewService;

    /**
     * 내 프로필 조회.
     *
     * @param principal 로그인된 사용자 인증 정보
     * @return 사용자 프로필 DTO
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserProfileResponse> getMyProfile(
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(userService.getUserProfile(userId));
    }

    /**
     * 내 등록글 목록 조회.
     * 소프트 삭제되지 않은 활성 등록글을 최신순으로 반환한다.
     *
     * @param principal 로그인된 사용자 인증 정보
     * @return 등록글 요약 응답 목록
     */
    @GetMapping("/me/listings")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ListingSummaryResponse>> getMyListings(
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(userService.getMyListings(userId));
    }

    /**
     * 내 거래 확정 내역 조회.
     * 판매자·구매자 양쪽으로 참여한 완료 거래를 모두 반환한다.
     *
     * @param userId 로그인된 사용자 인증 정보
     * @return 거래 확정 내역 목록
     */
    @GetMapping("/me/trades")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TradeHistoryResponse>> getMyTradeHistory(
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(userService.getMyTradeHistory(userId));
    }

    /**
     * 내 프로필 수정.
     * null 필드는 변경하지 않으므로 수정할 필드만 전송하면 된다.
     *
     * @param principal 로그인된 사용자 인증 정보
     * @param request   수정할 프로필 필드 (nickname, gameNickname, gameAccessTime)
     * @return 변경 반영된 프로필 DTO
     */
    @PatchMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @AuthenticationPrincipal Long userId,
            @RequestBody UserProfileUpdateRequest request) {
        return ResponseEntity.ok(userService.updateProfile(userId, request));
    }

    /**
     * 기본 서버 변경.
     * serverId를 null로 전송하면 서버 선택이 해제되어 전체 서버 조회 모드로 전환된다.
     *
     * @param principal 로그인된 사용자 인증 정보
     * @param request   서버 ID (null 허용)
     * @return 변경 반영된 프로필 DTO
     */
    @PatchMapping("/me/server")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserProfileResponse> updateServer(
            @AuthenticationPrincipal Long userId,
            @RequestBody UserServerUpdateRequest request) {
        return ResponseEntity.ok(userService.updateServer(userId, request));
    }

    /**
     * 회원 탈퇴 (소프트 삭제).
     * 탈퇴 후 데이터는 1년간 보관되며 배치 Job이 하드딜리트를 수행한다.
     *
     * @param principal 로그인된 사용자 인증 정보
     * @return 204 No Content
     */
    @DeleteMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> withdrawal(
            @AuthenticationPrincipal Long userId) {
        userService.withdrawal(userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 클리어타임 저장.
     * 몬스터 클리어 기록을 저장하고 데이터 기여 보상으로 EXP를 지급한다.
     */
    @PostMapping("/me/clear-time")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ClearTimeResponse> saveClearTime(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid ClearTimeRequest request) {
        return ResponseEntity.ok(userService.saveClearTime(userId, request));
    }

    /**
     * 타 유저 공개 프로필 조회.
     * 이메일·OAuth 정보를 제외한 공개 필드만 반환한다.
     *
     * @param userId 조회할 유저 ID
     * @return 공개 프로필 DTO
     */
    @GetMapping("/{userId}")
    public ResponseEntity<PublicUserProfileResponse> getPublicProfile(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getPublicProfile(userId));
    }

    /**
     * 타 유저가 받은 공개된 거래 평가 목록.
     * published=true인 평가만 반환한다.
     *
     * @param userId 조회할 유저 ID
     * @return 공개된 거래 평가 목록
     */
    @GetMapping("/{userId}/reviews")
    public ResponseEntity<List<TradeReviewResponse>> getUserReviews(@PathVariable Long userId) {
        return ResponseEntity.ok(tradeReviewService.getPublishedReviews(userId));
    }
}
