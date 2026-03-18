package org.example.gersangtrade.listing.repository;

import org.example.gersangtrade.domain.listing.BundleEquipmentRitual;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 번들 장비 주술 결과 기본 CRUD 레포지토리.
 */
public interface BundleEquipmentRitualRepository extends JpaRepository<BundleEquipmentRitual, Long> {

    /**
     * 여러 번들 라인 ID에 해당하는 주술 결과 목록 일괄 조회.
     * 등록글 상세 조회에서 N+1 방지를 위해 IN 쿼리로 미리 로드한다.
     * Ritual 엔티티를 fetch join으로 함께 조회해 추가 쿼리를 방지한다.
     */
    @Query("SELECT ber FROM BundleEquipmentRitual ber " +
           "JOIN FETCH ber.ritual " +
           "WHERE ber.bundleLine.id IN :bundleLineIds")
    List<BundleEquipmentRitual> findWithRitualByBundleLineIdIn(
            @Param("bundleLineIds") List<Long> bundleLineIds);
}
