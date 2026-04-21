package org.example.gersangtrade.user.controller;

import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.auth.security.CustomOAuth2UserDetails;
import org.example.gersangtrade.listing.dto.response.ListingSummaryResponse;
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
 * GET    /api/users/me           — 내 프로필 조회 (로그인 필수)
 * GET    /api/users/me/listings  — 내 등록글 목록 조회 (로그인 필수)
 * DELETE /api/users/me          — 회원 탈퇴 (소프트 삭제, 로그인 필수)
 * </pre>
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 내 프로필 조회.
     *
     * @param principal 로그인된 사용자 인증 정보
     * @return 사용자 프로필 DTO
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserProfileResponse> getMyProfile(
            @AuthenticationPrincipal CustomOAuth2UserDetails principal) {
        Long userId = principal.getUser().getId();
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
            @AuthenticationPrincipal CustomOAuth2UserDetails principal) {
        Long userId = principal.getUser().getId();
        return ResponseEntity.ok(userService.getMyListings(userId));
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
            @AuthenticationPrincipal CustomOAuth2UserDetails principal) {
        Long userId = principal.getUser().getId();
        userService.withdrawal(userId);
        return ResponseEntity.noContent().build();
    }
}
