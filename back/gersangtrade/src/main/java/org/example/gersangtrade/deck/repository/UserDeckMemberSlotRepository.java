package org.example.gersangtrade.deck.repository;

import org.example.gersangtrade.domain.deck.UserDeckMemberSlot;
import org.example.gersangtrade.domain.deck.enums.EquipSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserDeckMemberSlotRepository extends JpaRepository<UserDeckMemberSlot, Long> {

    /** 용병의 전체 장착 슬롯 조회 (아이템 + 주술 fetch join) */
    @Query("""
            SELECT s FROM UserDeckMemberSlot s
            JOIN FETCH s.equipmentItem
            LEFT JOIN FETCH s.ritual
            WHERE s.deckMember.id = :deckMemberId
            """)
    List<UserDeckMemberSlot> findByDeckMemberIdWithDetails(@Param("deckMemberId") Long deckMemberId);

    /** 특정 슬롯 조회 */
    Optional<UserDeckMemberSlot> findByDeckMemberIdAndSlot(Long deckMemberId, EquipSlot slot);

    /** 용병 ID 목록으로 전체 슬롯 일괄 조회 — DPS 계산기 배치 로딩용 (세트 소속 포함) */
    @Query("""
            SELECT s FROM UserDeckMemberSlot s
            JOIN FETCH s.equipmentItem ei
            LEFT JOIN FETCH ei.equipmentSet
            WHERE s.deckMember.id IN :memberIds
            """)
    List<UserDeckMemberSlot> findByDeckMemberIdIn(@Param("memberIds") List<Long> memberIds);
}
