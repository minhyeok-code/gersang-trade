package org.example.gersangtrade.catalog.repository;

import org.example.gersangtrade.domain.catalog.Mercenary;
import org.example.gersangtrade.domain.catalog.enums.MercenaryCategory;
import org.example.gersangtrade.domain.catalog.enums.Nation;
import org.example.gersangtrade.domain.catalog.enums.Nature;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 용병 JPA 레포지토리.
 * gerniverse 용병 목록·상세 크롤링(Job 1) 결과를 저장한다.
 */
public interface MercenaryRepository extends JpaRepository<Mercenary, Long> {

    /**
     * 용병명으로 조회 — UPSERT 패턴에서 기존 레코드 확인에 사용된다.
     * name은 UNIQUE 제약이 있으므로 Optional 반환.
     */
    Optional<Mercenary> findByName(String name);

    /** mercenary_key로 단건 조회 — 거니버스 스킬 계수 적재 시 용병 FK 조회에 사용된다 */
    Optional<Mercenary> findByKey(String key);

    /**
     * 상세 크롤링이 완료되지 않은 용병 목록 조회 (crawledAt IS NULL).
     * DetailReader가 처리 대상을 선정하는 데 사용된다.
     */
    List<Mercenary> findByCrawledAtIsNull();

    /**
     * 관리자 목록: category + nature + nation 복합 필터 — 페이징.
     * null 파라미터는 조건에서 제외된다.
     */
    @Query("""
            SELECT m FROM Mercenary m
            WHERE (:category IS NULL OR m.category = :category)
              AND (:nature IS NULL OR m.nature = :nature)
              AND (:nation IS NULL OR m.nation = :nation)
            ORDER BY m.name
            """)
    Page<Mercenary> findByFilters(@Param("category") MercenaryCategory category,
                                  @Param("nature") Nature nature,
                                  @Param("nation") Nation nation,
                                  Pageable pageable);

    /**
     * ID 목록에 해당하는 용병의 nature를 일괄 변경.
     * 벌크 update이므로 영속성 컨텍스트 동기화를 위해 @Modifying(clearAutomatically = true) 사용.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Mercenary m SET m.nature = :nature WHERE m.id IN :ids")
    int bulkUpdateNature(@Param("ids") List<Long> ids, @Param("nature") Nature nature);

    /**
     * ID 목록에 해당하는 용병의 nation을 일괄 변경.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Mercenary m SET m.nation = :nation WHERE m.id IN :ids")
    int bulkUpdateNation(@Param("ids") List<Long> ids, @Param("nation") Nation nation);
}
