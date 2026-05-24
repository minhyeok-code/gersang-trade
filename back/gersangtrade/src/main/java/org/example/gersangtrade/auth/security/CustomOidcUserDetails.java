package org.example.gersangtrade.auth.security;

import org.example.gersangtrade.domain.user.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Google OIDC 로그인 성공 후 SecurityContext에 저장되는 principal.
 * OidcUser를 위임하면서 DB User 엔티티를 함께 보관한다.
 */
public class CustomOidcUserDetails implements OidcUser, CustomUserPrincipal {

    private final User user;
    private final OidcUser delegate;

    public CustomOidcUserDetails(User user, OidcUser delegate) {
        this.user = user;
        this.delegate = delegate;
    }

    @Override
    public User getUser() {
        return user;
    }

    @Override
    public Map<String, Object> getClaims() {
        return delegate.getClaims();
    }

    @Override
    public OidcUserInfo getUserInfo() {
        return delegate.getUserInfo();
    }

    @Override
    public OidcIdToken getIdToken() {
        return delegate.getIdToken();
    }

    @Override
    public Map<String, Object> getAttributes() {
        return delegate.getAttributes();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override
    public String getName() {
        return String.valueOf(user.getId());
    }
}
