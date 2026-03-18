package org.example.gersangtrade.domain.catalog;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.gersangtrade.domain.catalog.enums.ItemType;
import org.example.gersangtrade.domain.common.BaseEntity;

/**
 * 아이템 카탈로그 기반 엔티티.
 * 재료(MATERIAL)와 장비(EQUIPMENT) 모두 이 엔티티를 부모로 가진다.
 * 상세 정보는 MaterialItem 또는 EquipmentItem 에서 @MapsId 패턴으로 확장된다.
 * Flyway 시드 데이터로 관리되며, 애플리케이션에서 직접 생성하지 않는다.
 */
@Entity
@Table(name = "items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Item extends BaseEntity {

    /** 아이템 고유 식별자 (자동 증가) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 표준 아이템명 (게임 내 명칭 기준) */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /** 아이템 종류 — MATERIAL: 재료, EQUIPMENT: 장비 */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private ItemType type;

    /** 거래 카테고리 — 추후 분류 확장용 (nullable) */
    @Column(name = "trade_category", length = 50)
    private String tradeCategory;

    @Builder
    public Item(String name, ItemType type, String tradeCategory) {
        this.name = name;
        this.type = type;
        this.tradeCategory = tradeCategory;
    }
}
