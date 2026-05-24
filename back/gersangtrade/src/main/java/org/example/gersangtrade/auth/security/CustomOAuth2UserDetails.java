package org.example.gersangtrade.auth.security;

import lombok.Getter;
import org.example.gersangtrade.domain.user.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * OAuth2 로그인 성공 후 Spring Security가 사용하는 인증 객체.
 * User 엔티티와 OAuth2 제공자의 원본 attributes를 함께 보관한다.
 */
@Getter
public class CustomOAuth2UserDetails implements OAuth2User, CustomUserPrincipal {

    /** DB에 저장된 사용자 엔티티 */
    private final User user;

    /** OAuth2 제공자(Google 등)에서 받아온 원본 속성 */
    private final Map<String, Object> attributes;

    public CustomOAuth2UserDetails(User user, Map<String, Object> attributes) {
        this.user = user;
        this.attributes = attributes;
    }

    /** Spring Security 권한 목록 — "ROLE_USER" 또는 "ROLE_ADMIN" */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    /** OAuth2User.getName() — userId를 문자열로 반환 */
    @Override
    public String getName() {
        return String.valueOf(user.getId());
    }
}
