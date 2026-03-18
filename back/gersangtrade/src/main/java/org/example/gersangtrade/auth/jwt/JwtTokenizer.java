package org.example.gersangtrade.auth.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * JWT 액세스 토큰 / 리프레시 토큰 생성 및 검증 컴포넌트.
 *
 * 클레임 구조:
 *   - sub  : 사용자 ID (Long → String)
 *   - role : 사용자 권한 ("USER" | "ADMIN")
 *   - type : 토큰 유형 ("ACCESS" | "REFRESH") — RT를 AT로 악용 방지
 */
@Slf4j
@Component
public class JwtTokenizer {

    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TYPE = "type";
    private static final String TYPE_ACCESS = "ACCESS";
    private static final String TYPE_REFRESH = "REFRESH";

    private final SecretKey signingKey;
    private final long accessTokenExpiryMs;
    private final long refreshTokenExpiryMs;

    public JwtTokenizer(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-expiry-ms}") long accessTokenExpiryMs,
            @Value("${app.jwt.refresh-token-expiry-ms}") long refreshTokenExpiryMs
    ) {
        // HMAC-SHA256은 최소 32바이트 키가 필요 — 설정 오류를 애플리케이션 시작 시점에 노출
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException(
                    "JWT 시크릿 키가 너무 짧습니다. 최소 32바이트 이상이어야 합니다. (현재: " + keyBytes.length + "바이트)"
            );
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenExpiryMs = accessTokenExpiryMs;
        this.refreshTokenExpiryMs = refreshTokenExpiryMs;
    }

    /**
     * 액세스 토큰 생성 (유효시간: 15분).
     *
     * @param userId 사용자 ID
     * @param role   사용자 권한 (예: "USER", "ADMIN")
     */
    public String createAccessToken(Long userId, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_ROLE, role)
                .claim(CLAIM_TYPE, TYPE_ACCESS)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(accessTokenExpiryMs)))
                .signWith(signingKey)
                .compact();
    }

    /**
     * 리프레시 토큰 생성 (유효시간: 7일).
     * role 클레임은 포함하지 않음 — RT는 새 AT 발급 용도만이므로 최소 정보만 담는다.
     *
     * @param userId 사용자 ID
     */
    public String createRefreshToken(Long userId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_TYPE, TYPE_REFRESH)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(refreshTokenExpiryMs)))
                .signWith(signingKey)
                .compact();
    }

    /**
     * 토큰을 파싱하여 클레임을 반환한다.
     * 서명 불일치, 만료, 형식 오류 시 JwtException을 던진다.
     */
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 토큰에서 사용자 ID를 추출한다.
     */
    public Long getUserId(String token) {
        return Long.parseLong(parseClaims(token).getSubject());
    }

    /**
     * 토큰에서 권한(role)을 추출한다.
     */
    public String getRole(String token) {
        return parseClaims(token).get(CLAIM_ROLE, String.class);
    }

    /**
     * 토큰이 액세스 토큰인지 검증한다.
     * RT를 AT 용도로 악용하는 경우를 방어한다.
     */
    public boolean isAccessToken(String token) {
        try {
            return TYPE_ACCESS.equals(parseClaims(token).get(CLAIM_TYPE, String.class));
        } catch (JwtException e) {
            return false;
        }
    }

    /**
     * 토큰 유효성을 검사하고, 유효하면 true를 반환한다.
     * (서명, 만료, 형식 오류 시 false)
     */
    public boolean validate(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException e) {
            log.debug("JWT 검증 실패: {}", e.getMessage());
            return false;
        }
    }

    public long getRefreshTokenExpiryMs() {
        return refreshTokenExpiryMs;
    }
}
