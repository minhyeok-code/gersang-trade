package org.example.gersangtrade.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 로컬 개발 환경 전용 Security 설정.
 * local 프로파일 + local.security.permit-admin=true 일 때만 활성화.
 * /admin/crawler/** 와 /local/** 에 ADMIN 인증을 자동 주입한다.
 * 운영 환경에서는 절대 활성화되지 않는다.
 */
@Configuration
@Profile("local")
@ConditionalOnProperty(name = "local.security.permit-admin", havingValue = "true")
@Order(1)
public class LocalSecurityConfig {

    @Bean
    public SecurityFilterChain localCrawlerFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/admin/crawler/**", "/local/**")
                .csrf(AbstractHttpConfigurer::disable)
                .addFilterBefore(new LocalAdminInjectFilter(), UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    /** 모든 요청에 ADMIN 인증을 자동 주입하는 로컬 전용 필터 */
    private static class LocalAdminInjectFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request,
                                        HttpServletResponse response,
                                        FilterChain filterChain) throws ServletException, IOException {
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    0L, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
            SecurityContextHolder.getContext().setAuthentication(auth);
            filterChain.doFilter(request, response);
        }
    }
}
