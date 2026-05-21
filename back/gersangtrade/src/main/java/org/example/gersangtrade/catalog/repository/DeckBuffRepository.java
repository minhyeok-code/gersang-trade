package org.example.gersangtrade.catalog.repository;

import org.example.gersangtrade.domain.catalog.DeckBuff;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeckBuffRepository extends JpaRepository<DeckBuff, Long> {

    List<DeckBuff> findBySourceId(Long sourceId);
}
