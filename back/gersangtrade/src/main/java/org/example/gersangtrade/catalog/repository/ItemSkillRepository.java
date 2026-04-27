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
}
