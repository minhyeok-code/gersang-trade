package org.example.gersangtrade.domain.catalog;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.gersangtrade.domain.catalog.enums.MercenaryCategory;

/**
 * 아이템 착용 제한 엔티티.
 *
 * <p>행이 없으면 공용(누구나 착용 가능).
 * 행이 있으면 아래 조건 중 하나를 만족하는 용병만 착용 가능.
 * <ul>
 *   <li>mercenaryId 일치 — 특정 1명(또는 일반/각성 공유 시 2행)
 *   <li>category 일치 — 해당 카테고리 전원 (예: PROTAGONIST)
 * </ul>
 *
 * <p>한 행에 mercenaryId와 category 중 정확히 하나만 설정해야 한다.
 */
@Entity
@Table(name = "item_mercenary_restrictions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ItemMercenaryRestriction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 제한 대상 아이템 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    /**
     * 착용 가능한 특정 용병.
     * category와 둘 중 하나만 설정한다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mercenary_id")
    private Mercenary mercenary;

    /**
     * 착용 가능한 카테고리 전체.
     * mercenaryId와 둘 중 하나만 설정한다.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 50)
    private MercenaryCategory category;

    @Builder
    public ItemMercenaryRestriction(Item item, Mercenary mercenary, MercenaryCategory category) {
        if ((mercenary == null) == (category == null)) {
            throw new IllegalArgumentException("mercenary와 category 중 하나만 설정해야 합니다.");
        }
        this.item = item;
        this.mercenary = mercenary;
        this.category = category;
    }

    /** 용병이 이 제한 조건을 통과하는지 반환한다. */
    public boolean allows(Mercenary target) {
        if (mercenary != null) {
            return mercenary.getId().equals(target.getId());
        }
        return category == target.getCategory();
    }
}
