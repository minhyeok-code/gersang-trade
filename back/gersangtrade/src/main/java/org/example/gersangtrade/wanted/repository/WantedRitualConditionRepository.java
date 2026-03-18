package org.example.gersangtrade.wanted.repository;

import org.example.gersangtrade.domain.wanted.WantedRitualCondition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 구매 희망 주술 조건 기본 CRUD 레포지토리.
 */
public interface WantedRitualConditionRepository extends JpaRepository<WantedRitualCondition, Long> {

    /**
     * 여러 WantedItem ID에 해당하는 주술 조건 목록 일괄 조회 (N+1 방지).
     * Ritual 엔티티를 fetch join으로 함께 조회한다.
     */
    @Query("SELECT wrc FROM WantedRitualCondition wrc " +
           "JOIN FETCH wrc.ritual " +
           "WHERE wrc.wantedItem.id IN :wantedItemIds")
    List<WantedRitualCondition> findWithRitualByWantedItemIdIn(
            @Param("wantedItemIds") List<Long> wantedItemIds);
}
