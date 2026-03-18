package org.example.gersangtrade.domain.catalog;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 주술-장비 적용 가능 여부 매핑 엔티티 (M:N 조인 테이블 역할).
 * 특정 주술이 어떤 장비 아이템에 적용 가능한지를 정의한다.
 * 거래 등록 시 사용자가 장비를 선택하면, 이 테이블을 통해 적용 가능한 주술 목록을 필터링한다.
 * 동일 주술-장비 조합 중복 방지를 위해 UNIQUE 제약이 적용된다.
 */
@Entity
@Table(
        name = "ritual_applicabilities",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_ritual_applicability_ritual_equipment",
                columnNames = {"ritual_id", "equipment_item_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RitualApplicability {

    /** 주술-장비 적용 매핑 고유 식별자 (자동 증가) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 적용 가능한 주술 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ritual_id", nullable = false)
    private Ritual ritual;

    /** 주술을 적용할 수 있는 장비 아이템 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_item_id", nullable = false)
    private EquipmentItem equipmentItem;

    @Builder
    public RitualApplicability(Ritual ritual, EquipmentItem equipmentItem) {
        this.ritual = ritual;
        this.equipmentItem = equipmentItem;
    }
}
