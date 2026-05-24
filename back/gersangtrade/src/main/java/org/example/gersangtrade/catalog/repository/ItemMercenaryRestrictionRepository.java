package org.example.gersangtrade.catalog.repository;

import org.example.gersangtrade.domain.catalog.ItemMercenaryRestriction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ItemMercenaryRestrictionRepository extends JpaRepository<ItemMercenaryRestriction, Long> {

    /**
     * 아이템 착용 제한 목록 조회.
     * 결과가 비어 있으면 공용 아이템, 있으면 조건 일치 용병만 착용 가능.
     */
    @Query("SELECT r FROM ItemMercenaryRestriction r " +
           "LEFT JOIN FETCH r.mercenary " +
           "WHERE r.item.id = :itemId")
    List<ItemMercenaryRestriction> findByItemId(@Param("itemId") Long itemId);
}
