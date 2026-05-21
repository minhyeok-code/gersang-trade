package org.example.gersangtrade.domain.catalog;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.gersangtrade.domain.catalog.enums.LegendGeneralType;

import java.util.ArrayList;
import java.util.List;

/**
 * 전설장수 엔티티.
 * TYPE_A: 레벨 기반 패시브 + 특성 강화 (주몽/맹획/노부츠나/바지라오/초선).
 * TYPE_B: 패시브 없음, 특성 포인트 배분으로만 버프 발동 (나머지 10마리).
 */
@Entity
@Table(name = "legend_general")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LegendGeneral {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mercenary_id", nullable = false, unique = true)
    private Mercenary mercenary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private LegendGeneralType type;

    @OneToMany(mappedBy = "legendGeneral", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LegendGeneralPassive> passives = new ArrayList<>();

    @OneToMany(mappedBy = "legendGeneral", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LegendGeneralCharacteristic> characteristics = new ArrayList<>();

    @Builder
    public LegendGeneral(Mercenary mercenary, LegendGeneralType type) {
        this.mercenary = mercenary;
        this.type = type;
    }
}
