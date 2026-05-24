package org.example.gersangtrade.deck.repository;

import org.example.gersangtrade.domain.deck.UserDeckMemberEquip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserDeckMemberEquipRepository extends JpaRepository<UserDeckMemberEquip, Long> {

    /** 용병 제거 시 카테고리별 장비 상세 선삭제 — FK 제약 회피 */
    @Modifying
    @Query("DELETE FROM UserDeckMemberEquip e WHERE e.deckMember.id = :deckMemberId")
    void deleteByDeckMemberId(@Param("deckMemberId") Long deckMemberId);
}
