package org.example.gersangtrade.catalog.repository;

import org.example.gersangtrade.domain.catalog.MercenaryCharacteristic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MercenaryCharacteristicRepository extends JpaRepository<MercenaryCharacteristic, Long> {

    /** 용병별 전체 특성 조회 */
    List<MercenaryCharacteristic> findByMercenaryId(Long mercenaryId);

    /** 용병 ID 목록으로 특성 일괄 조회 — 관리자 목록 페이지 N+1 방지 */
    @Query("SELECT c FROM MercenaryCharacteristic c JOIN FETCH c.mercenary WHERE c.mercenary.id IN :ids")
    List<MercenaryCharacteristic> findByMercenaryIdIn(@Param("ids") Collection<Long> ids);

    /** gerniverse 키로 특성 조회 (UPSERT 기준) */
    Optional<MercenaryCharacteristic> findByKey(String key);

    /** 크롤링 재적재 시 용병 특성 전체 삭제 */
    void deleteByMercenaryId(Long mercenaryId);
}
