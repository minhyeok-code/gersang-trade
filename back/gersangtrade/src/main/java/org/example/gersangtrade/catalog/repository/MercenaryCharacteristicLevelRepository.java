package org.example.gersangtrade.catalog.repository;

import org.example.gersangtrade.domain.catalog.MercenaryCharacteristicLevel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MercenaryCharacteristicLevelRepository extends JpaRepository<MercenaryCharacteristicLevel, Long> {

    /** 특성의 특정 label × level 조회 (calculateTotalStats에서 합산 시 사용) */
    Optional<MercenaryCharacteristicLevel> findByCharacteristicIdAndLabelAndLevel(
            Long characteristicId, String label, Integer level);

    /** 특성의 전체 레벨 수치 조회 */
    List<MercenaryCharacteristicLevel> findByCharacteristicId(Long characteristicId);

    /** 크롤링 재적재 시 특성 레벨 전체 삭제 */
    void deleteByCharacteristicId(Long characteristicId);

    /** 특성 ID 목록으로 전체 레벨 수치 일괄 조회 — DPS 계산기 배치 로딩용 */
    @org.springframework.data.jpa.repository.Query("""
            SELECT l FROM MercenaryCharacteristicLevel l
            JOIN FETCH l.characteristic
            WHERE l.characteristic.id IN :characteristicIds
            """)
    List<MercenaryCharacteristicLevel> findByCharacteristicIdIn(
            @org.springframework.data.repository.query.Param("characteristicIds") List<Long> characteristicIds);
}
