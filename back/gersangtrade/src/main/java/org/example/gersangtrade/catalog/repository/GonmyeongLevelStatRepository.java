package org.example.gersangtrade.catalog.repository;

import org.example.gersangtrade.domain.catalog.GonmyeongLevelStat;
import org.example.gersangtrade.domain.catalog.enums.StatType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GonmyeongLevelStatRepository extends JpaRepository<GonmyeongLevelStat, Long> {

    List<GonmyeongLevelStat> findByLevel(int level);

    Optional<GonmyeongLevelStat> findByLevelAndStatType(int level, StatType statType);
}
