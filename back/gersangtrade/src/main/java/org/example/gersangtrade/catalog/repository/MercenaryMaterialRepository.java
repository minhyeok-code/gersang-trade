package org.example.gersangtrade.catalog.repository;

import org.example.gersangtrade.domain.catalog.MercenaryMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 용병 고용 재료 JPA 레포지토리.
 * 가성비 계산기에서 용병별 재료 목록 조회 시 사용된다.
 */
public interface MercenaryMaterialRepository extends JpaRepository<MercenaryMaterial, Long> {

    /**
     * 용병 ID로 고용 재료 목록 조회 (Item fetch join 포함).
     * 가성비 계산기에서 재료 아이템 정보와 함께 수량을 표시하는 데 사용된다.
     */
    @Query("""
            SELECT mm FROM MercenaryMaterial mm
            JOIN FETCH mm.item
            WHERE mm.mercenary.id = :mercenaryId
            """)
    List<MercenaryMaterial> findWithItemByMercenaryId(@Param("mercenaryId") Long mercenaryId);

    /**
     * 용병 ID 목록으로 재료 일괄 조회 (Item·Mercenary fetch join 포함).
     * 가성비 계산기에서 용병 고용 총비용 산출 시 N+1 방지용으로 사용된다.
     */
    @Query("""
            SELECT mm FROM MercenaryMaterial mm
            JOIN FETCH mm.item
            JOIN FETCH mm.mercenary
            WHERE mm.mercenary.id IN :mercenaryIds
            """)
    List<MercenaryMaterial> findAllWithItemAndMercenaryByMercenaryIds(
            @Param("mercenaryIds") List<Long> mercenaryIds);

    /**
     * 용병 ID에 해당하는 재료 전체 삭제.
     * 크롤러 재파싱 시 재료 목록 초기화 후 재적재에 사용된다.
     */
    void deleteByMercenaryId(Long mercenaryId);
}
