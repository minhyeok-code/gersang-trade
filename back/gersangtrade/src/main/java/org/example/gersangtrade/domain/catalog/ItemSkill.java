package org.example.gersangtrade.domain.catalog;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 아이템 고유 스킬 엔티티.
 * 거상짱 아이템 페이지의 .w-stat 셀에서 수치 없이 텍스트만 표기되는 스킬명을 저장한다.
 * 예: 도선빙의, 화염방패, 신선도술 등
 *
 * <p>skillKey는 거니버스 내부 식별 키다. 거상짱 크롤링 시에는 null이며,
 * 거니버스 데이터 적재 후 채워진다. SkillCoefficient와의 연결 키로 사용된다.
 */
@Entity
@Table(
        name = "item_skills",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_item_skills_item_skill",
                columnNames = {"item_id", "skill_name"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ItemSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(name = "skill_name", nullable = false, length = 100)
    private String skillName;

    /** 거니버스 내부 스킬 식별 키 — 예: "dmsgktnrkd". 거상짱 크롤링 시에는 null. */
    @Column(name = "skill_key", length = 100)
    private String skillKey;

    @Builder
    public ItemSkill(Item item, String skillName, String skillKey) {
        this.item = item;
        this.skillName = skillName;
        this.skillKey = skillKey;
    }

    public void updateSkillKey(String skillKey) {
        this.skillKey = skillKey;
    }
}
