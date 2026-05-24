package org.example.gersangtrade.catalog.repository;

import org.example.gersangtrade.domain.catalog.EquipmentSetPiece;
import org.example.gersangtrade.domain.catalog.enums.EquipmentSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** 장비 세트 피스 레포지토리 */
public interface EquipmentSetPieceRepository extends JpaRepository<EquipmentSetPiece, Long> {

    boolean existsByEquipmentSetIdAndSlot(Long equipmentSetId, EquipmentSlot slot);

    /** 세트 ID로 피스 목록 조회 — 주술 적용 가능 아이템 저장 시 세트 전 피스에 일괄 적용할 때 사용 */
    List<EquipmentSetPiece> findByEquipmentSetId(Long equipmentSetId);

    /** 세트 ID로 피스 목록 조회 (equipmentItem + item fetch join) — 세트 상세 응답 및 일괄 장착에 사용 */
    @Query("""
            SELECT esp FROM EquipmentSetPiece esp
            JOIN FETCH esp.equipmentItem ei
            JOIN FETCH ei.item
            WHERE esp.equipmentSet.id = :setId
            """)
    List<EquipmentSetPiece> findWithItemByEquipmentSetId(@Param("setId") Long setId);

    /** 장비 아이템 삭제 전 세트 피스 매핑을 정리한다. */
    void deleteByEquipmentItem_ItemId(Long equipmentItemId);
}
