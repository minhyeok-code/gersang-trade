package org.example.gersangtrade.hunt.repository;

import org.example.gersangtrade.domain.hunt.DeckSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeckSnapshotRepository extends JpaRepository<DeckSnapshot, Long> {

    Optional<DeckSnapshot> findByContentHash(String contentHash);
}
