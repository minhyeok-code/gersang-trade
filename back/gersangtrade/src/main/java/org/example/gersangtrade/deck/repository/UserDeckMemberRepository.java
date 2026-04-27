package org.example.gersangtrade.deck.repository;

import org.example.gersangtrade.domain.deck.UserDeckMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserDeckMemberRepository extends JpaRepository<UserDeckMember, Long> {

    /**
     * 덱의 전체 슬롯 조회.
     * 합산 계산(calculateTotalStats) 시 N+1 방지를 위해 mercenary fetch join 포함.
     */
    @Query("SELECT m FROM UserDeckMember m JOIN FETCH m.mercenary WHERE m.deck.id = :deckId ORDER BY m.slotIndex")
    List<UserDeckMember> findByDeckIdWithMercenary(@Param("deckId") Long deckId);
}
