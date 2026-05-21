package org.example.gersangtrade.domain.catalog;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.gersangtrade.domain.catalog.enums.Element;
import org.example.gersangtrade.domain.catalog.enums.BuffTarget;
import org.example.gersangtrade.domain.catalog.enums.StatType;
import org.example.gersangtrade.domain.catalog.enums.StatUnit;

/**
 * 장비 세트 착용 수별 추가 능력치 엔티티.
 * 착용한 피스 수(requiredPieces)에 따라 발동하는 세트 보너스를 저장한다.
 * element=NONE: 모든 속성 공통 증가. non-NONE: 특정 속성 증가.
 */
@Entity
@Table(
        name = "equipment_set_effects",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_set_effects_set_pieces_stat_element_scope",
                columnNames = {"equipment_set_id", "required_pieces", "stat_type", "element", "scope"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EquipmentSetEffect {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_set_id", nullable = false)
    private EquipmentSet equipmentSet;

    /** 몇 종 착용 시 발동하는 효과인지 (2, 3, 4, 5, 6) */
    @Column(name = "required_pieces", nullable = false)
    private Integer requiredPieces;

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
     * NONE: 모든 속성 공통 증가. non-NONE: 특정 속성 증가(예: EARTH=땅속성).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "element", nullable = false, length = 20)
    private Element element = Element.NONE;

    /**
     * 스탯 적용 범위.
     * SELF : 장착 용병 본인 (기본값, 대부분의 세트 효과).
     * PARTY: 아군 덱 전체 용병.
     * ENEMY: 적 전체 디버프.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false, length = 10)
    private BuffTarget scope = BuffTarget.SELF;

    @Builder
    public EquipmentSetEffect(EquipmentSet equipmentSet, Integer requiredPieces,
                               StatType statType, Integer statValue, StatUnit statUnit,
                               Element element, BuffTarget scope) {
        this.equipmentSet = equipmentSet;
        this.requiredPieces = requiredPieces;
        this.statType = statType;
        this.statValue = statValue;
        this.statUnit = statUnit;
        this.element = (element != null) ? element : Element.NONE;
        this.scope = (scope != null) ? scope : BuffTarget.SELF;
    }
}
