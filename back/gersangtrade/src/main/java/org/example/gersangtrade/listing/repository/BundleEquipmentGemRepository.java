package org.example.gersangtrade.listing.repository;

import org.example.gersangtrade.domain.listing.BundleEquipmentGem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 번들 장비 보석 JPA 레포지토리.
 * 매물 등록 시 장비 라인에 박힌 보석 정보를 저장하고, 매물 조회 시 보석 목록을 반환한다.
 */
public interface BundleEquipmentGemRepository extends JpaRepository<BundleEquipmentGem, Long> {

    /**
     * 번들 라인 ID로 보석 목록 조회 (Gem fetch join 포함).
     * 매물 상세 조회 시 장비 라인별 보석 정보를 표시하는 데 사용된다.
     */
    @Query("""
            SELECT beg FROM BundleEquipmentGem beg
            JOIN FETCH beg.gem
            WHERE beg.bundleLine.id = :bundleLineId
            """)
    List<BundleEquipmentGem> findWithGemByBundleLineId(@Param("bundleLineId") Long bundleLineId);
}
