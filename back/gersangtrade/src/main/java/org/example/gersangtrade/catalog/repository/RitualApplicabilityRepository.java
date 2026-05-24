package org.example.gersangtrade.catalog.repository;

import org.example.gersangtrade.domain.catalog.RitualApplicability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 주술-장비 적용 가능 매핑 레포지토리.
 * 장비 선택 시 허용된 주술 목록을 조회하는 데 사용된다.
 */
public interface RitualApplicabilityRepository extends JpaRepository<RitualApplicability, Long> {

    /**
     * 특정 장비에 적용 가능한 주술 매핑 목록 조회 (Ritual fetch join 포함).
     * 매물 등록 UI에서 "이 장비에 가능한 주술 목록" 드롭다운 구성에 사용된다.
     *
     * @param equipmentItemId EquipmentItem의 ID (= Item.id)
     */
    @Query("""
            SELECT ra FROM RitualApplicability ra
            JOIN FETCH ra.ritual
            WHERE ra.equipmentItem.itemId = :equipmentItemId
            """)
    List<RitualApplicability> findByEquipmentItemIdWithRitual(@Param("equipmentItemId") Long equipmentItemId);

    boolean existsByRitual_IdAndEquipmentItem_ItemId(Long ritualId, Long equipmentItemId);

    /** 장비 아이템 삭제 전 주술 적용 가능 매핑을 정리한다. */
    void deleteByEquipmentItem_ItemId(Long equipmentItemId);
}
