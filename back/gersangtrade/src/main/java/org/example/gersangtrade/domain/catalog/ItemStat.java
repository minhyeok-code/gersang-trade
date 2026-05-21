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
 * 아이템 능력치 정의 엔티티.
 * 각 아이템의 속성값/속성깎/저항깎 수치를 속성(Element)별로 보관한다.
 * element는 null 대신 NONE 상수를 사용하여 UNIQUE 제약(item_id + stat_type + element)이 정상 동작하도록 한다.
 * 가성비(가성비 = avgPrice ÷ statValue) 계산 시 이 테이블의 value가 기준이 된다.
 */
@Entity
@Table(
        name = "item_stats",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_item_stats_item_stat_element_scope",
                columnNames = {"item_id", "stat_type", "element", "scope"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ItemStat {

    /** 아이템 능력치 고유 식별자 (자동 증가) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 능력치를 보유한 아이템 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    /**
     * 능력치 종류.
     * MVP statType: ELEMENT_VALUE(속성값) | ELEMENT_PIERCE(속성깎) | RESIST_PIERCE(저항깎)
     * 확장 예정 (MVP 이후): ATTACK_POWER | CRIT_RATE | MAIN_STAT_STR | MAIN_STAT_DEX 등
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "stat_type", nullable = false, length = 30)
    private StatType statType;

    /**
     * 속성 구분.
     * FIRE/WATER/WIND/EARTH/THUNDER: 속성별 능력치. ADAPTIVE: 용병 속성 추종
     * NONE: 속성 구분이 없는 경우 (null 대체값 — UNIQUE 제약 동작 보장용)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "element", nullable = false, length = 20)
    private Element element;

    /** 해당 능력치의 수치 */
    @Column(name = "value", nullable = false)
    private Integer value;

    /** 능력치 단위 — FLAT / PERCENT / LEVEL */
    @Enumerated(EnumType.STRING)
    @Column(name = "stat_unit", nullable = false, length = 10)
    private StatUnit statUnit;

    /**
     * 스탯 적용 범위.
     * SELF : 장착 용병 본인에게만 적용 (기본값, 대부분의 아이템).
     * PARTY: 덱 전체 용병에게 적용되는 파티 버프 아이템.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false, length = 10)
    private BuffTarget scope;

    @Builder
    public ItemStat(Item item, StatType statType, Element element,
                    Integer value, StatUnit statUnit, BuffTarget scope) {
        this.item = item;
        this.statType = statType;
        this.element = (element != null) ? element : Element.NONE;
        this.value = value;
        this.statUnit = statUnit;
        this.scope = (scope != null) ? scope : BuffTarget.SELF;
    }
}
