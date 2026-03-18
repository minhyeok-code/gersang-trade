package org.example.gersangtrade.domain.catalog;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.gersangtrade.domain.catalog.enums.EquipmentKind;
import org.example.gersangtrade.domain.catalog.enums.EquipmentSlot;

/**
 * 장비 아이템 상세 정보 엔티티.
 * Item 엔티티와 @MapsId 패턴으로 PK를 공유한다 (1:1, Shared PK).
 * type=EQUIPMENT인 Item에 대해 1:1로 존재하며, 장비 종류·슬롯·세트 소속 정보를 보관한다.
 */
@Entity
@Table(name = "equipment_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EquipmentItem {

    /** Item의 PK와 공유하는 기본 키 */
    @Id
    private Long itemId;

    /** 연결된 기반 아이템 — @MapsId로 itemId를 item.id에서 주입받는다 */
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "item_id")
    private Item item;

    /** 장비 종류 — APPEARANCE: 외변(5강만 거래), NORMAL: 일반 장비 */
    @Enumerated(EnumType.STRING)
    @Column(name = "equipment_kind", nullable = false, length = 20)
    private EquipmentKind equipmentKind;

    /** 장비 착용 슬롯 — WEAPON, HELMET, ARMOR, GLOVES, BELT, SHOES */
    @Enumerated(EnumType.STRING)
    @Column(name = "slot", nullable = false, length = 20)
    private EquipmentSlot slot;

    /**
     * 소속 장비 세트.
     * 세트에 속하는 장비는 non-null, 단품 장비는 null.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "set_id")
    private EquipmentSet equipmentSet;

    @Builder
    public EquipmentItem(Item item, EquipmentKind equipmentKind,
                         EquipmentSlot slot, EquipmentSet equipmentSet) {
        this.item = item;
        this.equipmentKind = equipmentKind;
        this.slot = slot;
        this.equipmentSet = equipmentSet;
    }
}
