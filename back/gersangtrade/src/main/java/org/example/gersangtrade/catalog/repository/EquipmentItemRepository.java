package org.example.gersangtrade.catalog.repository;

import org.example.gersangtrade.domain.catalog.EquipmentItem;
import org.example.gersangtrade.domain.catalog.enums.EquipmentSlot;
import org.example.gersangtrade.domain.deck.enums.EquipSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 장비 아이템 상세 정보 레포지토리.
 * EquipmentItem은 Item과 PK를 공유(@MapsId)하므로 itemId로 조회한다.
 */
public interface EquipmentItemRepository extends JpaRepository<EquipmentItem, Long> {

    /**
     * 아이템 ID로 장비 상세 정보 조회.
     * 매물 등록 시 장비 유효성 검사 및 스냅샷 저장에 사용된다.
     *
     * @param itemId Item.id (= EquipmentItem.itemId)
     */
    @Query("""
            SELECT ei FROM EquipmentItem ei
            JOIN FETCH ei.item
            WHERE ei.itemId = :itemId
            """)
    Optional<EquipmentItem> findWithItemByItemId(@Param("itemId") Long itemId);

    /**
     * 아이템 ID로 장비 상세 정보 존재 여부 확인.
     * 매물 등록 시 장비 타입 검증에 사용된다.
     */
    boolean existsByItemId(Long itemId);

    /**
     * 아이템명 + 슬롯으로 장비 상세 정보 조회.
     * 크롤링 upsert 기준 키로 사용 (name + slot 조합이 유일).
     */
    Optional<EquipmentItem> findByItem_NameAndSlot(String name, EquipmentSlot slot);

    /**
     * 아이템명 prefix + 슬롯으로 장비 목록 조회.
     * 세트-피스 자동 매핑에 사용 — "세트명%"로 아이템을 찾아 슬롯으로 범위를 좁힌다.
     */
    @Query("""
            SELECT ei FROM EquipmentItem ei
            JOIN ei.item i
            WHERE i.name LIKE :namePrefix%
              AND ei.slot = :slot
              AND ei.equipmentSet IS NULL
            """)
    List<EquipmentItem> findUnlinkedByNamePrefixAndSlot(
            @Param("namePrefix") String namePrefix,
            @Param("slot") EquipmentSlot slot);

    /**
     * 아이템명으로 장비 상세 정보 조회.
     * 주술 적용 가능 아이템 파싱 시 단품 아이템명(예: "태황반지")으로 EquipmentItem을 찾을 때 사용.
     */
    @Query("""
            SELECT ei FROM EquipmentItem ei
            JOIN FETCH ei.item i
            WHERE i.name = :name
            """)
    Optional<EquipmentItem> findByItemName(@Param("name") String name);

    /**
     * 덱 슬롯(EquipSlot)으로 착용 가능한 장비 목록 조회.
     * APP_* 슬롯(외변)과 일반 슬롯 모두 equipSlot 필드로 조회한다.
     * RING_1/RING_2는 EquipmentSlot.RING으로 별도 조회해야 한다.
     */
    @Query("""
            SELECT ei FROM EquipmentItem ei
            JOIN FETCH ei.item
            LEFT JOIN FETCH ei.equipmentSet
            LEFT JOIN FETCH ei.mercenary
            WHERE ei.equipSlot = :slot
            """)
    List<EquipmentItem> findByEquipSlot(@Param("slot") EquipSlot slot);

    /**
     * EquipmentSlot으로 장비 목록 조회 (Item fetch join 포함).
     * RING_1/RING_2 슬롯에서 반지 아이템 목록 조회 시 사용한다.
     */
    @Query("""
            SELECT ei FROM EquipmentItem ei
            JOIN FETCH ei.item
            LEFT JOIN FETCH ei.equipmentSet
            LEFT JOIN FETCH ei.mercenary
            WHERE ei.slot = :slot
            """)
    List<EquipmentItem> findBySlotWithItem(@Param("slot") EquipmentSlot slot);

    /** 전체 장비 목록 조회 (덱 설정 페이지 초기 로딩용) */
    @Query("""
            SELECT ei FROM EquipmentItem ei
            JOIN FETCH ei.item
            LEFT JOIN FETCH ei.equipmentSet
            LEFT JOIN FETCH ei.mercenary
            """)
    List<EquipmentItem> findAllWithItem();

    /** 세트 ID로 소속 장비 목록 조회 — equipment_set_pieces가 없을 때 일괄 장착 fallback */
    @Query("""
            SELECT ei FROM EquipmentItem ei
            JOIN FETCH ei.item
            LEFT JOIN FETCH ei.equipmentSet
            WHERE ei.equipmentSet.id = :setId
            """)
    List<EquipmentItem> findBySetIdWithItem(@Param("setId") Long setId);

    /** 전용 용병 FK 또는 item_mercenary_restrictions 대상 장비 일괄 조회 */
    @Query("""
            SELECT DISTINCT ei FROM EquipmentItem ei
            JOIN FETCH ei.item i
            LEFT JOIN FETCH ei.equipmentSet
            LEFT JOIN FETCH ei.mercenary
            LEFT JOIN ItemMercenaryRestriction r ON r.item = i AND r.mercenary.id = :mercenaryId
            WHERE ei.mercenary.id = :mercenaryId OR r.id IS NOT NULL
            """)
    List<EquipmentItem> findExclusiveEquipmentByMercenaryId(@Param("mercenaryId") Long mercenaryId);
}
