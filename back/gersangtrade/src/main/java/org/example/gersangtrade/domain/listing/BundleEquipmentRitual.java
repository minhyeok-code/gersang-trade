package org.example.gersangtrade.domain.listing;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.gersangtrade.domain.catalog.Ritual;
import org.example.gersangtrade.domain.listing.enums.RitualOutcome;

/**
 * 번들 라인의 주술 결과 정보 엔티티.
 * 장비에 적용된 주술의 결과(성공/대성공)와 최종 표기 마크 스냅샷을 보관한다.
 * 동일 번들 라인에 동일 주술이 중복 적용되는 것을 방지하기 위해 UNIQUE 제약이 적용된다.
 * 주술 표기 예: "풀 XX 00세트" (세트 전체 동일 주술), "풀 00세트 3XX" (부분 주술)
 */
@Entity
@Table(
        name = "bundle_equipment_rituals",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_bundle_equipment_rituals_line_ritual",
                columnNames = {"bundle_line_id", "ritual_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BundleEquipmentRitual {

    /** 주술 결과 고유 식별자 (자동 증가) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 이 주술 정보가 속하는 번들 라인 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bundle_line_id", nullable = false)
    private BundleLine bundleLine;

    /** 적용된 주술 종류 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ritual_id", nullable = false)
    private Ritual ritual;

    /** 주술 결과 등급 — SUCCESS: 성공, GREAT_SUCCESS: 대성공 */
    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false, length = 20)
    private RitualOutcome outcome;

    /**
     * 등록 시점의 최종 주술 표기 스냅샷.
     * 예: "00"(성공), "**"(대성공).
     * 카탈로그의 Ritual.successMark / greatSuccessMark 변경과 무관하게 등록 당시 표기를 보존한다.
     */
    @Column(name = "applied_mark_snapshot", nullable = false, length = 20)
    private String appliedMarkSnapshot;

    @Builder
    public BundleEquipmentRitual(BundleLine bundleLine, Ritual ritual,
                                  RitualOutcome outcome, String appliedMarkSnapshot) {
        this.bundleLine = bundleLine;
        this.ritual = ritual;
        this.outcome = outcome;
        this.appliedMarkSnapshot = appliedMarkSnapshot;
    }
}
