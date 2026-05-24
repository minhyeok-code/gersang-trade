package org.example.gersangtrade.catalog.repository;

import org.example.gersangtrade.domain.catalog.EquipmentSetSkillEffect;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EquipmentSetSkillEffectRepository extends JpaRepository<EquipmentSetSkillEffect, Long> {

    List<EquipmentSetSkillEffect> findByEquipmentSetId(Long setId);

    /** 세트 ID 목록으로 세트 스킬 효과 일괄 조회 — DPS 계산기 배치 로딩용 */
    @org.springframework.data.jpa.repository.Query("""
            SELECT e FROM EquipmentSetSkillEffect e
            JOIN FETCH e.equipmentSet
            JOIN FETCH e.setGrantedSkill
            WHERE e.equipmentSet.id IN :setIds
            """)
    java.util.List<EquipmentSetSkillEffect> findByEquipmentSetIdIn(
            @org.springframework.data.repository.query.Param("setIds") java.util.List<Long> setIds);
}
