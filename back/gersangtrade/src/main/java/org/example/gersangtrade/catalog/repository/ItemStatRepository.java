package org.example.gersangtrade.catalog.repository;

import org.example.gersangtrade.domain.catalog.ItemStat;
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
     * 크롤러 배치에서 중복 저장 방지에 사용된다.
     */
    @Query("""
            SELECT COUNT(ist) > 0 FROM ItemStat ist
            WHERE ist.item.id = :itemId AND ist.statType = :statType
            """)
    boolean existsByItemIdAndStatType(@Param("itemId") Long itemId, @Param("statType") StatType statType);
}
