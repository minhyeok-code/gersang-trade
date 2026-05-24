package org.example.gersangtrade.deck.repository;

import org.example.gersangtrade.domain.deck.UserDeckMemberCharacteristic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserDeckMemberCharacteristicRepository extends JpaRepository<UserDeckMemberCharacteristic, Long> {

    /** 멤버 ID 목록으로 선택 특성 일괄 조회 — DPS 계산기 배치 로딩용 */
    @Query("""
            SELECT c FROM UserDeckMemberCharacteristic c
            JOIN FETCH c.characteristic
            WHERE c.deckMember.id IN :memberIds
            """)
    List<UserDeckMemberCharacteristic> findByDeckMemberIdIn(@Param("memberIds") List<Long> memberIds);
}
