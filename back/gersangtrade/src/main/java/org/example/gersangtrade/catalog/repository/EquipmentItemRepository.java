package org.example.gersangtrade.catalog.repository;

import org.example.gersangtrade.domain.catalog.EquipmentItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * 장비 아이템 상세 정보 레포지토리.
 * EquipmentItem은 Item과 PK를 공유(@MapsId)하므로 itemId로 조회한다.
 */
public interface EquipmentItemRepository extends JpaRepository<EquipmentItem, Long> {

    /**
     * 아이템 ID로 장비 상세 정보 조회.
     * 매물 등록 시 장비 유효성 검사 및 스냅샷 저장에 사용된다.
     *
     * @param itemId Item.id (= EquipmentItem.itemId)
     */
    @Query("""
            SELECT ei FROM EquipmentItem ei
            JOIN FETCH ei.item
            WHERE ei.itemId = :itemId
            """)
    Optional<EquipmentItem> findWithItemByItemId(@Param("itemId") Long itemId);

    /**
     * 아이템 ID로 장비 상세 정보 존재 여부 확인.
     * 매물 등록 시 장비 타입 검증에 사용된다.
     */
    boolean existsByItemId(Long itemId);
}
