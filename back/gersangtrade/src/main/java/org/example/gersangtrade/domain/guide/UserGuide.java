package org.example.gersangtrade.domain.guide;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.gersangtrade.domain.catalog.Mercenary;
import org.example.gersangtrade.domain.common.BaseEntity;
import org.example.gersangtrade.domain.user.User;

import java.time.LocalDateTime;

/**
 * 유저 가이드 사본 엔티티 (비공개).
 * 유저가 원본 {@link Guide}를 "시작하기" 하면 스텝까지 복제되어 생성된다.
 * 오직 본인만 조회·수정할 수 있으며 공유되지 않는다.
 *
 * <p>사본은 뜰 당시 원본의 스냅샷이다 — 원본이 나중에 갱신돼도 자동 반영되지 않는다.
 * ({@code sourceVersion}에 기준 버전을 보관한다.)
 *
 * <p>같은 원본당 활성 사본은 1개 — 유일성은 애플리케이션에서 강제한다.
 * (소프트삭제된 사본이 남을 수 있어 DB 유니크 제약 대신 앱 레벨에서 확인한다.)
 */
@Entity
@Table(name = "user_guides")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserGuide extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 소유 유저 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 원본 가이드 — 어느 가이드에서 떴는지 추적용.
     * 원본이 삭제돼도 사본은 유지돼야 하므로 nullable.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_guide_id")
    private Guide sourceGuide;

    /** 사본을 뜰 당시의 원본 버전 스냅샷 — 예: "2025-12" */
    @Column(name = "source_version", nullable = false, length = 30)
    private String sourceVersion;

    /** 사본 제목 — 유저가 변경 가능 (기본값은 원본 제목) */
    @Column(name = "title", nullable = false, length = 100)
    private String title;

    /** 대상 사천왕 스냅샷 — 표시·덱 매칭용 (nullable) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_mercenary_id")
    private Mercenary targetMercenary;

    /** 소프트삭제 시각 — null이면 활성 */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    public UserGuide(User user, Guide sourceGuide, String sourceVersion,
                     String title, Mercenary targetMercenary) {
        this.user = user;
        this.sourceGuide = sourceGuide;
        this.sourceVersion = sourceVersion;
        this.title = title;
        this.targetMercenary = targetMercenary;
    }

    /** 유저가 사본 제목 변경 */
    public void updateTitle(String title) {
        if (title != null && !title.isBlank()) this.title = title;
    }

    /** 소프트삭제 */
    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }
}
