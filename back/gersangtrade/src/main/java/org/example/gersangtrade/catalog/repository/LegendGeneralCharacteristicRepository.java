package org.example.gersangtrade.catalog.repository;

import org.example.gersangtrade.domain.catalog.LegendGeneralCharacteristic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LegendGeneralCharacteristicRepository extends JpaRepository<LegendGeneralCharacteristic, Long> {

    List<LegendGeneralCharacteristic> findByLegendGeneralId(Long legendGeneralId);

    /** characteristic.effects만 별도 fetch — LegendGeneral.characteristics와 동시 bag fetch 금지 */
    @Query("""
            SELECT DISTINCT c FROM LegendGeneralCharacteristic c
            LEFT JOIN FETCH c.effects
            WHERE c.legendGeneral.id = :legendGeneralId
            """)
    List<LegendGeneralCharacteristic> findWithEffectsByLegendGeneralId(@Param("legendGeneralId") Long legendGeneralId);
}
