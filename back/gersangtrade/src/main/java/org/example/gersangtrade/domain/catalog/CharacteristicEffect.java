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
 * 전설장수 특성 레벨 행의 개별 효과.
 * 같은 레벨 행에 여러 효과가 묶일 수 있다.
 * ENEMY 타깃은 value를 음수로 저장한다.
 */
@Entity
@Table(name = "characteristic_effect")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CharacteristicEffect {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "characteristic_id", nullable = false)
    private LegendGeneralCharacteristic characteristic;

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

    /** ENEMY 타깃은 음수로 저장 */
    @Column(nullable = false)
    private float value;

    @Builder
    public CharacteristicEffect(LegendGeneralCharacteristic characteristic,
                                 StatType statType, Element element,
                                 BuffValueType valueType, BuffTarget target, float value) {
        this.characteristic = characteristic;
        this.statType = statType;
        this.element = element;
        this.valueType = valueType;
        this.target = target;
        this.value = value;
    }
}
