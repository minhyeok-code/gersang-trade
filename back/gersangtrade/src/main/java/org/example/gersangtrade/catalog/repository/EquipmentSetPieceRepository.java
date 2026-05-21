package org.example.gersangtrade.catalog.repository;

import org.example.gersangtrade.domain.catalog.EquipmentSetPiece;
import org.example.gersangtrade.domain.catalog.enums.EquipmentSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** 장비 세트 피스 레포지토리 */
public interface EquipmentSetPieceRepository extends JpaRepository<EquipmentSetPiece, Long> {

    boolean existsByEquipmentSetIdAndSlot(Long equipmentSetId, EquipmentSlot slot);

    /** 세트 ID로 피스 목록 조회 — 주술 적용 가능 아이템 저장 시 세트 전 피스에 일괄 적용할 때 사용 */
    List<EquipmentSetPiece> findByEquipmentSetId(Long equipmentSetId);
}
