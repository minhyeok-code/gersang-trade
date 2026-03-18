package org.example.gersangtrade.domain.catalog;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 재료 아이템 상세 정보 엔티티.
 * Item 엔티티와 @MapsId 패턴으로 PK를 공유한다 (1:1, Shared PK).
 * type=MATERIAL인 Item에 대해 1:1로 존재하며, 수량 단위 정보를 보관한다.
 */
@Entity
@Table(name = "material_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MaterialItem {

    /** Item의 PK와 공유하는 기본 키 */
    @Id
    private Long itemId;

    /** 연결된 기반 아이템 — @MapsId로 itemId를 item.id에서 주입받는다 */
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "item_id")
    private Item item;

    /**
     * 수량 단위명.
     * 예: "개", "묶음". 거래 목록에서 "100개", "5묶음" 형태로 표시된다.
     * nullable — 단위 표기가 불필요한 재료의 경우 null 허용.
     */
    @Column(name = "stack_unit_name", length = 20)
    private String stackUnitName;

    @Builder
    public MaterialItem(Item item, String stackUnitName) {
        this.item = item;
        this.stackUnitName = stackUnitName;
    }
}
