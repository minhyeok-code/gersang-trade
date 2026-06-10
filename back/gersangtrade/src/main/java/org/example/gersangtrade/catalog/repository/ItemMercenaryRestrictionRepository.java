package org.example.gersangtrade.catalog.repository;

import org.example.gersangtrade.domain.catalog.ItemMercenaryRestriction;
import org.example.gersangtrade.domain.catalog.enums.MercenaryCategory;
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

    /** 특정 용병 전용 restriction 중복 여부 (크롤러 멱등성) */
    boolean existsByItemIdAndMercenaryId(Long itemId, Long mercenaryId);

    /** 카테고리 전용 restriction 중복 여부 (크롤러 멱등성) */
    boolean existsByItemIdAndCategory(Long itemId, MercenaryCategory category);

    /** 슬롯별 장비 목록 — 아이템별 전용 용병 restriction 일괄 조회 */
    @Query("""
            SELECT r FROM ItemMercenaryRestriction r
            JOIN FETCH r.item
            JOIN FETCH r.mercenary
            WHERE r.item.id IN :itemIds AND r.mercenary IS NOT NULL
            """)
    List<ItemMercenaryRestriction> findByItemIdInWithMercenary(@Param("itemIds") List<Long> itemIds);
}
