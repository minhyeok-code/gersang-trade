package org.example.gersangtrade.deck.repository;

import org.example.gersangtrade.domain.deck.UserDeckMemberSlotRitual;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserDeckMemberSlotRitualRepository extends JpaRepository<UserDeckMemberSlotRitual, Long> {

    Optional<UserDeckMemberSlotRitual> findByDeckMemberSlotId(Long deckMemberSlotId);
}
