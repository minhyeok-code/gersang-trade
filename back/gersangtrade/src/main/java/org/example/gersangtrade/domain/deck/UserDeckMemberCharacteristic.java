package org.example.gersangtrade.domain.deck;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.gersangtrade.domain.catalog.MercenaryCharacteristic;

/**
 * 덱 슬롯 용병의 선택된 특성 엔티티.
 * 전설장수 패시브도 MercenaryCharacteristic으로 통합되어 이 엔티티로 관리된다.
 *
 * <p>selectedLevel: 유저가 해당 특성에 투자한 레벨.
 * 각성 사천왕·명왕·주인공은 1~5, 전설장수는 1~10.
 */
@Entity
@Table(
        name = "user_deck_member_characteristics",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_user_deck_member_characteristics",
                columnNames = {"deck_member_id", "characteristic_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserDeckMemberCharacteristic {

    /** 선택 특성 고유 식별자 (자동 증가) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 특성을 선택한 슬롯 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deck_member_id", nullable = false)
    private UserDeckMember deckMember;

    /** 선택된 특성 정의 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "characteristic_id", nullable = false)
    private MercenaryCharacteristic characteristic;

    /**
     * 선택한 레벨.
     * calculateTotalStats에서 이 레벨에 해당하는 MercenaryCharacteristicLevel을 조회해 합산한다.
     */
    @Column(name = "selected_level", nullable = false)
    private Integer selectedLevel;

    @Builder
    public UserDeckMemberCharacteristic(UserDeckMember deckMember,
                                        MercenaryCharacteristic characteristic,
                                        Integer selectedLevel) {
        this.deckMember = deckMember;
        this.characteristic = characteristic;
        this.selectedLevel = selectedLevel;
    }
}
