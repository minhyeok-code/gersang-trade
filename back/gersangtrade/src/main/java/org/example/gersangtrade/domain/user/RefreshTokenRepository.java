package org.example.gersangtrade.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** 리프레시 토큰 레포지토리 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /** 토큰 값으로 조회 (refresh 요청 시 검증에 사용) */
    Optional<RefreshToken> findByToken(String token);

    /** 사용자 ID로 조회 (로그인 시 기존 토큰 교체에 사용) */
    Optional<RefreshToken> findByUserId(Long userId);

    /** 사용자 ID에 해당하는 토큰 삭제 (로그아웃 시 사용) */
    void deleteByUserId(Long userId);
}
