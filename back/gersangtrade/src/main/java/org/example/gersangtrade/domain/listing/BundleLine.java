package org.example.gersangtrade.domain.listing;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.gersangtrade.domain.catalog.Item;

/**
 * 번들 구성 라인(Line) 엔티티.
 * ListingBundle 안의 개별 아이템과 수량을 정의한다.
 * 재료 번들: 수량 >= 1, 장비 번들: 보통 quantity=1.
 * 장비인 경우 BundleEquipmentDetail을 통해 강화·주술 정보가 확장된다.
 */
@Entity
@Table(name = "bundle_lines")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BundleLine {

    /** 번들 라인 고유 식별자 (자동 증가) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 이 라인이 속하는 번들 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bundle_id", nullable = false)
    private ListingBundle bundle;

    /** 거래 대상 아이템 (재료 또는 장비) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    /** 수량 — 재료: 1 이상, 장비: 보통 1 */
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    /** 번들 내 표시 순서 */
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Builder
    public BundleLine(ListingBundle bundle, Item item, Integer quantity, Integer sortOrder) {
        this.bundle = bundle;
        this.item = item;
        this.quantity = quantity;
        this.sortOrder = sortOrder;
    }
}
