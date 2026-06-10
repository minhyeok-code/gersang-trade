package org.example.gersangtrade.catalog.repository;

import org.example.gersangtrade.domain.catalog.Monster;
import org.example.gersangtrade.domain.catalog.enums.Element;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MonsterRepository extends JpaRepository<Monster, Long> {

    Optional<Monster> findByName(String name);

    List<Monster> findByElement(Element element);

    @Query("SELECT m FROM Monster m WHERE m.name LIKE %:q%")
    List<Monster> findByNameContaining(@Param("q") String q, Pageable pageable);
}
