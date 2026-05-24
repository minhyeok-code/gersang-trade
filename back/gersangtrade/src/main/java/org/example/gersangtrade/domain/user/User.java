package org.example.gersangtrade.domain.user;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.gersangtrade.domain.catalog.Server;
import org.example.gersangtrade.domain.common.BaseEntity;
import org.example.gersangtrade.domain.user.enums.GradeLevel;
import org.example.gersangtrade.domain.user.enums.Role;
import org.example.gersangtrade.domain.user.enums.UserStatus;

import java.time.LocalDateTime;

/**
 * 서비스 사용자 엔티티.
 * OAuth2 소셜 로그인 기반으로 가입되며, provider + oauthId 조합이 고유 식별자 역할을 한다.
 * 소프트 삭제(deletedAt) 방식을 사용하며, 배치 작업으로 1년 후 하드딜리트된다.
 *
 * 등급·EXP·매너점수 정책: docs/gersang-grade-policy.md 참고.
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
     * 현재 등급 — GradeLevel.HAENGSANG(행상)에서 시작.
     * totalExp를 기반으로 ExpGradeCalculator가 갱신한다.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "grade", nullable = false, length = 20)
    private GradeLevel grade;

    /**
     * 현재 등급 내 호봉 (1부터 시작).
     * 거상(GEOSANG)은 호봉이 없으므로 null.
     */
    @Column(name = "grade_step")
    private Integer gradeStep;

    /**
     * 누적 EXP — 거래 확정 및 평가 결과로 증감된다.
     * 이 값이 grade·gradeStep 계산의 원천(Source of Truth)이다.
     */
    @Column(name = "total_exp", nullable = false)
    private Long totalExp;

    /**
     * 매너점수 — 신규 가입 시 60점으로 시작, 범위 0~100.
     * 거래 평가(TradeReview)가 공개될 때 배치 Job이 반영한다.
     */
    @Column(name = "manner_score", nullable = false)
    private Integer mannerScore;

    /**
     * 거래 확정 횟수 — TradeConfirmed 생성 시마다 +1.
     * 프로필 공개 정보로 상대방 신뢰도 확인에 사용된다.
     */
    @Column(name = "trade_count", nullable = false)
    private Integer tradeCount;

    /**
     * 사용자가 선택한 기본 서버.
     * null: 미설정 (전체 서버 조회). 비로그인 유저는 세션·쿼리 파라미터로 처리.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "server_id")
    private Server server;

    /**
     * 거상 게임 닉네임 — 유저가 직접 입력하는 인게임 캐릭터명.
     * null: 미입력.
     */
    @Column(name = "game_nickname", length = 50)
    private String gameNickname;

    /**
     * 게임 접속 가능 시간대 — 유저가 직접 입력하는 자유 텍스트.
     * 거래 상대방이 언제 연락 가능한지 판단하는 데 사용된다.
     * 예: "평일 저녁 7~10시", "주말 오후"
     * null: 미입력.
     */
    @Column(name = "game_access_time", length = 100)
    private String gameAccessTime;

    /**
     * 프로필 이미지 URL.
     * OAuth2 최초 가입 시 제공자 프로필 사진으로 초기화된다.
     * 유저가 직접 수정 가능. null: 미설정.
     */
    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    /**
     * 소프트 삭제 시각.
     * null: 활성 계정, non-null: 탈퇴 처리된 계정 (1년 후 배치 하드딜리트 대상).
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    public User(String oauthProvider, String oauthId, String nickname,
                String email, Role role, UserStatus status, String profileImageUrl) {
        this.oauthProvider = oauthProvider;
        this.oauthId = oauthId;
        this.nickname = nickname;
        this.email = email;
        this.role = role;
        this.profileImageUrl = profileImageUrl;
        // 상태 미지정 시 기본값 ACTIVE 적용
        this.status = (status != null) ? status : UserStatus.ACTIVE;
        // 등급·EXP·매너점수 초기값
        this.grade = GradeLevel.HAENGSANG;
        this.gradeStep = 1;
        this.totalExp = 0L;
        this.mannerScore = 60;
        this.tradeCount = 0;
    }

    /** 닉네임 변경 */
    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    /** 게임 닉네임 변경 — null 전달 시 미입력 상태로 초기화 */
    public void updateGameNickname(String gameNickname) {
        this.gameNickname = gameNickname;
    }

    /** 게임 접속 가능 시간대 변경 — null 전달 시 미입력 상태로 초기화 */
    public void updateGameAccessTime(String gameAccessTime) {
        this.gameAccessTime = gameAccessTime;
    }

    /** 프로필 이미지 URL 변경 — null 전달 시 미설정 상태로 초기화 */
    public void updateProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    /** 기본 서버 설정 — null 전달 시 선택 해제 */
    public void updateServer(Server server) {
        this.server = server;
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

    /**
     * EXP 추가 및 등급·호봉 재계산.
     * ExpGradeCalculator가 반환한 값을 받아 User에 반영한다.
     *
     * @param expDelta    추가할 EXP (음수 가능)
     * @param newGrade    재계산된 등급
     * @param newStep     재계산된 호봉 (거상은 null)
     */
    public void applyExp(long expDelta, GradeLevel newGrade, Integer newStep) {
        this.totalExp = Math.max(0, this.totalExp + expDelta);
        this.grade = newGrade;
        this.gradeStep = newStep;
    }

    /**
     * 거래 확정 횟수 증가.
     * TradeConfirmed 생성 트랜잭션에서 호출된다.
     */
    public void incrementTradeCount() {
        this.tradeCount++;
    }

    /**
     * 매너점수 변경 — 0~100 범위 내로 클램핑.
     * TradeReview 공개(배치 Job) 시 호출된다.
     *
     * @param delta 변화량 (+2 / 0 / -3)
     */
    public void applyMannerScore(int delta) {
        this.mannerScore = Math.max(0, Math.min(100, this.mannerScore + delta));
    }
}
