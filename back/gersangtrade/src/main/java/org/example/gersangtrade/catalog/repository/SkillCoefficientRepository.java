package org.example.gersangtrade.catalog.repository;

import org.example.gersangtrade.domain.catalog.SkillCoefficient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SkillCoefficientRepository extends JpaRepository<SkillCoefficient, Long> {

    /** row_id로 단건 조회 — upsert 패턴에서 기존 레코드 확인에 사용된다 */
    Optional<SkillCoefficient> findByRowId(String rowId);

    boolean existsByRowId(String rowId);

    /**
     * 전체 목록을 skill owner까지 fetch join하여 조회 — 관리자 목록 N+1 방지.
     * mercenarySkill / itemSkill은 상호 배타적이므로 LEFT JOIN FETCH 두 개를 써도 카테시안 곱 없음.
     */
    @org.springframework.data.jpa.repository.Query("""
            SELECT sc FROM SkillCoefficient sc
            LEFT JOIN FETCH sc.mercenarySkill ms
            LEFT JOIN FETCH ms.mercenary
            LEFT JOIN FETCH sc.itemSkill isk
            LEFT JOIN FETCH isk.item
            """)
    java.util.List<SkillCoefficient> findAllWithOwners();

    /** 미측정 목록 (castsPerSecond IS NULL AND tickIntervalMs IS NULL) — fetch join 포함 */
    @org.springframework.data.jpa.repository.Query("""
            SELECT sc FROM SkillCoefficient sc
            LEFT JOIN FETCH sc.mercenarySkill ms
            LEFT JOIN FETCH ms.mercenary
            LEFT JOIN FETCH sc.itemSkill isk
            LEFT JOIN FETCH isk.item
            WHERE sc.castsPerSecond IS NULL AND sc.tickIntervalMs IS NULL
            """)
    java.util.List<SkillCoefficient> findUnmeasuredWithOwners();

    /** 용병 ID 목록으로 스킬 계수 일괄 조회 — DPS 계산기 배치 로딩용 */
    @org.springframework.data.jpa.repository.Query("""
            SELECT sc FROM SkillCoefficient sc
            JOIN FETCH sc.mercenarySkill ms
            WHERE ms.mercenary.id IN :mercenaryIds
            """)
    java.util.List<SkillCoefficient> findByMercenaryIdIn(
            @org.springframework.data.repository.query.Param("mercenaryIds") java.util.List<Long> mercenaryIds);

    /** 아이템 ID 목록으로 아이템 스킬 계수 일괄 조회 — DPS 계산기 배치 로딩용 */
    @org.springframework.data.jpa.repository.Query("""
            SELECT sc FROM SkillCoefficient sc
            JOIN FETCH sc.itemSkill isk
            JOIN FETCH isk.item
            WHERE isk.item.id IN :itemIds
            """)
    java.util.List<SkillCoefficient> findByItemIdIn(
            @org.springframework.data.repository.query.Param("itemIds") java.util.List<Long> itemIds);
}
