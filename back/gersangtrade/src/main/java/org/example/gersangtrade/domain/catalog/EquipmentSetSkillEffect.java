package org.example.gersangtrade.domain.catalog;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.gersangtrade.domain.catalog.enums.Enhancement;

/**
 * 장비 세트 n종 달성 시 발동되는 스킬 효과 연결 엔티티.
 * EquipmentSet + 달성 피스 수 + 강화 단계 → SetGrantedSkill 을 매핑한다.
 *
 * <p>예: 최무선(EquipmentSet) 10강(enhancement) 7종(requiredPieces) → 천자총통:개량(SetGrantedSkill)
 */
@Entity
@Table(
        name = "equipment_set_skill_effects",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_set_skill_effect",
                columnNames = {"set_id", "required_pieces", "enhancement"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EquipmentSetSkillEffect {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 소속 장비 세트 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "set_id", nullable = false)
    private EquipmentSet equipmentSet;

    /** 스킬 발동에 필요한 착용 피스 수 */
    @Column(name = "required_pieces", nullable = false)
    private int requiredPieces;

    /**
     * 강화 단계 조건.
     * 전설장수 세트는 0강·5강·10강별로 발동 스킬이 다르다.
     * DB에는 실제 숫자(0/5/10)로 저장한다.
     */
    @Convert(converter = EnhancementConverter.class)
    @Column(name = "enhancement")
    private Enhancement enhancement;

    /** 조건 달성 시 부여되는 스킬 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "set_granted_skill_id", nullable = false)
    private SetGrantedSkill setGrantedSkill;

    @Builder
    public EquipmentSetSkillEffect(EquipmentSet equipmentSet, int requiredPieces,
                                   Enhancement enhancement, SetGrantedSkill setGrantedSkill) {
        this.equipmentSet = equipmentSet;
        this.requiredPieces = requiredPieces;
        this.enhancement = enhancement;
        this.setGrantedSkill = setGrantedSkill;
    }
}
