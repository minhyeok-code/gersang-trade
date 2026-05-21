package org.example.gersangtrade.catalog.repository;

import org.example.gersangtrade.domain.catalog.RitualStat;
import org.example.gersangtrade.domain.listing.enums.RitualOutcome;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 주술 성공/대성공 스탯 JPA 레포지토리.
 */
public interface RitualStatRepository extends JpaRepository<RitualStat, Long> {

    List<RitualStat> findByRitualId(Long ritualId);

    List<RitualStat> findByRitualIdAndOutcome(Long ritualId, RitualOutcome outcome);

    /** 주술 ID 목록에 해당하는 스탯 일괄 조회 — N+1 방지용 */
    @Query("SELECT rs FROM RitualStat rs WHERE rs.ritual.id IN :ritualIds")
    List<RitualStat> findByRitualIdIn(@Param("ritualIds") List<Long> ritualIds);

    void deleteByRitualId(Long ritualId);
}
