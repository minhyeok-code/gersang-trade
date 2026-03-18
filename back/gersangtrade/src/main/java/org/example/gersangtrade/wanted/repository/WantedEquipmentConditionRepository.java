package org.example.gersangtrade.wanted.repository;

import org.example.gersangtrade.domain.wanted.WantedEquipmentCondition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 구매 희망 장비 조건 기본 CRUD 레포지토리.
 * WantedEquipmentCondition은 WantedItem과 PK를 공유(@MapsId)한다.
 */
public interface WantedEquipmentConditionRepository extends JpaRepository<WantedEquipmentCondition, Long> {

    /**
     * 여러 WantedItem ID에 해당하는 장비 조건 목록 일괄 조회 (N+1 방지).
     */
    List<WantedEquipmentCondition> findByWantedItemIdIn(List<Long> wantedItemIds);
}
