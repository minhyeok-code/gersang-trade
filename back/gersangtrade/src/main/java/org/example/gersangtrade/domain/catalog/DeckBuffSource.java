package org.example.gersangtrade.domain.catalog;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.gersangtrade.domain.catalog.enums.DeckBuffSourceType;

import java.util.ArrayList;
import java.util.List;

/**
 * 덱 버프 출처 — 진법/층진/전설장수 패시브.
 * sourceId: LEGEND_GENERAL이면 LegendGeneral.id, 나머지는 자유값.
 */
@Entity
@Table(name = "deck_buff_source")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeckBuffSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeckBuffSourceType sourceType;

    /** LEGEND_GENERAL이면 LegendGeneral.id */
    @Column(nullable = false)
    private Long sourceId;

    /** 표시용 — 예: "주몽 패시브", "진법 A" */
    @Column(nullable = false, length = 50)
    private String name;

    @OneToMany(mappedBy = "source", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DeckBuff> buffs = new ArrayList<>();

    @Builder
    public DeckBuffSource(DeckBuffSourceType sourceType, Long sourceId, String name) {
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.name = name;
    }
}
