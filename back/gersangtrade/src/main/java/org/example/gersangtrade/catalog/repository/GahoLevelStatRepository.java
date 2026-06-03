package org.example.gersangtrade.catalog.repository;

import org.example.gersangtrade.domain.catalog.GahoLevelStat;
import org.example.gersangtrade.domain.catalog.enums.StatType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GahoLevelStatRepository extends JpaRepository<GahoLevelStat, Long> {

    List<GahoLevelStat> findByLevel(int level);

    Optional<GahoLevelStat> findByLevelAndStatType(int level, StatType statType);
}
