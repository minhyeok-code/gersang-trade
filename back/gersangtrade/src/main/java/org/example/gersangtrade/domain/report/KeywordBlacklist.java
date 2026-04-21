package org.example.gersangtrade.domain.report;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.gersangtrade.domain.user.User;

import java.time.LocalDateTime;

/**
 * 현금거래 의심 키워드·패턴 블랙리스트.
 * 관리자가 코드 배포 없이 키워드를 추가·수정·비활성화할 수 있다.
 * KeywordDetectionService가 isActive=true인 패턴을 캐싱하여 메시지 전송 시 검사한다.
 *
 * 감지 방식: Soft — 메시지 전송은 허용하고 자동 신고(Report) 생성 + 관리자 알림.
 * 상세: docs/report-system.ko.md 2-3절 참고.
 */
@Entity
@Table(name = "keyword_blacklists",
        indexes = @Index(name = "idx_keyword_blacklists_active", columnList = "is_active")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class KeywordBlacklist {

    /** 고유 식별자 (자동 증가) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 검사할 키워드 또는 정규식 문자열.
     * isRegex=false: 단순 포함 검사 (content.contains(pattern))
     * isRegex=true: 정규식 매칭 (Pattern.compile(pattern).matcher(content).find())
     */
    @Column(name = "pattern", nullable = false, length = 200)
    private String pattern;

    /**
     * 정규식 여부.
     * false: 단순 문자열 포함 검사.
     * true: Java 정규식으로 매칭.
     */
    @Column(name = "is_regex", nullable = false)
    private boolean regex;

    /** 이 패턴을 추가한 이유·설명 (관리자 참고용) */
    @Column(name = "description", length = 200)
    private String description;

    /**
     * 활성 여부.
     * false이면 KeywordDetectionService 캐시에 포함되지 않아 검사에서 제외된다.
     */
    @Column(name = "is_active", nullable = false)
    private boolean active;

    /** 패턴을 등록한 관리자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    /** 등록 시각 */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public KeywordBlacklist(String pattern, boolean regex, String description, User createdBy) {
        this.pattern = pattern;
        this.regex = regex;
        this.description = description;
        this.createdBy = createdBy;
        this.active = true;
        this.createdAt = LocalDateTime.now();
    }

    /** 패턴 비활성화 — 캐시 갱신 후 검사에서 제외된다 */
    public void deactivate() {
        this.active = false;
    }

    /** 패턴 재활성화 */
    public void activate() {
        this.active = true;
    }

    /** 패턴 내용 수정 */
    public void updatePattern(String pattern, boolean regex, String description) {
        this.pattern = pattern;
        this.regex = regex;
        this.description = description;
    }
}
