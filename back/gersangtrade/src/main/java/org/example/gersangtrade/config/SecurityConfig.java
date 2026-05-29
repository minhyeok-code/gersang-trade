package org.example.gersangtrade.config;

import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.auth.filter.JwtAuthenticationFilter;
import org.example.gersangtrade.auth.handler.OAuth2LoginFailureHandler;
import org.example.gersangtrade.auth.handler.OAuth2LoginSuccessHandler;
import org.example.gersangtrade.auth.jwt.JwtTokenizer;
import org.example.gersangtrade.auth.service.CustomOAuth2UserService;
import org.example.gersangtrade.auth.service.CustomOidcUserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.List;

/**
 * Spring Security 설정.
 *
 * 인증 흐름:
 *   1. 비로그인: GET /listings, GET /stats 등 공개 API → 허용
 *   2. OAuth2 로그인: /oauth2/authorization/google → Google → 콜백 → SuccessHandler → JWT 발급
 *   3. 이후 API 요청: Authorization: Bearer {AT} → JwtAuthenticationFilter → SecurityContext 설정
 *   4. AT 만료: GET /auth/refresh (RT 쿠키) → 새 AT + 새 RT 발급 (RTR 패턴)
 *   5. 로그아웃: POST /auth/logout → RT DB 삭제 + 쿠키 만료
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // @PreAuthorize 어노테이션 활성화
@RequiredArgsConstructor
@Order(2)
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final CustomOidcUserService customOidcUserService;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final OAuth2LoginFailureHandler oAuth2LoginFailureHandler;
    private final JwtTokenizer jwtTokenizer;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // REST API는 CSRF 불필요 (stateless + Bearer 토큰 방식)
                .csrf(AbstractHttpConfigurer::disable)

                // CORS — WebSocket upgrade(ws://127.0.0.1:8080)는 cross-origin이므로 명시 필요
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // OAuth2 흐름(state 검증)에는 세션이 필요하므로 IF_REQUIRED 사용
                // JWT로 인증하는 API 요청에서는 세션을 생성하지 않음
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))

                // TODO: 로컬 개발용 — 운영 전 권한 설정 복구 필요
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())

                // OAuth2 소셜 로그인 설정 (MVP 범위: Google + Naver)
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)       // Naver (non-OIDC)
                                .oidcUserService(customOidcUserService))    // Google (OIDC)
                        .successHandler(oAuth2LoginSuccessHandler)
                        .failureHandler(oAuth2LoginFailureHandler)
                )

                // JWT 인증 필터를 UsernamePasswordAuthenticationFilter 앞에 삽입
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtTokenizer),
                        UsernamePasswordAuthenticationFilter.class
                )

                // 미인증 요청 시 로그인 페이지 대신 401 JSON 반환
                .exceptionHandling(ex ->
                        ex.authenticationEntryPoint(unauthorizedEntryPoint()));

        return http.build();
    }

    /**
     * CORS 설정 — REST API 및 WebSocket upgrade 요청 모두 허용.
     * WebSocket은 ws://127.0.0.1:8080 으로 직접 연결하므로 cross-origin 처리 필수.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * 인증 실패(401) 시 JSON 응답 반환.
     * 기본 동작(로그인 페이지 리다이렉트)을 REST API 방식으로 교체.
     */
    @Bean
    public AuthenticationEntryPoint unauthorizedEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("""
                    {"error": "unauthorized", "message": "로그인이 필요합니다."}
                    """.strip());
        };
    }
}
