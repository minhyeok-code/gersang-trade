package org.example.gersangtrade.auth.cookie;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 리프레시 토큰 쿠키 설정/만료를 일원화하는 컴포넌트.
 * 쿠키 관련 설정(secure, path 등)을 한 곳에서 관리하여 일관성을 보장한다.
 */
@Component
public class RefreshTokenCookieManager {

    private static final String RT_COOKIE_NAME = "refreshToken";
    private static final String RT_COOKIE_PATH = "/";

    /** 운영 환경에서는 true (HTTPS 필수). 로컬 개발 시 false. */
    @Value("${app.cookie.secure:false}")
    private boolean secure;

    /**
     * RT 쿠키를 응답에 추가한다.
     *
     * @param response       HTTP 응답 객체
     * @param token          리프레시 토큰 값
     * @param maxAgeSeconds  쿠키 유효 시간 (초)
     */
    public void set(HttpServletResponse response, String token, long maxAgeSeconds) {
        Cookie cookie = new Cookie(RT_COOKIE_NAME, token);
        cookie.setHttpOnly(true);           // JavaScript 접근 불가 (XSS 방어)
        cookie.setSecure(secure);           // 환경변수 app.cookie.secure로 제어
        cookie.setPath(RT_COOKIE_PATH);     // /auth 경로에서만 쿠키 전송
        cookie.setMaxAge((int) maxAgeSeconds);
        response.addCookie(cookie);
    }

    /**
     * RT 쿠키를 즉시 만료시킨다 (로그아웃 시 사용).
     */
    public void expire(HttpServletResponse response) {
        Cookie cookie = new Cookie(RT_COOKIE_NAME, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(secure);
        cookie.setPath(RT_COOKIE_PATH);
        cookie.setMaxAge(0);    // 즉시 만료
        response.addCookie(cookie);
    }
}
