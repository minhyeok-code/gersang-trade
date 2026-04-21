package org.example.gersangtrade.domain.listing;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.gersangtrade.domain.catalog.EquipmentItem;
import org.example.gersangtrade.domain.catalog.Gem;
import org.example.gersangtrade.domain.catalog.enums.EquipmentKind;

/**
 * 번들 라인의 장비 상세 정보 확장 엔티티.
 * BundleLine과 @MapsId 패턴으로 PK를 공유한다 (1:1, Shared PK).
 * 장비의 강화 수치, 주술 여부, 보석 정보 등 장비 전용 정보를 보관한다.
 *
 * <p>장비 1개당 보석 최대 1개 슬롯을 가지므로, 보석 정보는 이 엔티티에 gem FK로 직접 포함한다.
 * 별도 BundleEquipmentGem 테이블은 사용하지 않는다.
 *
 * <pre>
 * 필드 구성
 * ┌──────────────────────┬──────────────────┬──────┬──────────────────────────────────────────────────┐
 * │ 필드                 │ 타입             │ Null │ 설명                                             │
 * ├──────────────────────┼──────────────────┼──────┼──────────────────────────────────────────────────┤
 * │ bundleLineId         │ Long (PK/FK)     │ NO   │ BundleLine.id 공유 (@MapsId)                     │
 * │ bundleLine           │ BundleLine       │ NO   │ 1:1 연결 대상                                    │
 * │ equipmentItem        │ EquipmentItem FK │ NO   │ 정합성 체크 및 주술 필터링 용도                  │
 * │ equipmentKindSnapshot│ EquipmentKind    │ NO   │ 등록 시점 스냅샷 (APPEARANCE | NORMAL)           │
 * │ enhanceLevel         │ Integer          │ YES  │ 강화 수치. 외변 정책: 5만 유효 / 일반: 실제 수치 │
 * │ hasRitual            │ boolean          │ NO   │ 주술 적용 여부 — true면 BundleEquipmentRitual 존재│
 * │ gem                  │ Gem FK           │ YES  │ 장착된 보석 — 장비당 최대 1개. 없으면 null       │
 * └──────────────────────┴──────────────────┴──────┴──────────────────────────────────────────────────┘
 * </pre>
 */
@Entity
@Table(name = "bundle_equipment_details")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BundleEquipmentDetail {

    /** BundleLine의 PK와 공유하는 기본 키 */
    @Id
    private Long bundleLineId;

    /** 연결된 번들 라인 — @MapsId로 bundleLineId를 bundleLine.id에서 주입받는다 */
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "bundle_line_id")
    private BundleLine bundleLine;

    /**
     * 장비 아이템 참조 (정합성 체크 및 주술 조회 용도).
     * BundleLine.item과 동일해야 하며, EquipmentItem 기준으로 주술 필터링에 활용된다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_item_id", nullable = false)
    private EquipmentItem equipmentItem;

    /**
     * 등록 시점의 장비 종류 스냅샷.
     * 카탈로그 데이터 변경 시에도 등록 당시의 종류를 유지한다.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "equipment_kind_snapshot", nullable = false, length = 20)
    private EquipmentKind equipmentKindSnapshot;

    /**
     * 강화 수치.
     * 외변(APPEARANCE): 5만 유효한 값.
     * 일반 장비(NORMAL): 실제 강화 수치.
     * nullable — 강화 수치가 거래와 무관한 경우 생략 가능.
     */
    @Column(name = "enhance_level")
    private Integer enhanceLevel;

    /** 주술 적용 여부 — true이면 BundleEquipmentRitual 레코드가 존재한다 */
    @Column(name = "has_ritual", nullable = false)
    private boolean hasRitual;

    /**
     * 장착된 보석 — 장비당 최대 1개 슬롯.
     * null이면 보석 없음. non-null이면 해당 보석이 장착된 상태.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gem_id")
    private Gem gem;

    @Builder
    public BundleEquipmentDetail(BundleLine bundleLine, EquipmentItem equipmentItem,
                                  EquipmentKind equipmentKindSnapshot,
                                  Integer enhanceLevel, boolean hasRitual, Gem gem) {
        this.bundleLine = bundleLine;
        this.equipmentItem = equipmentItem;
        this.equipmentKindSnapshot = equipmentKindSnapshot;
        this.enhanceLevel = enhanceLevel;
        this.hasRitual = hasRitual;
        this.gem = gem;
    }
}
