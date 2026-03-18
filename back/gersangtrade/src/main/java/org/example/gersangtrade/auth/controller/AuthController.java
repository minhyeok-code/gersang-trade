package org.example.gersangtrade.auth.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.auth.dto.TokenResponse;
import org.example.gersangtrade.auth.jwt.JwtTokenizer;
import org.example.gersangtrade.auth.service.AuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

/**
 * 인증 관련 API 컨트롤러.
 *
 * POST /auth/refresh — RT 쿠키로 AT(+ 새 RT) 재발급
 * POST /auth/logout  — 로그아웃 (RT DB 삭제 + 쿠키 만료)
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String RT_COOKIE_NAME = "refreshToken";
    private final AuthService authService;
    private final JwtTokenizer jwtTokenizer;

    /** 운영 환경에서는 true (HTTPS 필수). 로컬 개발 시 false. */
    @Value("${app.cookie.secure:false}")
    private boolean cookieSecure;

    /**
     * 액세스 토큰 재발급.
     * RTR 패턴: 기존 RT를 무효화하고 새 AT + 새 RT를 동시에 발급한다.
     *
     * Request:  Cookie: refreshToken={RT}
     * Response: body { accessToken: "..." }
     *           Cookie: refreshToken={새 RT} (HttpOnly, /auth path)
     */
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String refreshToken = extractRefreshTokenFromCookie(request);

        AuthService.RefreshResult result = authService.refresh(refreshToken);

        // 새 RT를 HttpOnly Cookie에 갱신
        addRefreshTokenCookie(response, result.refreshToken());

        return ResponseEntity.ok(new TokenResponse(result.accessToken()));
    }

    /**
     * 로그아웃.
     * DB에서 RT를 삭제하고 클라이언트 쿠키를 만료시킨다.
     * AT는 서버에서 즉시 무효화 불가 — 클라이언트에서 메모리/스토리지에서 즉시 폐기해야 한다.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = extractRefreshTokenFromCookie(request);
        authService.logout(refreshToken);

        // 쿠키 만료 처리 (maxAge=0)
        expireRefreshTokenCookie(response);
        return ResponseEntity.noContent().build();
    }

    // ── 쿠키 유틸 ──────────────────────────────────────────────────────────

    private String extractRefreshTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            throw new org.springframework.security.authentication.BadCredentialsException(
                    "리프레시 토큰 쿠키가 없습니다.");
        }
        return Arrays.stream(cookies)
                .filter(c -> RT_COOKIE_NAME.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElseThrow(() -> new org.springframework.security.authentication.BadCredentialsException(
                        "리프레시 토큰 쿠키가 없습니다."));
    }

    private void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie(RT_COOKIE_NAME, refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieSecure);  // app.cookie.secure 설정값으로 제어
        cookie.setPath("/auth");
        // JwtTokenizer와 동일한 만료시간 사용 (하드코딩 방지)
        cookie.setMaxAge((int) (jwtTokenizer.getRefreshTokenExpiryMs() / 1000));
        response.addCookie(cookie);
    }

    private void expireRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(RT_COOKIE_NAME, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieSecure);
        cookie.setPath("/auth");
        cookie.setMaxAge(0);  // 즉시 만료
        response.addCookie(cookie);
    }
}
