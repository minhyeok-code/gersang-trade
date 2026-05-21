package org.example.gersangtrade.domain.deck;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.gersangtrade.domain.catalog.EquipmentItem;
import org.example.gersangtrade.domain.deck.enums.EquipSlot;

/**
 * 덱 용병의 장비 슬롯 엔티티.
 * 용병 1명당 최대 18슬롯 (일반 9 + 외변 9).
 * 미착용 슬롯은 row를 생성하지 않는다 — equipmentItem=null 행 없음.
 */
@Entity
@Table(
        name = "user_deck_member_slots",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_user_deck_member_slot",
                columnNames = {"deck_member_id", "slot"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserDeckMemberSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deck_member_id", nullable = false)
    private UserDeckMember deckMember;

    /**
     * 착용 슬롯.
     * APP_* 슬롯에는 EquipmentKind=APPEARANCE 아이템만 착용 가능.
     * RING_1 / RING_2 슬롯에는 EquipmentSlot.RING 아이템 착용 가능 (서비스 레이어 검증).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "slot", nullable = false, length = 30)
    private EquipSlot slot;

    /** 착용 아이템. null 불가 — 아이템 없으면 row 자체를 삭제한다. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_item_id", nullable = false)
    private EquipmentItem equipmentItem;

    /**
     * 주술 정보. 주술 없으면 null.
     * cascade=ALL + orphanRemoval로 슬롯 삭제 시 주술도 함께 삭제된다.
     */
    @OneToOne(mappedBy = "deckMemberSlot", cascade = CascadeType.ALL, orphanRemoval = true)
    private UserDeckMemberSlotRitual ritual;

    public static UserDeckMemberSlot of(UserDeckMember deckMember, EquipSlot slot, EquipmentItem item) {
        UserDeckMemberSlot s = new UserDeckMemberSlot();
        s.deckMember = deckMember;
        s.slot = slot;
        s.equipmentItem = item;
        return s;
    }

    /** 착용 아이템 교체 — 주술은 초기화된다 */
    public void changeItem(EquipmentItem item) {
        this.equipmentItem = item;
        this.ritual = null;
    }

    /** 주술 등록 */
    public void applyRitual(UserDeckMemberSlotRitual ritual) {
        this.ritual = ritual;
    }

    /** 주술 해제 */
    public void removeRitual() {
        this.ritual = null;
    }
}
