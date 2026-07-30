package org.example.gersangtrade.domain.catalog;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.gersangtrade.domain.catalog.enums.Gender;
import org.example.gersangtrade.domain.catalog.enums.JobType;
import org.example.gersangtrade.domain.catalog.enums.Nation;

/**
 * 주인공 상세 정보 엔티티.
 * Mercenary와 1:1 관계. 국가·전직·성별을 저장한다.
 */
@Entity
@Table(name = "player_character_detail")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlayerCharacterDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mercenary_id", nullable = false, unique = true)
    private Mercenary mercenary;

    /** 출신 국가 — 국가 속성 버프 및 ALLY_AUTO 특성 대상 결정에 사용 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Nation nation;

    /** 전직 단계 — SECOND(2차전직)만 계산기 대상 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private JobType jobType;

    /** 성별 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Gender gender;

    @Builder
    public PlayerCharacterDetail(Mercenary mercenary, Nation nation,
                                 JobType jobType, Gender gender) {
        this.mercenary = mercenary;
        this.nation = nation;
        this.jobType = jobType;
        this.gender = gender;
    }
}
