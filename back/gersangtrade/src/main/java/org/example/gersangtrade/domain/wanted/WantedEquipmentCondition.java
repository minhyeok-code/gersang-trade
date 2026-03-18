package org.example.gersangtrade.domain.wanted;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 구매 희망 장비 조건 확장 엔티티.
 * WantedItem과 @MapsId 패턴으로 PK를 공유한다 (1:1, Shared PK).
 * 장비 아이템인 경우에만 존재하며, 구매자가 원하는 강화 수치·주술 여부 조건을 보관한다.
 *
 * <p>판매 등록의 BundleEquipmentDetail(확정 스냅샷)과 달리,
 * 여기서는 구매 허용 조건(최소 강화 수치, 주술 여부)을 저장한다.
 */
@Entity
@Table(name = "wanted_equipment_conditions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WantedEquipmentCondition {

    /** WantedItem의 PK와 공유하는 기본 키 */
    @Id
    private Long wantedItemId;

    /** 연결된 구매 희망 아이템 — @MapsId로 wantedItemId를 wantedItem.id에서 주입받는다 */
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "wanted_item_id")
    private WantedItem wantedItem;

    /**
     * 최소 허용 강화 수치.
     * null: 강화 수치 무관.
     * non-null: 해당 수치 이상이어야 수락 가능.
     * 외변(APPEARANCE) 장비의 경우 5만 유효하므로 null 또는 5로 입력한다.
     */
    @Column(name = "min_enhance_level")
    private Integer minEnhanceLevel;

    /**
     * 주술 적용 여부 조건.
     * true: 주술이 적용된 장비를 원함 → WantedRitualCondition 목록이 존재함.
     * false: 주술 미적용 장비를 원함.
     */
    @Column(name = "has_ritual", nullable = false)
    private boolean hasRitual;

    @Builder
    public WantedEquipmentCondition(WantedItem wantedItem,
                                     Integer minEnhanceLevel, boolean hasRitual) {
        this.wantedItem = wantedItem;
        this.minEnhanceLevel = minEnhanceLevel;
        this.hasRitual = hasRitual;
    }
}
