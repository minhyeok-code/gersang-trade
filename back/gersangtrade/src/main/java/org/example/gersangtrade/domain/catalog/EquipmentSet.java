package org.example.gersangtrade.domain.catalog;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 장비 세트 정의 엔티티.
 * 거상의 세트 장비 그룹(예: 00세트, XX세트)을 정의한다.
 * 세트에 속하는 피스(EquipmentSetPiece)들을 통해 실제 구성 아이템이 연결된다.
 */
@Entity
@Table(name = "equipment_sets")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EquipmentSet {

    /** 장비 세트 고유 식별자 (자동 증가) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 세트 명칭 — 예: "00세트", "XX세트" */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * 세트 유형 분류.
     * 현재는 "ARMOR_5PIECE" 등의 문자열로 관리, 추후 Enum 전환 가능.
     */
    @Column(name = "set_type", nullable = false, length = 20)
    private String setType;

    @Builder
    public EquipmentSet(String name, String setType) {
        this.name = name;
        this.setType = setType;
    }
}
