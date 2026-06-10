package org.example.gersangtrade.listing.repository;

import org.example.gersangtrade.domain.listing.BundleEquipmentDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 번들 장비 상세 정보 기본 CRUD 레포지토리.
 * BundleEquipmentDetail은 BundleLine과 PK를 공유(@MapsId)한다.
 */
public interface BundleEquipmentDetailRepository extends JpaRepository<BundleEquipmentDetail, Long> {

    /**
     * 여러 번들 라인 ID에 해당하는 장비 상세 목록 일괄 조회.
     * 등록글 상세 조회에서 N+1 방지를 위해 IN 쿼리로 미리 로드한다.
     */
    List<BundleEquipmentDetail> findByBundleLineIdIn(List<Long> bundleLineIds);

    /**
     * 장비·세트 정보를 fetch join으로 일괄 조회한다.
     * 세트 제목 재계산 시 N+1을 방지한다.
     */
    @Query("SELECT d FROM BundleEquipmentDetail d "
            + "JOIN FETCH d.equipmentItem ei "
            + "LEFT JOIN FETCH ei.equipmentSet "
            + "WHERE d.bundleLineId IN :bundleLineIds")
    List<BundleEquipmentDetail> findWithEquipmentSetByBundleLineIdIn(
            @Param("bundleLineIds") List<Long> bundleLineIds);
}
