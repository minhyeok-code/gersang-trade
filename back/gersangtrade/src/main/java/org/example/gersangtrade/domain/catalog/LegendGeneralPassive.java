package org.example.gersangtrade.domain.catalog;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.gersangtrade.domain.catalog.enums.BuffTarget;
import org.example.gersangtrade.domain.catalog.enums.BuffValueType;
import org.example.gersangtrade.domain.catalog.enums.Element;
import org.example.gersangtrade.domain.catalog.enums.StatType;

/**
 * 전설장수 타입 A 전용 레벨 기반 패시브.
 * 계산식: value = startValue + floor((level - startLevel) / incrementPerLevels) * incrementValue
 * startValue/incrementPerLevels/incrementValue가 null이면 계산 스킵 (미확정 데이터).
 */
@Entity
@Table(name = "legend_general_passive")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LegendGeneralPassive {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "legend_general_id", nullable = false)
    private LegendGeneral legendGeneral;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatType statType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Element element;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private BuffValueType valueType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private BuffTarget target;

    @Column
    private Integer startLevel;

    /** 미확정이면 null — 계산기에서 null 감지 시 스킵 */
    @Column
    private Float startValue;

    @Column
    private Integer incrementPerLevels;

    @Column
    private Float incrementValue;

    @Column
    private Float maxValue;

    @Builder
    public LegendGeneralPassive(LegendGeneral legendGeneral, StatType statType,
                                 Element element, BuffValueType valueType, BuffTarget target,
                                 Integer startLevel, Float startValue,
                                 Integer incrementPerLevels, Float incrementValue, Float maxValue) {
        this.legendGeneral = legendGeneral;
        this.statType = statType;
        this.element = element;
        this.valueType = valueType;
        this.target = target;
        this.startLevel = startLevel;
        this.startValue = startValue;
        this.incrementPerLevels = incrementPerLevels;
        this.incrementValue = incrementValue;
        this.maxValue = maxValue;
    }
}
