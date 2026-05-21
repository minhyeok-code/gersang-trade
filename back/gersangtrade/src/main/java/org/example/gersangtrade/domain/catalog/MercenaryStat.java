package org.example.gersangtrade.domain.catalog;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.gersangtrade.domain.catalog.enums.StatType;

/**
 * 용병 능력치 엔티티.
 * 한 용병의 스탯(힘, 민첩, 저항깎, 속성값 등)을 타입별로 저장한다.
 * StatType은 ItemStat과 공유하며, MVP 필수 타입은 ELEMENT_VALUE / ELEMENT_PIERCE / RESIST_PIERCE.
 *
 * <p>UNIQUE(mercenary_id, stat_key): 동일 용병에 동일 스탯 타입 중복 방지.
 */
@Entity
@Table(
        name = "mercenary_stats",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_mercenary_stats_mercenary_stat_key",
                columnNames = {"mercenary_id", "stat_key"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MercenaryStat {

    /** 용병 능력치 고유 식별자 (자동 증가) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 능력치를 보유한 용병 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mercenary_id", nullable = false)
    private Mercenary mercenary;

    /**
     * 능력치 종류.
     * MVP 필수: ELEMENT_VALUE(속성값) / ELEMENT_PIERCE(속성깎) / RESIST_PIERCE(저항깎)
     * Phase 2 확장: STRENGTH / VITALITY / DEXTERITY / INTELLECT / DEFENSE 등
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "stat_key", nullable = false, length = 30)
    private StatType statKey;

    /** 해당 스탯의 수치 */
    @Column(name = "stat_value", nullable = false)
    private Integer statValue;

    @Builder
    public MercenaryStat(Mercenary mercenary, StatType statKey, Integer statValue) {
        this.mercenary = mercenary;
        this.statKey = statKey;
        this.statValue = statValue;
    }

    public void updateValue(int value) {
        this.statValue = value;
    }
}
