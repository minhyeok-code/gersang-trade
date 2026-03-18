package org.example.gersangtrade.domain.catalog;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.gersangtrade.domain.catalog.enums.EquipmentSlot;

/**
 * 장비 세트를 구성하는 피스(Piece) 정의 엔티티.
 * 하나의 세트는 여러 피스를 가지며, 각 피스는 슬롯과 해당 장비 아이템이 1:1로 매핑된다.
 * 동일 세트 내 슬롯은 중복될 수 없다 (UNIQUE 제약: set_id + slot).
 */
@Entity
@Table(
        name = "equipment_set_pieces",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_set_pieces_set_id_slot",
                columnNames = {"set_id", "slot"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EquipmentSetPiece {

    /** 세트 피스 고유 식별자 (자동 증가) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 이 피스가 속하는 장비 세트 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "set_id", nullable = false)
    private EquipmentSet equipmentSet;

    /** 이 피스가 차지하는 슬롯 위치 */
    @Enumerated(EnumType.STRING)
    @Column(name = "slot", nullable = false, length = 20)
    private EquipmentSlot slot;

    /** 해당 슬롯의 실제 장비 아이템 정의 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_item_id", nullable = false)
    private EquipmentItem equipmentItem;

    @Builder
    public EquipmentSetPiece(EquipmentSet equipmentSet, EquipmentSlot slot,
                              EquipmentItem equipmentItem) {
        this.equipmentSet = equipmentSet;
        this.slot = slot;
        this.equipmentItem = equipmentItem;
    }
}
