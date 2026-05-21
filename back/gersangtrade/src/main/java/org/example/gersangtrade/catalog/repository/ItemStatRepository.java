package org.example.gersangtrade.catalog.repository;

import org.example.gersangtrade.domain.catalog.ItemStat;
import org.example.gersangtrade.domain.catalog.enums.Element;
import org.example.gersangtrade.domain.catalog.enums.BuffTarget;
import org.example.gersangtrade.domain.catalog.enums.StatType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 아이템 능력치 JPA 레포지토리.
 * 가성비 계산기에서 저항깎·속성값 제공 아이템 목록 조회에 사용된다.
 */
public interface ItemStatRepository extends JpaRepository<ItemStat, Long> {

    /**
     * 능력치 종류별 아이템 목록 조회 (Item fetch join 포함).
     * 가성비 계산기에서 RESIST_PIERCE 또는 ELEMENT_VALUE 아이템 전체를 한 번에 로드한다.
     *
     * @param statType 조회할 능력치 종류 (RESIST_PIERCE | ELEMENT_VALUE | ELEMENT_PIERCE)
     */
    @Query("""
            SELECT ist FROM ItemStat ist
            JOIN FETCH ist.item
            WHERE ist.statType = :statType
            """)
    List<ItemStat> findAllByStatTypeWithItem(@Param("statType") StatType statType);

    /**
     * 아이템 ID와 능력치 종류로 존재 여부 확인.
     * 속성 구분 없는 스탯(Element.NONE) 중복 저장 방지에 사용된다.
     */
    @Query("""
            SELECT COUNT(ist) > 0 FROM ItemStat ist
            WHERE ist.item.id = :itemId AND ist.statType = :statType
            """)
    boolean existsByItemIdAndStatType(@Param("itemId") Long itemId, @Param("statType") StatType statType);

    /**
     * 아이템 ID + 능력치 종류 + 속성 + 범위 조합으로 존재 여부 확인.
     * UNIQUE 제약(item_id, stat_type, element, scope) 기준 중복 저장 방지에 사용된다.
     */
    @Query("""
            SELECT COUNT(ist) > 0 FROM ItemStat ist
            WHERE ist.item.id = :itemId
              AND ist.statType = :statType
              AND ist.element = :element
              AND ist.scope = :scope
            """)
    boolean existsByItemIdAndStatTypeAndElement(
            @Param("itemId") Long itemId,
            @Param("statType") StatType statType,
            @Param("element") Element element,
            @Param("scope") BuffTarget scope);

    List<ItemStat> findByItemId(Long itemId);

    /** 아이템 ID 목록에 해당하는 스탯 일괄 조회 — 목록 페이지 N+1 방지용 */
    @Query("SELECT ist FROM ItemStat ist WHERE ist.item.id IN :itemIds")
    List<ItemStat> findByItemIdIn(@Param("itemIds") List<Long> itemIds);

    void deleteByItemId(Long itemId);
}
