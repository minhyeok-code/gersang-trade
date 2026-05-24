package org.example.gersangtrade.auth.security;

import org.example.gersangtrade.domain.user.User;

/** OAuth2/OIDC 로그인 성공 후 SecurityContext에 저장되는 principal 공통 인터페이스. */
public interface CustomUserPrincipal {
    User getUser();
}
