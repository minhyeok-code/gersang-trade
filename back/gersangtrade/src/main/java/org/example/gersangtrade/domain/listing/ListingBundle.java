package org.example.gersangtrade.domain.listing;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.gersangtrade.domain.catalog.EquipmentSet;
import org.example.gersangtrade.domain.listing.enums.BundleType;

/**
 * 거래 등록글 내 판매 단위(Bundle) 엔티티.
 * 하나의 TradeListing은 하나 이상의 ListingBundle을 가질 수 있다.
 * 번들 유형에 따라 재료 묶음, 장비 단품, 장비 세트 풀셋 거래를 지원한다.
 * 번들의 실제 구성 아이템은 BundleLine을 통해 관리된다.
 */
@Entity
@Table(name = "listing_bundles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ListingBundle {

    /** 거래 번들 고유 식별자 (자동 증가) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 이 번들이 속하는 거래 등록글 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = false)
    private TradeListing listing;

    /** 번들 유형 — MATERIAL_BUNDLE, EQUIPMENT_SINGLE, EQUIPMENT_SET */
    @Enumerated(EnumType.STRING)
    @Column(name = "bundle_type", nullable = false, length = 30)
    private BundleType bundleType;

    /**
     * 세트 매물일 때 해당 장비 세트 참조.
     * bundleType=EQUIPMENT_SET일 때만 non-null. 나머지는 null.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_set_id")
    private EquipmentSet equipmentSet;

    /**
     * 번들 표시 제목.
     * EQUIPMENT_SET: 등록 시 자동 생성 제목이 저장되나, 조회 시 라인·주술 데이터로 재계산한다.
     * EQUIPMENT_SINGLE 등: 판매자 입력 또는 시스템 생성 제목을 그대로 사용한다.
     */
    @Column(name = "title_override", length = 200)
    private String titleOverride;

    @Builder
    public ListingBundle(TradeListing listing, BundleType bundleType,
                         EquipmentSet equipmentSet, String titleOverride) {
        this.listing = listing;
        this.bundleType = bundleType;
        this.equipmentSet = equipmentSet;
        this.titleOverride = titleOverride;
    }

    /** 시스템 자동 생성 제목을 설정한다. titleOverride가 없을 때만 호출한다. */
    public void updateTitle(String title) {
        this.titleOverride = title;
    }
}
