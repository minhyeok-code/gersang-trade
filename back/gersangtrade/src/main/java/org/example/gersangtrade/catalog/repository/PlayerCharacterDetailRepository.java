package org.example.gersangtrade.catalog.repository;

import org.example.gersangtrade.domain.catalog.PlayerCharacterDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlayerCharacterDetailRepository extends JpaRepository<PlayerCharacterDetail, Long> {

    Optional<PlayerCharacterDetail> findByMercenaryId(Long mercenaryId);
}
