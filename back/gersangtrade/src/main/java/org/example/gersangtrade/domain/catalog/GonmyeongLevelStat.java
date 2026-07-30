package org.example.gersangtrade.domain.catalog;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.gersangtrade.domain.catalog.enums.StatType;

/**
 * 공명 레벨별 스탯 마스터 테이블.
 * lv1~14: MAIN_STAT_FLAT만 존재.
 * lv15~30: MAIN_STAT_FLAT + DAMAGE_PERCENT 존재.
 *
 * MAIN_STAT_FLAT: 주인공 주스텟 증가 (속성/장비 기준으로 스탯 종류 런타임 결정).
 * DAMAGE_PERCENT: 전체 용병 데미지 증가 (%).
 */
@Entity
@Table(
        name = "gonmyeong_level_stat",
        uniqueConstraints = @UniqueConstraint(columnNames = {"level", "stat_type"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GonmyeongLevelStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 공명 레벨 (1~30) */
    @Column(nullable = false)
    private Integer level;

    @Enumerated(EnumType.STRING)
    @Column(name = "stat_type", nullable = false, length = 30)
    private StatType statType;

    @Column(nullable = false)
    private Integer value;

    public static GonmyeongLevelStat of(int level, StatType statType, int value) {
        GonmyeongLevelStat stat = new GonmyeongLevelStat();
        stat.level = level;
        stat.statType = statType;
        stat.value = value;
        return stat;
    }
}
