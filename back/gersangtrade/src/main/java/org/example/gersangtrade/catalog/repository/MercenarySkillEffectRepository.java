package org.example.gersangtrade.catalog.repository;

import org.example.gersangtrade.domain.catalog.MercenarySkillEffect;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MercenarySkillEffectRepository extends JpaRepository<MercenarySkillEffect, Long> {

    List<MercenarySkillEffect> findBySkillIdIn(List<Long> skillIds);

    /** 덱 계산용 — 스킬 계수 유무와 무관하게 용병 ID 목록으로 전체 스킬 효과를 일괄 조회한다. */
    List<MercenarySkillEffect> findBySkill_MercenaryIdIn(List<Long> mercenaryIds);

    @Modifying
    @Query("DELETE FROM MercenarySkillEffect e WHERE e.skill.id = :skillId")
    void deleteBySkillId(@Param("skillId") Long skillId);

    /** 용병 삭제·스킬 전체 교체 시 해당 용병의 모든 스킬 효과를 일괄 삭제한다. */
    @Modifying
    @Query("DELETE FROM MercenarySkillEffect e WHERE e.skill.mercenary.id = :mercenaryId")
    void deleteBySkill_MercenaryId(@Param("mercenaryId") Long mercenaryId);
}
