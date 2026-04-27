package org.example.gersangtrade.domain.deck;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.gersangtrade.domain.catalog.EquipmentItem;
import org.example.gersangtrade.domain.catalog.EquipmentSet;

/**
 * 덱 슬롯 용병 장비 상세 엔티티.
 * UserDeckMember와 1:1 관계이며, 카테고리별로 다른 필드를 사용한다.
 *
 * <p>카테고리별 사용 필드:
 * <ul>
 *   <li>LEGENDARY_GENERAL: equipmentSet, enhanceLevel, setPieceCount, hasAffinity</li>
 *   <li>MYEONG_KING / MYEONG_KING_AWAKENING: equipmentItem</li>
 *   <li>그 외 카테고리: 장비 정보 없음 (이 엔티티 행 미생성)</li>
 * </ul>
 */
@Entity
@Table(name = "user_deck_member_equips")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserDeckMemberEquip {

    /** 장비 상세 고유 식별자 (자동 증가) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 장비 정보를 보유하는 슬롯 (1:1) */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deck_member_id", nullable = false, unique = true)
    private UserDeckMember deckMember;

    /**
     * 전설장수 세트 — LEGENDARY_GENERAL 전용.
     * 다른 카테고리는 null.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_set_id")
    private EquipmentSet equipmentSet;

    /**
     * 전설장수 세트 강화 수치 — LEGENDARY_GENERAL 전용.
     * null이면 강화 없음.
     */
    @Column(name = "enhance_level")
    private Integer enhanceLevel;

    /**
     * 전설장수 세트 피스 수 — LEGENDARY_GENERAL 전용.
     * null이면 피스 수 미지정.
     */
    @Column(name = "set_piece_count")
    private Integer setPieceCount;

    /**
     * 전설장수 인연 여부 — LEGENDARY_GENERAL 전용.
     * 인연 효과 적용 시 true.
     */
    @Column(name = "has_affinity", nullable = false)
    private boolean hasAffinity = false;

    /**
     * 명왕 개별 장비 — MYEONG_KING / MYEONG_KING_AWAKENING 전용.
     * 다른 카테고리는 null.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_item_id")
    private EquipmentItem equipmentItem;

    @Builder
    public UserDeckMemberEquip(UserDeckMember deckMember,
                               EquipmentSet equipmentSet, Integer enhanceLevel,
                               Integer setPieceCount, boolean hasAffinity,
                               EquipmentItem equipmentItem) {
        this.deckMember = deckMember;
        this.equipmentSet = equipmentSet;
        this.enhanceLevel = enhanceLevel;
        this.setPieceCount = setPieceCount;
        this.hasAffinity = hasAffinity;
        this.equipmentItem = equipmentItem;
    }
}
