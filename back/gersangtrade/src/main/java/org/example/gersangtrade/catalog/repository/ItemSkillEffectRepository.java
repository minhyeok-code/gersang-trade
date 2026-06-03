package org.example.gersangtrade.catalog.repository;

import org.example.gersangtrade.domain.catalog.ItemSkillEffect;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ItemSkillEffectRepository extends JpaRepository<ItemSkillEffect, Long> {

    List<ItemSkillEffect> findBySkillIdIn(List<Long> skillIds);

    void deleteBySkillId(Long skillId);
}
