package org.example.gersangtrade.domain.user;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.gersangtrade.domain.common.BaseEntity;
import org.example.gersangtrade.domain.user.enums.Role;
import org.example.gersangtrade.domain.user.enums.UserStatus;

import java.time.LocalDateTime;

/**
 * 서비스 사용자 엔티티.
 * OAuth2 소셜 로그인 기반으로 가입되며, provider + oauthId 조합이 고유 식별자 역할을 한다.
 * 소프트 삭제(deletedAt) 방식을 사용하며, 배치 작업으로 1년 후 하드딜리트된다.
 */
@Entity
@Table(
        name = "users",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_users_oauth_provider_id",
                columnNames = {"oauth_provider", "oauth_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    /** 사용자 고유 식별자 (자동 증가) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** OAuth2 제공자명 — 현재 MVP는 "google", 추후 "kakao" 등 확장 예정 */
    @Column(name = "oauth_provider", nullable = false, length = 20)
    private String oauthProvider;

    /** 소셜 로그인 제공자가 발급한 고유 사용자 ID */
    @Column(name = "oauth_id", nullable = false)
    private String oauthId;

    /** 서비스 내 표시 닉네임 */
    @Column(name = "nickname", nullable = false, length = 50)
    private String nickname;

    /** 소셜 로그인 이메일 주소 */
    @Column(name = "email", nullable = false, length = 100)
    private String email;

    /** 사용자 권한 역할 (USER / ADMIN) */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    /** 계정 활성 상태 — ACTIVE: 정상, BLOCKED: 차단됨 */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status;

    /**
     * 차단 만료 일시.
     * null: 영구 차단이거나 차단 상태가 아님.
     * 미래 날짜: 기간 차단 만료일.
     */
    @Column(name = "blocked_until")
    private LocalDateTime blockedUntil;

    /** 관리자가 기록한 차단 사유 */
    @Column(name = "block_reason", length = 500)
    private String blockReason;

    /**
     * 소프트 삭제 시각.
     * null: 활성 계정, non-null: 탈퇴 처리된 계정 (1년 후 배치 하드딜리트 대상).
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    public User(String oauthProvider, String oauthId, String nickname,
                String email, Role role, UserStatus status) {
        this.oauthProvider = oauthProvider;
        this.oauthId = oauthId;
        this.nickname = nickname;
        this.email = email;
        this.role = role;
        // 상태 미지정 시 기본값 ACTIVE 적용
        this.status = (status != null) ? status : UserStatus.ACTIVE;
    }

    /** 닉네임 변경 */
    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    /** 차단 처리 — blockedUntil이 null이면 영구 차단 */
    public void block(String reason, LocalDateTime blockedUntil) {
        this.status = UserStatus.BLOCKED;
        this.blockReason = reason;
        this.blockedUntil = blockedUntil;
    }

    /** 차단 해제 */
    public void unblock() {
        this.status = UserStatus.ACTIVE;
        this.blockReason = null;
        this.blockedUntil = null;
    }

    /** 소프트 삭제 처리 */
    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }
}
