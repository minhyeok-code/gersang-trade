package org.example.gersangtrade.catalog.repository;

import org.example.gersangtrade.domain.catalog.Spirit;
import org.example.gersangtrade.domain.catalog.enums.Nature;
import org.example.gersangtrade.domain.catalog.enums.SpiritGrade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * 정령 JPA 레포지토리.
 * 시딩 시 UPSERT 키(nature+grade)로 조회하며, 계산기에서 id 목록으로 로드한다.
 */
public interface SpiritRepository extends JpaRepository<Spirit, Long> {

    /** nature + grade 조합으로 조회 — UPSERT 패턴에서 기존 레코드 확인에 사용 */
    Optional<Spirit> findByNatureAndGrade(Nature nature, SpiritGrade grade);

    /** 버프를 포함하여 조회 — 계산기에서 N+1 방지용 */
    @Query("SELECT s FROM Spirit s LEFT JOIN FETCH s.buffs WHERE s.id IN :ids")
    List<Spirit> findAllWithBuffsByIdIn(List<Long> ids);
}
