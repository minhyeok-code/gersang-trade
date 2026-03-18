package org.example.gersangtrade.domain.wanted;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.gersangtrade.domain.catalog.Item;

/**
 * 구매 희망 아이템 엔티티.
 * WantedListing 내에서 구매자가 원하는 개별 아이템과 수량을 정의한다.
 * 장비인 경우 WantedEquipmentCondition을 통해 강화·주술 조건이 확장된다.
 */
@Entity
@Table(name = "wanted_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WantedItem {

    /** 구매 희망 아이템 고유 식별자 (자동 증가) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 이 아이템이 속하는 구매 희망 등록글 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wanted_listing_id", nullable = false)
    private WantedListing wantedListing;

    /** 구매 희망 대상 아이템 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    /** 구매 희망 수량 — 재료: 1 이상, 장비: 보통 1 */
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    /** 목록 내 표시 순서 */
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Builder
    public WantedItem(WantedListing wantedListing, Item item,
                      Integer quantity, Integer sortOrder) {
        this.wantedListing = wantedListing;
        this.item = item;
        this.quantity = quantity;
        this.sortOrder = sortOrder;
    }
}
