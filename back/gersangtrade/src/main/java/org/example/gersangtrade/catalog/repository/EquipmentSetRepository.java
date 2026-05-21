package org.example.gersangtrade.catalog.repository;

import org.example.gersangtrade.domain.catalog.EquipmentSet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** 장비 세트 레포지토리 */
public interface EquipmentSetRepository extends JpaRepository<EquipmentSet, Long> {

    Optional<EquipmentSet> findByName(String name);

    Page<EquipmentSet> findByNameContaining(String name, Pageable pageable);

    /** 거래 노출 세트 전체 목록 페이징 (유저용) */
    Page<EquipmentSet> findByIsTradeableTrue(Pageable pageable);

    /** 거래 노출 세트 이름 검색 페이징 (유저용) */
    Page<EquipmentSet> findByNameContainingAndIsTradeableTrue(String name, Pageable pageable);
}
