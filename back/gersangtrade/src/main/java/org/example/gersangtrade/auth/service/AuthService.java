package org.example.gersangtrade.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gersangtrade.auth.jwt.JwtTokenizer;
import org.example.gersangtrade.domain.user.RefreshToken;
import org.example.gersangtrade.domain.user.RefreshTokenRepository;
import org.example.gersangtrade.domain.user.User;
import org.example.gersangtrade.domain.user.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 인증 관련 비즈니스 로직 서비스.
 *
 * refresh(): RTR(Refresh Token Rotation) 패턴으로 AT와 RT를 동시에 재발급한다.
 *   - RT 검증 → 기존 RT 삭제 → 새 RT 저장 → 새 AT + 새 RT 반환
 *   - 이미 사용된(DB에 없는) RT로 refresh를 시도하면 탈취 시도로 간주하여 거부한다.
 *
 * logout(): DB에서 RT를 삭제하여 토큰을 무효화한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtTokenizer jwtTokenizer;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    /**
     * 로그인 성공 시 RT 발급 및 DB 저장.
     * OAuth2LoginSuccessHandler에서 @Transactional 보장을 위해 이 메서드에 위임한다.
     *
     * @param userId 사용자 ID
     * @return 새로 발급된 리프레시 토큰 문자열
     */
    @Transactional
    public String issueRefreshToken(Long userId) {
        String refreshToken = jwtTokenizer.createRefreshToken(userId);
        LocalDateTime expiresAt = LocalDateTime.now()
                .plusSeconds(jwtTokenizer.getRefreshTokenExpiryMs() / 1000);

        // 기존 RT가 있으면 교체(RTR), 없으면 신규 저장
        refreshTokenRepository.findByUserId(userId)
                .ifPresentOrElse(
                        existing -> existing.rotate(refreshToken, expiresAt),
                        () -> refreshTokenRepository.save(
                                RefreshToken.builder()
                                        .userId(userId)
                                        .token(refreshToken)
                                        .expiresAt(expiresAt)
                                        .build()
                        )
                );
        return refreshToken;
    }

    /**
     * RTR 패턴 토큰 재발급.
     * 기존 RT를 무효화하고 새 AT + RT를 발급한다.
     *
     * @param oldRefreshToken 쿠키에서 읽어온 기존 RT
     * @return 새 AT (새 RT는 호출 측에서 Cookie로 설정)
     */
    @Transactional
    public RefreshResult refresh(String oldRefreshToken) {
        // 1. JWT 서명/만료 검증
        if (!jwtTokenizer.validate(oldRefreshToken)) {
            throw new BadCredentialsException("유효하지 않은 리프레시 토큰입니다.");
        }

        // 2. DB에서 RT 조회 — 없으면 이미 사용된 토큰이거나 탈취 후 재사용 시도
        RefreshToken storedToken = refreshTokenRepository.findByToken(oldRefreshToken)
                .orElseThrow(() -> {
                    // 유효한 JWT이지만 DB에 없음 → 탈취된 토큰으로 재발급 시도 가능성
                    // JWT에서 userId를 추출하여 해당 사용자의 모든 RT를 무효화한다
                    try {
                        Long suspectUserId = jwtTokenizer.getUserId(oldRefreshToken);
                        refreshTokenRepository.deleteByUserId(suspectUserId);
                        log.warn("탈취 의심 RT 감지: userId={}의 모든 RT 무효화 처리", suspectUserId);
                    } catch (Exception e) {
                        log.warn("탈취 의심 RT 감지: userId 추출 실패 (이미 만료된 토큰일 수 있음)");
                    }
                    return new BadCredentialsException("이미 사용되었거나 만료된 리프레시 토큰입니다. 다시 로그인해 주세요.");
                });

        // 3. DB 만료 시간 재검사 (JWT exp와 별도로 이중 검사)
        if (storedToken.isExpired()) {
            refreshTokenRepository.delete(storedToken);
            throw new BadCredentialsException("만료된 리프레시 토큰입니다. 다시 로그인해 주세요.");
        }

        // 4. 사용자 조회
        Long userId = storedToken.getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadCredentialsException("존재하지 않는 사용자입니다."));

        // 5. 새 AT + RT 발급
        String newAccessToken = jwtTokenizer.createAccessToken(userId, user.getRole().name());
        String newRefreshToken = jwtTokenizer.createRefreshToken(userId);

        // 6. DB에서 기존 RT를 새 RT로 교체 (RTR)
        LocalDateTime newExpiresAt = LocalDateTime.now()
                .plusSeconds(jwtTokenizer.getRefreshTokenExpiryMs() / 1000);
        storedToken.rotate(newRefreshToken, newExpiresAt);

        log.debug("토큰 재발급 완료 (RTR): userId={}", userId);
        return new RefreshResult(newAccessToken, newRefreshToken);
    }

    /**
     * 로그아웃: DB에서 RT를 삭제하여 해당 토큰을 영구 무효화한다.
     * AT는 서버에서 무효화 불가 (만료 대기). 클라이언트에서 즉시 폐기해야 한다.
     *
     * @param refreshToken 쿠키에서 읽어온 RT
     */
    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepository.findByToken(refreshToken)
                .ifPresent(token -> {
                    refreshTokenRepository.delete(token);
                    log.debug("로그아웃: userId={}의 RT 삭제 완료", token.getUserId());
                });
    }

    /** refresh 결과를 담는 내부 레코드 (새 AT + 새 RT) */
    public record RefreshResult(String accessToken, String refreshToken) {}
}
