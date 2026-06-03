package org.example.gersangtrade.catalog.repository;

import org.example.gersangtrade.domain.catalog.ItemSkillMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ItemSkillMappingRepository extends JpaRepository<ItemSkillMapping, Long> {

    /** 아이템 ID로 매핑 목록을 스킬과 함께 조회 — 관리자 상세 및 스킬 교체에 사용 */
    @Query("SELECT ism FROM ItemSkillMapping ism JOIN FETCH ism.skill WHERE ism.item.id = :itemId")
    List<ItemSkillMapping> findByItemId(@Param("itemId") Long itemId);

    /** 아이템 ID 목록으로 매핑 목록을 스킬·아이템과 함께 조회 — DPS 계산기 배치 로딩용 */
    @Query("SELECT ism FROM ItemSkillMapping ism JOIN FETCH ism.skill JOIN FETCH ism.item WHERE ism.item.id IN :itemIds")
    List<ItemSkillMapping> findByItemIdIn(@Param("itemIds") List<Long> itemIds);

    /** 아이템의 모든 스킬 매핑 삭제 — 스킬 교체·아이템 삭제 시 사용 */
    void deleteByItemId(Long itemId);

    /** 특정 (아이템, 스킬) 매핑 존재 여부 확인 — find-or-create 중복 방지용 */
    boolean existsByItemIdAndSkillId(Long itemId, Long skillId);
}
