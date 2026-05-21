package org.example.gersangtrade.domain.catalog;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 전설장수 특성의 레벨 행.
 * 포인트를 5 배분하면 characteristicIndex가 같은 행 중 level=5인 행의 effects가 적용된다.
 */
@Entity
@Table(
        name = "legend_general_characteristic",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"legend_general_id", "characteristic_index", "level"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LegendGeneralCharacteristic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "legend_general_id", nullable = false)
    private LegendGeneral legendGeneral;

    /** 같은 전설장수 내 특성 구분 인덱스 (0부터 시작) */
    @Column(name = "characteristic_index", nullable = false)
    private int characteristicIndex;

    /** 특성 레벨 (1~10) */
    @Column(nullable = false)
    private int level;

    @OneToMany(mappedBy = "characteristic", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CharacteristicEffect> effects = new ArrayList<>();

    @Builder
    public LegendGeneralCharacteristic(LegendGeneral legendGeneral,
                                        int characteristicIndex, int level) {
        this.legendGeneral = legendGeneral;
        this.characteristicIndex = characteristicIndex;
        this.level = level;
    }
}
