package org.example.gersangtrade.domain.catalog;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 아이템-스킬 M:N 매핑 엔티티.
 * 동일한 스킬명을 여러 아이템이 공유할 수 있으므로 조인 테이블로 관리한다.
 */
@Entity
@Table(
        name = "item_skill_mapping",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_item_skill_mapping",
                columnNames = {"item_id", "skill_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ItemSkillMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id", nullable = false)
    private ItemSkill skill;

    public ItemSkillMapping(Item item, ItemSkill skill) {
        this.item = item;
        this.skill = skill;
    }
}
