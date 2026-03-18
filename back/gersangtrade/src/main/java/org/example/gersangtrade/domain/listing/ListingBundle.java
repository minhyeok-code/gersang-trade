package org.example.gersangtrade.domain.listing;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
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
     * 사용자 임의 제목 오버라이드.
     * null: 시스템이 번들 구성을 기반으로 자동 생성한 제목을 사용.
     * non-null: 판매자가 직접 입력한 제목으로 표시.
     */
    @Column(name = "title_override", length = 200)
    private String titleOverride;

    @Builder
    public ListingBundle(TradeListing listing, BundleType bundleType, String titleOverride) {
        this.listing = listing;
        this.bundleType = bundleType;
        this.titleOverride = titleOverride;
    }
}
