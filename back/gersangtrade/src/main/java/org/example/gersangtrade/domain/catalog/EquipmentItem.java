package org.example.gersangtrade.domain.catalog;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.gersangtrade.domain.catalog.enums.Enhancement;
import org.example.gersangtrade.domain.catalog.enums.EquipmentKind;
import org.example.gersangtrade.domain.catalog.enums.EquipmentSlot;
import org.example.gersangtrade.domain.deck.enums.EquipSlot;

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

    /**
     * 주술 가능 여부.
     * true이면 RitualApplicability를 통해 적용 가능한 주술 목록이 존재한다.
     * 크롤러 Batch Job이 거상짱 파싱 결과를 바탕으로 설정한다.
     */
    @Column(name = "ritual_applicable", nullable = false)
    private boolean ritualApplicable = false;

    /**
     * 덱 장비 슬롯 매핑.
     * APPEARANCE 아이템: APP_* 값 (어느 외변 슬롯에 착용되는지). 수동 관리.
     * NORMAL 아이템: 대응하는 일반 슬롯 값. RING 아이템은 null (RING_1/RING_2 둘 다 가능).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "equip_slot", length = 30)
    private EquipSlot equipSlot;

    /**
     * 전용장비 소유 용병 (카탈로그 표시용, nullable).
     * null이면 공용 장비 또는 restriction이 2명 이상·카테고리 전용인 경우.
     * 런타임 착용 ACL은 {@link ItemMercenaryRestriction}이 담당한다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mercenary_id")
    private Mercenary mercenary;

    /**
     * 전설장수 장비 기본 강화 단계.
     * 전설장수 세트의 대표 강화 수치 (0강 / 5강 / 10강).
     * 일반 장비는 null. DB에는 실제 숫자(0/5/10)로 저장한다.
     */
    @Convert(converter = EnhancementConverter.class)
    @Column(name = "enhancement")
    private Enhancement enhancement;

    /**
     * 홈이 있는(슬롯 옵션) 버전 존재 여부.
     * true이면 보석을 장착할 수 있는 홈 버전이 별도로 존재함을 의미한다.
     * 크롤러 Batch Job이 거상짱 파싱 결과를 바탕으로 설정한다.
     */
    @Column(name = "has_slot_option", nullable = false)
    private boolean hasSlotOption = false;

    /**
     * 사인검 여부.
     * true이면 주인공 공명 주스텟을 속성 무관 지력(INTELLECT)으로 결정한다.
     * 관리자가 수동으로 설정한다.
     */
    @Column(name = "is_sain_sword", nullable = false)
    private boolean sainSword = false;

    @Builder
    public EquipmentItem(Item item, EquipmentKind equipmentKind,
                         EquipmentSlot slot, EquipmentSet equipmentSet,
                         boolean ritualApplicable, boolean hasSlotOption,
                         EquipSlot equipSlot, Mercenary mercenary,
                         Enhancement enhancement, boolean sainSword) {
        this.item = item;
        this.equipmentKind = equipmentKind;
        this.slot = slot;
        this.equipmentSet = equipmentSet;
        this.ritualApplicable = ritualApplicable;
        this.hasSlotOption = hasSlotOption;
        this.equipSlot = equipSlot;
        this.mercenary = mercenary;
        this.enhancement = enhancement;
        this.sainSword = sainSword;
    }

    /** 세트 크롤러 upsert 시 세트 소속 FK 갱신 */
    public void updateEquipmentSet(EquipmentSet equipmentSet) {
        this.equipmentSet = equipmentSet;
    }

    /** 크롤러 재실행 시 null인 slot/equipSlot 갱신 */
    public void updateSlotInfo(EquipmentSlot slot, EquipSlot equipSlot) {
        if (slot != null) this.slot = slot;
        if (equipSlot != null) this.equipSlot = equipSlot;
    }

    /** 관리자 수동 수정 — 슬롯, 장비 종류, 주술 가능 여부, 홈 옵션 여부, 세트 소속, 덱 슬롯, 전용 용병, 강화 단계, 사인검 여부 */
    public void updateInfo(EquipmentSlot slot, EquipmentKind equipmentKind,
                           boolean ritualApplicable, boolean hasSlotOption,
                           EquipmentSet equipmentSet, EquipSlot equipSlot,
                           Mercenary mercenary, Enhancement enhancement,
                           boolean sainSword) {
        if (slot != null) this.slot = slot;
        if (equipmentKind != null) this.equipmentKind = equipmentKind;
        this.ritualApplicable = ritualApplicable;
        this.hasSlotOption = hasSlotOption;
        this.equipmentSet = equipmentSet;
        this.equipSlot = equipSlot;
        this.mercenary = mercenary;
        this.enhancement = enhancement;
        this.sainSword = sainSword;
    }

    /** 전용장비 크롤러 — 단일 소유 용병 FK 갱신 (이미 설정된 경우 덮어쓰지 않음) */
    public void updateExclusiveMercenaryIfAbsent(Mercenary mercenary) {
        if (mercenary != null && this.mercenary == null) {
            this.mercenary = mercenary;
        }
    }
}
