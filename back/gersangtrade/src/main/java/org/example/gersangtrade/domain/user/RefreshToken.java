package org.example.gersangtrade.domain.user;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 리프레시 토큰 엔티티.
 *
 * 현재는 사용자당 토큰 1개 (단일 기기). 다중 기기 지원 시 deviceInfo 컬럼을 추가하여
 * userId + deviceInfo 복합 유니크 제약으로 확장한다.
 *
 * 보안 주의: 현재 평문 저장. 운영 전환 시 SHA-256 해싱 후 저장 권장.
 * (해싱 시 refresh 요청 토큰을 해싱하여 DB와 비교)
 */
@Entity
@Table(
        name = "refresh_tokens",
        indexes = @Index(name = "idx_refresh_tokens_user_id", columnList = "user_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 토큰 소유자 사용자 ID */
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    /**
     * 리프레시 토큰 값.
     * 운영 환경에서는 SHA-256 해시 값을 저장하고, 검증 시 동일하게 해싱하여 비교한다.
     */
    @Column(name = "token", nullable = false, length = 512)
    private String token;

    /** 토큰 만료 일시 */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /** 발급 일시 */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public RefreshToken(Long userId, String token, LocalDateTime expiresAt) {
        this.userId = userId;
        this.token = token;
        this.expiresAt = expiresAt;
        this.createdAt = LocalDateTime.now();
    }

    /** 토큰 교체 (RTR 패턴: 기존 객체 재사용 시 업데이트용) */
    public void rotate(String newToken, LocalDateTime newExpiresAt) {
        this.token = newToken;
        this.expiresAt = newExpiresAt;
    }

    /** 토큰이 만료되었는지 확인 */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }
}
