package org.example.gersangtrade.domain.catalog;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.gersangtrade.domain.catalog.enums.Element;
import org.example.gersangtrade.domain.catalog.enums.StatType;
import org.example.gersangtrade.domain.catalog.enums.StatUnit;
import org.example.gersangtrade.domain.listing.enums.RitualOutcome;

/**
 * 주술 세트 효과 엔티티.
 * 동일 주술을 여러 피스에 적용했을 때 발동하는 추가 능력치를 저장한다.
 * outcome으로 SUCCESS(성공)과 GREAT_SUCCESS(대성공)의 효과를 구분한다.
 * element=NONE: 모든 속성 공통 증가. non-NONE: 특정 속성 증가.
 */
@Entity
@Table(
        name = "ritual_set_effects",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_ritual_set_effects_ritual_outcome_set_pieces_stat",
                columnNames = {"ritual_id", "outcome", "equipment_set_id", "required_ritual_pieces", "stat_type"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RitualSetEffect {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ritual_id", nullable = false)
    private Ritual ritual;

    /**
     * 주술 결과 구분 — SUCCESS / GREAT_SUCCESS.
     * 천추 SUCCESS와 천추 GREAT_SUCCESS(북두칠성)는 같은 ritual_id이지만
     * 세트 효과가 다르므로 outcome으로 구분한다.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false, length = 20)
    private RitualOutcome outcome;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_set_id", nullable = false)
    private EquipmentSet equipmentSet;

    /** 주술을 몇 피스에 적용해야 발동하는지 (3 또는 5) */
    @Column(name = "required_ritual_pieces", nullable = false)
    private Integer requiredRitualPieces;

    @Enumerated(EnumType.STRING)
    @Column(name = "stat_type", nullable = false, length = 30)
    private StatType statType;

    @Column(name = "stat_value", nullable = false)
    private Integer statValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "stat_unit", nullable = false, length = 10)
    private StatUnit statUnit;

    /**
     * 속성 구분 — ELEMENT_VALUE인 경우만 사용.
     * NONE: 모든 속성 공통 증가. non-NONE: 특정 속성 증가(예: FIRE=화속성).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "element", nullable = false, length = 20)
    private Element element = Element.NONE;

    @Builder
    public RitualSetEffect(Ritual ritual, RitualOutcome outcome, EquipmentSet equipmentSet,
                            Integer requiredRitualPieces, StatType statType, Integer statValue,
                            StatUnit statUnit, Element element) {
        this.ritual = ritual;
        this.outcome = outcome;
        this.equipmentSet = equipmentSet;
        this.requiredRitualPieces = requiredRitualPieces;
        this.statType = statType;
        this.statValue = statValue;
        this.statUnit = statUnit;
        this.element = (element != null) ? element : Element.NONE;
    }
}
