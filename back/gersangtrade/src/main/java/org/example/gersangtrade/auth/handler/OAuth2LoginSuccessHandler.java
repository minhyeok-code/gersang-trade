package org.example.gersangtrade.auth.handler;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gersangtrade.auth.jwt.JwtTokenizer;
import org.example.gersangtrade.auth.security.CustomOAuth2UserDetails;
import org.example.gersangtrade.auth.service.AuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * OAuth2 로그인 성공 시 JWT(AT + RT)를 발급하는 핸들러.
 *
 * 주의: Spring Security는 이 핸들러를 AOP 프록시 없이 직접 호출하므로
 * @Transactional을 여기에 붙여도 동작하지 않는다.
 * 트랜잭션이 필요한 DB 작업은 @Service 빈인 AuthService에 위임한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final String RT_COOKIE_NAME = "refreshToken";

    private final JwtTokenizer jwtTokenizer;
    private final AuthService authService;

    @Value("${app.oauth2.authorized-redirect-uri}")
    private String authorizedRedirectUri;

    /** 운영 환경에서는 true (HTTPS 필수). 로컬 개발 시 false. */
    @Value("${app.cookie.secure:false}")
    private boolean cookieSecure;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {

        CustomOAuth2UserDetails userDetails = (CustomOAuth2UserDetails) authentication.getPrincipal();
        Long userId = userDetails.getUser().getId();
        String role = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .map(a -> a.replace("ROLE_", ""))
                .orElse("USER");

        // AT 생성
        String accessToken = jwtTokenizer.createAccessToken(userId, role);

        // RT 생성 + DB 저장 — @Transactional이 보장된 AuthService에 위임
        String refreshToken = authService.issueRefreshToken(userId);

        // RT를 HttpOnly Cookie에 설정 (XSS로부터 보호)
        addRefreshTokenCookie(response, refreshToken);

        // AT를 쿼리 파라미터로 포함하여 프론트엔드로 리다이렉트
        String redirectUrl = UriComponentsBuilder.fromUriString(authorizedRedirectUri)
                .queryParam("accessToken", accessToken)
                .build().toUriString();

        log.debug("OAuth2 로그인 성공: userId={}, redirect={}", userId, authorizedRedirectUri);
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }

    /** RT를 HttpOnly Cookie에 담는다. */
    private void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie(RT_COOKIE_NAME, refreshToken);
        cookie.setHttpOnly(true);       // JavaScript에서 접근 불가 (XSS 방어)
        cookie.setSecure(cookieSecure); // app.cookie.secure 설정값으로 제어
        cookie.setPath("/auth");        // /auth 경로에서만 쿠키 전송 (최소 노출)
        cookie.setMaxAge((int) (jwtTokenizer.getRefreshTokenExpiryMs() / 1000));
        response.addCookie(cookie);
    }
}
