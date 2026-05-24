package org.example.gersangtrade.catalog.repository;

import org.example.gersangtrade.domain.catalog.RitualSetEffect;
import org.example.gersangtrade.domain.catalog.enums.StatType;
import org.example.gersangtrade.domain.listing.enums.RitualOutcome;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 주술 세트 효과 레포지토리.
 * UPSERT 기준 키: ritual_id + outcome + equipment_set_id + required_ritual_pieces + stat_type
 */
public interface RitualSetEffectRepository extends JpaRepository<RitualSetEffect, Long> {

    boolean existsByRitual_IdAndOutcomeAndEquipmentSet_IdAndRequiredRitualPiecesAndStatType(
            Long ritualId, RitualOutcome outcome, Long equipmentSetId,
            Integer requiredRitualPieces, StatType statType);

    /** ritualId + setId 기준 배치 조회 — DPS 계산기에서 주술 세트효과 일괄 로딩용 */
    @org.springframework.data.jpa.repository.Query("""
            SELECT r FROM RitualSetEffect r
            JOIN FETCH r.ritual
            JOIN FETCH r.equipmentSet
            WHERE r.ritual.id IN :ritualIds AND r.equipmentSet.id IN :setIds
            """)
    java.util.List<RitualSetEffect> findByRitualIdInAndEquipmentSetIdIn(
            @org.springframework.data.repository.query.Param("ritualIds") java.util.List<Long> ritualIds,
            @org.springframework.data.repository.query.Param("setIds") java.util.List<Long> setIds);
}
