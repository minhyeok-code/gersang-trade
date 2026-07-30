package org.example.gersangtrade.catalog.repository;

import org.example.gersangtrade.domain.catalog.Monster;
import org.example.gersangtrade.domain.catalog.enums.Element;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MonsterRepository extends JpaRepository<Monster, Long> {

    Optional<Monster> findByName(String name);

    /** 공개 전체 목록 — hidden=false만 반환 */
    List<Monster> findByHiddenFalse();

    /** 공개 속성 필터 목록 — hidden=false만 반환 */
    List<Monster> findByElementAndHiddenFalse(Element element);

    /** 공개 이름 자동완성 — hidden=false만 검색 */
    @Query("SELECT m FROM Monster m WHERE m.hidden = false AND m.name LIKE %:q%")
    List<Monster> findVisibleByNameContaining(@Param("q") String q, Pageable pageable);

    /** 관리자 이름 부분 검색 (페이징) — hidden 포함 전체 */
    @Query("SELECT m FROM Monster m WHERE m.name LIKE %:name%")
    Page<Monster> searchByName(@Param("name") String name, Pageable pageable);
}
