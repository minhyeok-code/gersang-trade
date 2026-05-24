package org.example.gersangtrade.catalog.repository;

import org.example.gersangtrade.domain.catalog.ItemSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ItemSkillRepository extends JpaRepository<ItemSkill, Long> {

    List<ItemSkill> findByItemId(Long itemId);

    @Query("""
            SELECT COUNT(isk) > 0 FROM ItemSkill isk
            WHERE isk.item.id = :itemId AND isk.skillName = :skillName
            """)
    boolean existsByItemIdAndSkillName(@Param("itemId") Long itemId,
                                       @Param("skillName") String skillName);

    void deleteByItemId(Long itemId);

    /** item_id + 스킬명으로 단건 조회 — 스킬 계수 적재 시 find-or-create에 사용된다 */
    @Query("""
            SELECT isk FROM ItemSkill isk
            WHERE isk.item.id = :itemId AND isk.skillName = :skillName
            """)
    java.util.Optional<ItemSkill> findByItemIdAndSkillName(@Param("itemId") Long itemId,
                                                           @Param("skillName") String skillName);

    /** 거니버스 skillKey로 기존 스킬 조회 — itemKey가 비어 있는 기존 데이터 보완용 */
    List<ItemSkill> findBySkillKey(String skillKey);

    /** 스킬명으로 기존 스킬 조회 — skillKey가 아직 비어 있는 크롤링 데이터 보완용 */
    List<ItemSkill> findBySkillName(String skillName);
}
