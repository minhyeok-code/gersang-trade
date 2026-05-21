package org.example.gersangtrade.domain.catalog;

import jakarta.persistence.*;
import lombok.*;
import org.example.gersangtrade.domain.catalog.enums.BuffTarget;
import org.example.gersangtrade.domain.catalog.enums.Element;
import org.example.gersangtrade.domain.catalog.enums.StatType;
import org.example.gersangtrade.domain.catalog.enums.StatUnit;
import org.example.gersangtrade.domain.common.BaseEntity;

/**
 * 정령 버프 엔티티.
 * 정령 1개는 버프 1~6개를 가진다.
 *
 * <p>element 필드 사용 규칙:
 * ELEMENT_VALUE 버프에만 의미가 있으며, 나머지 버프는 NONE으로 저장한다.
 * ADAPTIVE = 착용 용병 속성 추종, EARTH 등 = 해당 속성 용병에게만 적용.
 */
@Entity
@Table(name = "spirit_buffs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SpiritBuff extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "spirit_id", nullable = false)
    private Spirit spirit;

    /** 버프 능력치 종류 */
    @Enumerated(EnumType.STRING)
    @Column(name = "stat_type", nullable = false, length = 30)
    private StatType statType;

    /** 속성 구분 — ELEMENT_VALUE 버프에만 의미 있음. 그 외는 NONE */
    @Enumerated(EnumType.STRING)
    @Column(name = "element", nullable = false, length = 20)
    private Element element;

    /** 수치 단위 — FLAT(고정값) 또는 PERCENT(퍼센트) */
    @Enumerated(EnumType.STRING)
    @Column(name = "stat_unit", nullable = false, length = 20)
    private StatUnit statUnit;

    /** 버프 수치. 양수=버프, 음수=디버프 */
    @Column(nullable = false)
    private float value;

    /** 버프 적용 대상 — ALLY(아군) 또는 ENEMY(적) */
    @Enumerated(EnumType.STRING)
    @Column(name = "target", nullable = false, length = 10)
    private BuffTarget target;
}
