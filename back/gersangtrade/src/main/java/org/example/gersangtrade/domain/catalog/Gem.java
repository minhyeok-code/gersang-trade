package org.example.gersangtrade.domain.catalog;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.gersangtrade.domain.catalog.enums.GemGrade;
import org.example.gersangtrade.domain.common.BaseEntity;

/**
 * 보석 엔티티.
 * 흑요석·적혈석 등 11종 보석이 4단계 등급을 가진다.
 * 등급 흐름: 기본 → 세공됨 → 강화됨 (빛나는은 별도 제작 루트)
 *
 * <p>주술은 독립 등급이 아니라 강화됨 등급에 부가되는 상태이다.
 * ritual_id가 non-null인 경우 = 강화됨 등급에 주술이 적용된 보석.
 * 빛나는 등급에는 주술 부착 불가 (DB CHECK 제약 동일 조건).
 *
 * <p>장비 아이템(EquipmentItem)과는 별개로 관리되며,
 * 거래 등록 시 BundleEquipmentDetail.gem을 통해 장비 라인에 연결된다.
 */
@Entity
@Table(
        name = "gems",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_gems_name_grade_ritual",
                columnNames = {"name", "gem_grade", "ritual_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Gem extends BaseEntity {

    /** 보석 고유 식별자 (자동 증가) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 기본 보석명.
     * 예: "흑요석", "적혈석". gemGrade와 ritual과 조합하여 전체 명칭이 결정된다.
     */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /** 보석 등급 — 기본 | 세공됨 | 강화됨 | 빛나는 */
    @Enumerated(EnumType.STRING)
    @Column(name = "gem_grade", nullable = false, length = 20)
    private GemGrade gemGrade;

    /**
     * 적용된 주술 — gemGrade가 강화됨일 때만 non-null 허용.
     * 예: "태산북두" 주술이 적용된 흑요석 → gemGrade=강화됨, ritual=태산북두 Ritual
     * DB CHECK 제약: ritual_id IS NULL OR gem_grade = '강화됨'
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ritual_id")
    private Ritual ritual;

    /**
     * gerniverse에서 수집한 이미지 S3 URL.
     * 크롤링 완료 전에는 null.
     */
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Builder
    public Gem(String name, GemGrade gemGrade, Ritual ritual, String imageUrl) {
        this.name = name;
        this.gemGrade = gemGrade;
        this.ritual = ritual;
        this.imageUrl = imageUrl;
    }

    /** 이미지 크롤링 완료 후 S3 URL 저장 */
    public void updateImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    /**
     * 강화됨 등급과 ritual 연결 정합성 검증.
     * DB 저장·수정 직전 자동 실행되어 잘못된 상태가 영속화되는 것을 방지한다.
     *
     * <ul>
     *   <li>강화됨 이외 등급 → ritual은 반드시 null</li>
     *   <li>강화됨 등급 → ritual은 null(주술 없음) 또는 non-null(주술 있음) 모두 허용</li>
     *   <li>빛나는 등급에는 주술 부착 불가</li>
     * </ul>
     */
    @PrePersist
    @PreUpdate
    private void validateRitual() {
        if (GemGrade.ENHANCED != this.gemGrade && this.ritual != null) {
            throw new IllegalStateException("강화됨 이외 등급 보석에는 ritual을 연결할 수 없습니다. name=" + this.name + ", grade=" + this.gemGrade);
        }
    }
}
