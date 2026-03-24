package org.example.gersangtrade.catalog.repository;

import org.example.gersangtrade.domain.catalog.Gem;
import org.example.gersangtrade.domain.catalog.enums.GemGrade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 보석 JPA 레포지토리.
 * 보석 목록은 Flyway 시드 데이터로 적재되며, 크롤러 Batch Job이 imageUrl 갱신 시 사용한다.
 */
public interface GemRepository extends JpaRepository<Gem, Long> {

    /**
     * 이름·등급·주술 조합으로 보석 조회 — UPSERT 패턴에서 기존 레코드 확인에 사용된다.
     * 주술됨 등급이 아닌 경우 ritualId는 null로 전달한다.
     */
    @Query("""
            SELECT g FROM Gem g
            LEFT JOIN g.ritual r
            WHERE g.name = :name
              AND g.gemGrade = :gemGrade
              AND (:ritualId IS NULL AND g.ritual IS NULL
                   OR r.id = :ritualId)
            """)
    Optional<Gem> findByNameAndGemGradeAndRitualId(
            @Param("name") String name,
            @Param("gemGrade") GemGrade gemGrade,
            @Param("ritualId") Long ritualId);

    /** 이미지 URL이 아직 없는 보석 목록 조회 — 이미지 수집 Batch Job에서 대상 선정에 사용된다 */
    List<Gem> findByImageUrlIsNull();
}
