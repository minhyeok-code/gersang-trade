package org.example.gersangtrade.catalog.repository;

import org.example.gersangtrade.domain.catalog.ItemSkill;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ItemSkillRepository extends JpaRepository<ItemSkill, Long> {

    /** skill_key로 단건 조회 — skill_key UNIQUE 기반 */
    Optional<ItemSkill> findBySkillKey(String skillKey);

    /** skill_name으로 단건 조회 — skill_name UNIQUE 기반 */
    Optional<ItemSkill> findBySkillName(String skillName);

    /** 스킬 ID 목록으로 일괄 조회 — 계산기용 배치 로딩 */
    List<ItemSkill> findByIdIn(List<Long> ids);

    /** 스킬명 부분 검색 (페이징) — 관리자 목록 API 전용 */
    @Query("SELECT s FROM ItemSkill s WHERE (:name IS NULL OR s.skillName LIKE %:name%)")
    Page<ItemSkill> searchBySkillName(@Param("name") String name, Pageable pageable);
}
