package org.example.gersangtrade.domain.catalog;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.gersangtrade.domain.catalog.enums.StatType;

/**
 * 가호 레벨별 스탯 마스터 테이블.
 * lv1~10: MAIN_STAT_FLAT만 존재.
 * lv11~15: MAIN_STAT_FLAT + DAMAGE_PERCENT.
 * lv16~30: MAIN_STAT_FLAT + DAMAGE_PERCENT + ELEMENT_VALUE.
 *
 * MAIN_STAT_FLAT: 용병별 주스텟 증가 (스킬 계수 최대 스탯 기준, 탱커는 VITALITY).
 * DAMAGE_PERCENT: 전체 용병 데미지 증가 (%).
 * ELEMENT_VALUE:  속성값 증가 (땅속성 용병은 floor(value/2) 적용).
 */
@Entity
@Table(
        name = "gaho_level_stat",
        uniqueConstraints = @UniqueConstraint(columnNames = {"level", "stat_type"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GahoLevelStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 가호 레벨 (1~30) */
    @Column(nullable = false)
    private Integer level;

    @Enumerated(EnumType.STRING)
    @Column(name = "stat_type", nullable = false, length = 30)
    private StatType statType;

    @Column(nullable = false)
    private Integer value;

    public static GahoLevelStat of(int level, StatType statType, int value) {
        GahoLevelStat stat = new GahoLevelStat();
        stat.level = level;
        stat.statType = statType;
        stat.value = value;
        return stat;
    }
}
