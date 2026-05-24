package org.example.gersangtrade.catalog.repository;

import org.example.gersangtrade.domain.catalog.LegendGeneral;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LegendGeneralRepository extends JpaRepository<LegendGeneral, Long> {

    Optional<LegendGeneral> findByMercenaryId(Long mercenaryId);

    /** 특성 목록만 fetch. effects·passives는 별도 쿼리로 로드한다 (MultipleBagFetchException 방지). */
    @Query("""
            SELECT DISTINCT lg FROM LegendGeneral lg
            LEFT JOIN FETCH lg.characteristics
            WHERE lg.mercenary.id = :mercenaryId
            """)
    Optional<LegendGeneral> findByMercenaryIdWithDetails(@Param("mercenaryId") Long mercenaryId);
}
