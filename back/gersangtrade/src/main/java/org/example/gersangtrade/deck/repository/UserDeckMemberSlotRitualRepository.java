package org.example.gersangtrade.deck.repository;

import org.example.gersangtrade.domain.deck.UserDeckMemberSlotRitual;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserDeckMemberSlotRitualRepository extends JpaRepository<UserDeckMemberSlotRitual, Long> {

    Optional<UserDeckMemberSlotRitual> findByDeckMemberSlotId(Long deckMemberSlotId);

    /** 용병 제거 시 주술 선삭제 — 슬롯 삭제 전 FK 제약 회피 */
    @Modifying
    @Query("DELETE FROM UserDeckMemberSlotRitual r WHERE r.deckMemberSlot.deckMember.id = :deckMemberId")
    void deleteByDeckMemberId(@Param("deckMemberId") Long deckMemberId);
}
