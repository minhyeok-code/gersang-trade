package org.example.gersangtrade.domain.catalog;

import jakarta.persistence.*;
import lombok.*;
import org.example.gersangtrade.domain.catalog.enums.Nature;
import org.example.gersangtrade.domain.catalog.enums.SpiritGrade;
import org.example.gersangtrade.domain.common.BaseEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * 정령 엔티티.
 * 속성 5종 × 등급 5단계 = 총 25종. 크롤링 없이 하드코딩 시딩.
 *
 * <p>유저 덱에 최대 2개 장착 가능하며, 같은 정령 중복 장착 불가.
 * 버프 합산은 SpiritBuffCalculator가 담당한다.
 */
@Entity
@Table(
        name = "spirits",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_spirits_nature_grade",
                columnNames = {"nature", "grade"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Spirit extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 정령 이름 — 예: "어린 심아리" */
    @Column(nullable = false, length = 100)
    private String name;

    /** 정령 자체 속성 — FIRE·WATER·THUNDER·WIND·EARTH */
    @Enumerated(EnumType.STRING)
    @Column(name = "nature", nullable = false, length = 20)
    private Nature nature;

    /** 정령 등급 — LOWER·MIDDLE·UPPER·HIGHEST·LEGEND */
    @Enumerated(EnumType.STRING)
    @Column(name = "grade", nullable = false, length = 20)
    private SpiritGrade grade;

    /** 획득 조건 텍스트 — 예: "친밀도50000, 물의정령옥30, 봉인의서3" */
    @Column(name = "acquire_condition", columnDefinition = "TEXT")
    private String acquireCondition;

    /** 구조화가 어려운 특수 효과 원문. 없으면 null */
    @Column(name = "special_effect_note", columnDefinition = "TEXT")
    private String specialEffectNote;

    @Builder.Default
    @OneToMany(mappedBy = "spirit", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SpiritBuff> buffs = new ArrayList<>();

    /** 화면 표시용 레이블 — 예: "풍 전설 어린 각웅" */
    public String getDisplayLabel() {
        return nature.getDisplayName() + " " + grade.getDisplayName() + " " + name;
    }
}
