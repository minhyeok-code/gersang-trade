package org.example.gersangtrade.catalog.repository;

import org.example.gersangtrade.domain.catalog.MercenaryMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 용병 전직 재료 JPA 레포지토리.
 * 가성비 계산기에서 용병별 재료 목록 조회 시 사용된다.
 */
public interface MercenaryMaterialRepository extends JpaRepository<MercenaryMaterial, Long> {

    /**
     * 완성 용병 ID로 전직 재료 목록 조회 (materialMercenary fetch join 포함).
     * 재료가 용병인 경우 N+1 방지용.
     */
    @Query("""
            SELECT mm FROM MercenaryMaterial mm
            LEFT JOIN FETCH mm.materialMercenary
            WHERE mm.resultMercenary.id = :resultMercenaryId
            """)
    List<MercenaryMaterial> findByResultMercenaryId(
            @Param("resultMercenaryId") Long resultMercenaryId);

    /**
     * 완성 용병 ID 목록으로 재료 일괄 조회.
     * 가성비 계산기에서 용병 고용 총비용 산출 시 N+1 방지용.
     */
    @Query("""
            SELECT mm FROM MercenaryMaterial mm
            LEFT JOIN FETCH mm.materialMercenary
            JOIN FETCH mm.resultMercenary
            WHERE mm.resultMercenary.id IN :resultMercenaryIds
            """)
    List<MercenaryMaterial> findAllByResultMercenaryIdIn(
            @Param("resultMercenaryIds") List<Long> resultMercenaryIds);

    /**
     * 완성 용병 ID에 해당하는 재료 전체 삭제.
     * 크롤러 재파싱 시 재료 목록 초기화 후 재적재에 사용된다.
     */
    void deleteByResultMercenaryId(Long resultMercenaryId);
}
