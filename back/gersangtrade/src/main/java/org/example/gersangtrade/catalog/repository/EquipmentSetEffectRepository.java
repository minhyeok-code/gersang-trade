package org.example.gersangtrade.catalog.repository;

import org.example.gersangtrade.domain.catalog.EquipmentSetEffect;
import org.example.gersangtrade.domain.catalog.enums.Element;
import org.example.gersangtrade.domain.catalog.enums.StatType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 장비 세트 효과 레포지토리.
 * upsert 기준 키: equipment_set_id + required_pieces + stat_type + element
 */
public interface EquipmentSetEffectRepository extends JpaRepository<EquipmentSetEffect, Long> {

    boolean existsByEquipmentSet_IdAndRequiredPiecesAndStatTypeAndElement(
            Long equipmentSetId, Integer requiredPieces, StatType statType, Element element);

    /** 세트 ID 목록에 해당하는 효과 일괄 조회 — DPS 계산기 배치 로딩용 */
    @Query("SELECT e FROM EquipmentSetEffect e WHERE e.equipmentSet.id IN :setIds")
    List<EquipmentSetEffect> findBySetIdIn(@Param("setIds") List<Long> setIds);
}
