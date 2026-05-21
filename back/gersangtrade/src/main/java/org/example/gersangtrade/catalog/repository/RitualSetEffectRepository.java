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
}
