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

    /** 세트 명칭 — 예: "지국천왕", "각성지국천왕" */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /** 세트 최대 피스 수 — 예: 지국천왕=6, 브리트라=2 */
    @Column(name = "total_pieces", nullable = false)
    private Integer totalPieces;

    /**
     * 현재 메타 세트 여부.
     * false이면 거래 목록에서 노출 제외. 관리자가 개별 토글 가능.
     * 크롤링 시 true로 저장.
     */
    @Column(name = "is_tradeable", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean isTradeable = true;

    @Builder
    public EquipmentSet(String name, Integer totalPieces, boolean isTradeable) {
        this.name = name;
        this.totalPieces = totalPieces;
        this.isTradeable = isTradeable;
    }

    /** 크롤링 upsert 시 피스 수 갱신 — isTradeable은 관리자 관리 필드이므로 건드리지 않음 */
    public void updateTotalPieces(Integer totalPieces) {
        this.totalPieces = totalPieces;
    }

    /** 관리자 수동 수정 — 이름, 피스 수, 거래 노출 여부 */
    public void updateInfo(String name, Integer totalPieces, boolean isTradeable) {
        if (name != null && !name.isBlank()) this.name = name;
        if (totalPieces != null && totalPieces > 0) this.totalPieces = totalPieces;
        this.isTradeable = isTradeable;
    }
}
