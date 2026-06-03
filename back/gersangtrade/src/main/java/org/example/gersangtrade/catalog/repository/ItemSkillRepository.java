package org.example.gersangtrade.catalog.repository;

import org.example.gersangtrade.domain.catalog.ItemSkill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ItemSkillRepository extends JpaRepository<ItemSkill, Long> {

    /** skill_key로 단건 조회 — skill_key UNIQUE 기반 */
    Optional<ItemSkill> findBySkillKey(String skillKey);

    /** skill_name으로 단건 조회 — skill_name UNIQUE 기반 */
    Optional<ItemSkill> findBySkillName(String skillName);

    /** 스킬 ID 목록으로 일괄 조회 — 계산기용 배치 로딩 */
    List<ItemSkill> findByIdIn(List<Long> ids);
}
