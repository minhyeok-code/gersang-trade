package org.example.gersangtrade.domain.guide;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.gersangtrade.domain.catalog.Mercenary;
import org.example.gersangtrade.domain.common.BaseEntity;
import org.example.gersangtrade.domain.guide.enums.GuidePhase;

/**
 * 가이드 원본 엔티티 (관리자 큐레이션).
 * 특정 사천왕(용병) 육성 로드맵의 원본이며, 유저는 읽기 전용이다.
 * 유저가 "시작하기"를 누르면 이 원본을 복제한 {@link UserGuide} 사본이 생성된다.
 *
 * <p>일반(NORMAL) → 각성(AWAKENED)은 {@code nextGuide}로 연결된다.
 */
@Entity
@Table(name = "guides")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Guide extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 가이드 제목 — 예: "일반 지국천왕 가이드" */
    @Column(name = "title", nullable = false, length = 100)
    private String title;

    /**
     * 대상 사천왕(용병) — 개인화·덱 매칭 키.
     * 유저 덱의 주력 용병과 대조해 홈에 노출할 가이드를 고른다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_mercenary_id")
    private Mercenary targetMercenary;

    /** 단계 — NORMAL(일반) | AWAKENED(각성) */
    @Enumerated(EnumType.STRING)
    @Column(name = "phase", nullable = false, length = 20)
    private GuidePhase phase;

    /** 가이드 버전 — 예: "2025-12". 사본이 뜰 때 스냅샷으로 저장된다 */
    @Column(name = "version", nullable = false, length = 30)
    private String version;

    /** 제작자 표기 — 예: "(봉황) [한라]구급차" */
    @Column(name = "author", length = 100)
    private String author;

    /** 공개 노출 여부 — false이면 유저에게 보이지 않는다(작성 중) */
    @Column(name = "published", nullable = false)
    private boolean published = false;

    /** 다음 단계 가이드 — 일반 → 각성 연결 (nullable) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "next_guide_id")
    private Guide nextGuide;

    @Builder
    public Guide(String title, Mercenary targetMercenary, GuidePhase phase,
                 String version, String author, Guide nextGuide) {
        this.title = title;
        this.targetMercenary = targetMercenary;
        this.phase = phase;
        this.version = version;
        this.author = author;
        this.nextGuide = nextGuide;
    }

    /** 관리자 수정 — 기본 정보 */
    public void updateInfo(String title, GuidePhase phase, String version, String author) {
        if (title != null && !title.isBlank()) this.title = title;
        if (phase != null) this.phase = phase;
        if (version != null && !version.isBlank()) this.version = version;
        this.author = author;
    }

    /** 대상 사천왕 변경 */
    public void updateTargetMercenary(Mercenary mercenary) {
        this.targetMercenary = mercenary;
    }

    /** 다음 단계 가이드 연결 */
    public void linkNextGuide(Guide nextGuide) {
        this.nextGuide = nextGuide;
    }

    /** 노출 여부 설정 */
    public void updatePublished(boolean published) {
        this.published = published;
    }
}
