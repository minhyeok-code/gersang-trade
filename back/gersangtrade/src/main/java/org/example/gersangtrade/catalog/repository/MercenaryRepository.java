package org.example.gersangtrade.catalog.repository;

import org.example.gersangtrade.domain.catalog.Mercenary;
import org.springframework.data.jpa.repository.JpaRepository;

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

    /**
     * 상세 크롤링이 완료되지 않은 용병 목록 조회 (crawledAt IS NULL).
     * DetailReader가 처리 대상을 선정하는 데 사용된다.
     */
    List<Mercenary> findByCrawledAtIsNull();
}
