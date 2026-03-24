package org.example.gersangtrade.catalog.repository;

import org.example.gersangtrade.domain.catalog.Ritual;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 주술 정의 JPA 레포지토리.
 * 크롤러에서 보석/장비의 주술명으로 Ritual 엔티티를 조회할 때 사용된다.
 */
public interface RitualRepository extends JpaRepository<Ritual, Long> {

    /**
     * 표시명(displayName)으로 주술 조회.
     * geota 아이템명의 <주술명> 부분과 매핑한다. 예: "태산북두" → Ritual
     */
    Optional<Ritual> findByDisplayName(String displayName);
}
