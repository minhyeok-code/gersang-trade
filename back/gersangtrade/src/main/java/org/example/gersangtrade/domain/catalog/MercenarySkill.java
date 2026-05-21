package org.example.gersangtrade.domain.catalog;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 용병 고유 스킬 엔티티.
 * 거상짱 용병 페이지 .w-stat 셀에서 수치 없이 텍스트로만 표기되는 스킬명을 저장한다.
 * 예: 성훈빙의, 태선빙의, 화선빙의 등
 *
 * <p>skillKey는 거니버스 내부 식별 키다. 거상짱 크롤링 시에는 null이며,
 * 거니버스 데이터 적재 후 채워진다. SkillCoefficient와의 연결 키로 사용된다.
 */
@Entity
@Table(
        name = "mercenary_skills",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_mercenary_skills_merc_skill",
                columnNames = {"mercenary_id", "skill_name"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MercenarySkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mercenary_id", nullable = false)
    private Mercenary mercenary;

    @Column(name = "skill_name", nullable = false, length = 100)
    private String skillName;

    /** 거니버스 내부 스킬 식별 키 — 예: "tlsxhfb", "qldghk". 거상짱 크롤링 시에는 null. */
    @Column(name = "skill_key", length = 100)
    private String skillKey;

    @Builder
    public MercenarySkill(Mercenary mercenary, String skillName, String skillKey) {
        this.mercenary = mercenary;
        this.skillName = skillName;
        this.skillKey = skillKey;
    }

    public void updateSkillKey(String skillKey) {
        this.skillKey = skillKey;
    }
}
