package org.example.gersangtrade.catalog.repository;

import org.example.gersangtrade.domain.catalog.MercenarySkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MercenarySkillRepository extends JpaRepository<MercenarySkill, Long> {

    List<MercenarySkill> findByMercenaryId(Long mercenaryId);

    @Query("SELECT COUNT(ms) > 0 FROM MercenarySkill ms WHERE ms.mercenary.id = :mercenaryId AND ms.skillName = :skillName")
    boolean existsByMercenaryIdAndSkillName(@Param("mercenaryId") Long mercenaryId,
                                            @Param("skillName") String skillName);

    void deleteByMercenaryId(Long mercenaryId);
}
