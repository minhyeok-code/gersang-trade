package org.example.gersangtrade.catalog.repository;

import org.example.gersangtrade.domain.catalog.MercenarySkillEffect;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MercenarySkillEffectRepository extends JpaRepository<MercenarySkillEffect, Long> {

    List<MercenarySkillEffect> findBySkillIdIn(List<Long> skillIds);

    void deleteBySkillId(Long skillId);

    /** 용병 삭제·스킬 전체 교체 시 해당 용병의 모든 스킬 효과를 일괄 삭제한다. */
    void deleteBySkill_MercenaryId(Long mercenaryId);
}
