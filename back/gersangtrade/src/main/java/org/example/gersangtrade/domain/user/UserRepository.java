package org.example.gersangtrade.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** 사용자 레포지토리 */
public interface UserRepository extends JpaRepository<User, Long> {

    /** OAuth2 provider + oauthId 조합으로 사용자 조회 (로그인/가입 시 사용) */
    Optional<User> findByOauthProviderAndOauthId(String oauthProvider, String oauthId);
}
