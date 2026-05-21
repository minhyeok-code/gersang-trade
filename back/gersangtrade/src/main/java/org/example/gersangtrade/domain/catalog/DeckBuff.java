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
 * 덱 버프 항목 — DeckBuffSource에 속한 개별 버프 수치.
 * ENEMY 타깃은 value를 음수로 저장한다.
 */
@Entity
@Table(name = "deck_buff")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeckBuff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id", nullable = false)
    private DeckBuffSource source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatType statType;

    /**
     * 속성 구분.
     * ADAPTIVE: 착용 용병 속성 추종. NONE: 속성 무관.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Element element;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private BuffValueType valueType;

    /** ENEMY 타깃은 음수로 저장 */
    @Column(nullable = false)
    private float value;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private BuffTarget target;

    @Builder
    public DeckBuff(DeckBuffSource source, StatType statType, Element element,
                    BuffValueType valueType, float value, BuffTarget target) {
        this.source = source;
        this.statType = statType;
        this.element = element;
        this.valueType = valueType;
        this.value = value;
        this.target = target;
    }
}
