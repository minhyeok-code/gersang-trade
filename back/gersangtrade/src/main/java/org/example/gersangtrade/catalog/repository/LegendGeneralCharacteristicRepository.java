package org.example.gersangtrade.catalog.repository;

import org.example.gersangtrade.domain.catalog.LegendGeneralCharacteristic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LegendGeneralCharacteristicRepository extends JpaRepository<LegendGeneralCharacteristic, Long> {

    List<LegendGeneralCharacteristic> findByLegendGeneralId(Long legendGeneralId);
}
