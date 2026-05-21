package org.example.gersangtrade.catalog.repository;

import org.example.gersangtrade.domain.catalog.LegendGeneral;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LegendGeneralRepository extends JpaRepository<LegendGeneral, Long> {

    Optional<LegendGeneral> findByMercenaryId(Long mercenaryId);
}
