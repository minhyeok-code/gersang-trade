package org.example.gersangtrade.domain.catalog;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 용병 고용 재료 엔티티.
 * 특정 용병을 고용하는 데 필요한 재료 아이템과 수량을 정의한다.
 * geota 용병 목록 페이지에서 재료명·수량을 파싱하여 적재한다.
 *
 * <p>동일 용병-재료 조합이 중복 저장되지 않도록 UNIQUE 제약을 둔다.
 */
@Entity
@Table(
        name = "mercenary_materials",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_mercenary_materials_mercenary_item",
                columnNames = {"mercenary_id", "item_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MercenaryMaterial {

    /** 고용 재료 고유 식별자 (자동 증가) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 이 재료가 필요한 용병 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mercenary_id", nullable = false)
    private Mercenary mercenary;

    /** 필요한 재료 아이템 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    /** 필요 수량 */
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Builder
    public MercenaryMaterial(Mercenary mercenary, Item item, Integer quantity) {
        this.mercenary = mercenary;
        this.item = item;
        this.quantity = quantity;
    }
}
