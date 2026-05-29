package org.example.gersangtrade.auth.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gersangtrade.auth.jwt.JwtTokenizer;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 요청마다 Authorization 헤더의 Bearer AT(액세스 토큰)를 검증하는 필터.
 *
 * 검증 성공 시: SecurityContext에 인증 정보를 저장하고 다음 필터로 진행
 * 검증 실패 시: SecurityContext를 초기화하고 다음 필터로 진행
 *              (실제 접근 거부는 Spring Security의 AuthorizationFilter에서 처리)
 *
 * DB 조회를 하지 않고 JWT 서명 + 클레임만으로 인증을 완료한다 (stateless).
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenizer jwtTokenizer;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String token = extractBearerToken(request);

        if (StringUtils.hasText(token)) {
            // 토큰 유효성 + 타입 검사 (RT가 AT 용도로 악용되는 케이스 차단)
            if (jwtTokenizer.validate(token) && jwtTokenizer.isAccessToken(token)) {
                Long userId = jwtTokenizer.getUserId(token);
                String role = jwtTokenizer.getRole(token);

                // Spring Security 인증 객체 생성 (DB 조회 없이 JWT 클레임만 사용)
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userId,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + role))
                        );
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("JWT 인증 성공: userId={}, role={}", userId, role);
            } else {
                log.debug("JWT 검증 실패 또는 잘못된 토큰 타입: path={}", request.getRequestURI());
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    /** Authorization 헤더 또는 ?token 쿼리 파라미터에서 토큰 추출 (EventSource는 헤더 설정 불가) */
    private String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        String queryToken = request.getParameter("token");
        if (StringUtils.hasText(queryToken)) {
            return queryToken;
        }
        return null;
    }
}
