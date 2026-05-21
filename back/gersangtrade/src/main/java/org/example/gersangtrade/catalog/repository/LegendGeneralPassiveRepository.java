package org.example.gersangtrade.catalog.repository;

import org.example.gersangtrade.domain.catalog.LegendGeneralPassive;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LegendGeneralPassiveRepository extends JpaRepository<LegendGeneralPassive, Long> {

    List<LegendGeneralPassive> findByLegendGeneralId(Long legendGeneralId);
}
