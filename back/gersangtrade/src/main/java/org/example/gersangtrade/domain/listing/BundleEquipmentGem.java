package org.example.gersangtrade.domain.listing;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.gersangtrade.domain.catalog.Gem;

/**
 * 거래 등록 장비 라인의 보석 정보 엔티티.
 * 장비 피스에 박힌 보석을 기록한다.
 * 장비 하나에 여러 보석이 장착될 수 있으므로 BundleLine과 N:1 관계다.
 *
 * <p>BundleEquipmentRitual(주술)과 동일하게 BundleLine에서 확장되는 구조이며,
 * 장비 라인(BundleEquipmentDetail.hasRitual처럼)에서 보석 여부를 관리하는
 * 별도 flag 필드는 현재 없으므로, 이 테이블에 레코드가 존재하면 보석이 있는 것으로 간주한다.
 */
@Entity
@Table(name = "bundle_equipment_gems")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BundleEquipmentGem {

    /** 보석 정보 고유 식별자 (자동 증가) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 이 보석이 속하는 번들 라인 (장비 아이템 라인이어야 한다) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bundle_line_id", nullable = false)
    private BundleLine bundleLine;

    /** 박힌 보석 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gem_id", nullable = false)
    private Gem gem;

    @Builder
    public BundleEquipmentGem(BundleLine bundleLine, Gem gem) {
        this.bundleLine = bundleLine;
        this.gem = gem;
    }
}
