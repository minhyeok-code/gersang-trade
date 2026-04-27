package org.example.gersangtrade.config;

import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.auth.filter.JwtAuthenticationFilter;
import org.example.gersangtrade.auth.handler.OAuth2LoginFailureHandler;
import org.example.gersangtrade.auth.handler.OAuth2LoginSuccessHandler;
import org.example.gersangtrade.auth.jwt.JwtTokenizer;
import org.example.gersangtrade.auth.service.CustomOAuth2UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;

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
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final OAuth2LoginFailureHandler oAuth2LoginFailureHandler;
    private final JwtTokenizer jwtTokenizer;

    /** 로컬 개발 전용 — true이면 /admin/** 인증 없이 허용 (application-local.yml) */
    @Value("${local.security.permit-admin:false}")
    private boolean permitAdmin;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // REST API는 CSRF 불필요 (stateless + Bearer 토큰 방식)
                .csrf(AbstractHttpConfigurer::disable)

                // JWT 기반 stateless 세션 — 서버에 세션 생성하지 않음
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 경로별 접근 권한 설정
                .authorizeHttpRequests(auth -> {
                    // 공개 API — 비로그인도 조회 가능
                    auth.requestMatchers(HttpMethod.GET,
                            "/listings/**", "/stats/**",
                            "/api/listings/**", "/api/wanted/**",
                            "/api/items/**").permitAll();
                    // 가성비 계산기 — 비로그인 허용 (세션 내 계산만, DB 저장 없음)
                    auth.requestMatchers(HttpMethod.POST, "/api/calculator").permitAll();
                    // 인증 관련 경로 전체 허용
                    auth.requestMatchers("/auth/**", "/oauth2/**", "/login/**").permitAll();
                    // 관리자 전용 API — 로컬 개발 시 permit-admin=true로 인증 우회 가능
                    if (permitAdmin) {
                        auth.requestMatchers("/admin/**").permitAll();
                    } else {
                        auth.requestMatchers("/admin/**").hasRole("ADMIN");
                    }
                    // 그 외 모든 요청은 로그인 필요
                    auth.anyRequest().authenticated();
                })

                // OAuth2 소셜 로그인 설정 (현재 Google만 MVP 범위)
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo ->
                                userInfo.userService(customOAuth2UserService))
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
