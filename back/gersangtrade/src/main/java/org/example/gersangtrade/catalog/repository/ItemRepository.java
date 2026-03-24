package org.example.gersangtrade.catalog.repository;

import org.example.gersangtrade.domain.catalog.Item;
import org.example.gersangtrade.domain.catalog.enums.ItemType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 아이템 JPA 레포지토리.
 * 단순 조회는 메서드 쿼리, 복잡한 검색은 ItemQueryRepository(QueryDSL) 또는 ItemJooqRepository(jOOQ) 사용.
 */
public interface ItemRepository extends JpaRepository<Item, Long> {

    // ── 메서드 쿼리 ──────────────────────────────────────────────────────────

    /** 아이템 타입으로 조회 (MATERIAL | EQUIPMENT) */
    List<Item> findByType(ItemType type);

    /** 아이템명으로 단건 조회 — 크롤러 UPSERT 패턴에서 기존 레코드 확인에 사용된다 */
    Optional<Item> findByName(String name);

    /** imageUrl이 null인 아이템 전체 조회 — ItemDetailStep의 처리 대상 선정에 사용된다 */
    List<Item> findByImageUrlIsNull();

    // ── JPQL ────────────────────────────────────────────────────────────────

    /**
     * ID로 EquipmentItem을 fetch join하여 아이템 조회.
     * 장비 아이템 상세가 필요한 경우 N+1 방지용.
     *
     * <p>EquipmentItem은 Item과 @MapsId(1:1)로 연결되어 있으나,
     * Item 엔티티에는 EquipmentItem으로 향하는 역방향 필드가 없다.
     * 따라서 일반 JOIN FETCH 대신 JPQL에서 EquipmentItem을 기준으로 OUTER JOIN한다.
     */
    @Query("""
            SELECT i FROM Item i
            LEFT JOIN EquipmentItem ei ON ei.item = i
            WHERE i.id = :id
            """)
    Optional<Item> findByIdWithEquipmentItem(@Param("id") Long id);

    /**
     * ID 목록으로 아이템 일괄 조회.
     * 매물 등록 시 유효성 검사에 사용된다.
     */
    @Query("SELECT i FROM Item i WHERE i.id IN :ids")
    List<Item> findAllByIds(@Param("ids") List<Long> ids);
}
