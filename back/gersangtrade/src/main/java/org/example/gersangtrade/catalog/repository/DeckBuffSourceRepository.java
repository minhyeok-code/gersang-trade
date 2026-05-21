package org.example.gersangtrade.catalog.repository;

import org.example.gersangtrade.domain.catalog.DeckBuffSource;
import org.example.gersangtrade.domain.catalog.enums.DeckBuffSourceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeckBuffSourceRepository extends JpaRepository<DeckBuffSource, Long> {

    Optional<DeckBuffSource> findBySourceTypeAndSourceId(DeckBuffSourceType sourceType, Long sourceId);

    List<DeckBuffSource> findBySourceType(DeckBuffSourceType sourceType);
}
