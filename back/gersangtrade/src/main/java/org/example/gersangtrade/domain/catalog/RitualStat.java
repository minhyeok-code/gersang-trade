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
 * 주술 성공/대성공 시 장비에 부여되는 능력치 엔티티.
 * ItemStat은 아이템 자체 스탯 전용이므로 주술 스탯은 별도 테이블로 관리한다.
 * outcome으로 일반 성공(SUCCESS)과 북두칠성 대성공(GREAT_SUCCESS)을 구분한다.
 */
@Entity
@Table(
        name = "ritual_stats",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_ritual_stats_ritual_outcome_stat_element",
                columnNames = {"ritual_id", "outcome", "stat_type", "element"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RitualStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ritual_id", nullable = false)
    private Ritual ritual;

    /**
     * 주술 결과 구분.
     * SUCCESS: 일반 주술 성공 스탯.
     * GREAT_SUCCESS: 북두칠성 대성공 스탯.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false, length = 20)
    private RitualOutcome outcome;

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
     * 속성 구분이 없는 스탯은 NONE.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "element", nullable = false, length = 20)
    private Element element;

    @Builder
    public RitualStat(Ritual ritual, RitualOutcome outcome, StatType statType,
                      Integer statValue, StatUnit statUnit, Element element) {
        this.ritual = ritual;
        this.outcome = outcome;
        this.statType = statType;
        this.statValue = statValue;
        this.statUnit = statUnit;
        this.element = (element != null) ? element : Element.NONE;
    }
}
