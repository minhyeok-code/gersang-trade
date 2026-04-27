package org.example.gersangtrade.domain.deck;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.gersangtrade.domain.catalog.Mercenary;

/**
 * 덱 내 용병 슬롯 엔티티.
 * 하나의 UserDeck은 최대 12개의 UserDeckMember를 가진다.
 * 주인공(PROTAGONIST)도 Mercenary로 통일해 이 엔티티로 관리한다.
 */
@Entity
@Table(
        name = "user_deck_members",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_user_deck_members_deck_slot",
                columnNames = {"deck_id", "slot_index"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserDeckMember {

    /** 슬롯 고유 식별자 (자동 증가) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 소속 덱 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deck_id", nullable = false)
    private UserDeck deck;

    /** 슬롯에 배치된 용병 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mercenary_id", nullable = false)
    private Mercenary mercenary;

    /**
     * 슬롯 위치 (0~11).
     * 같은 덱 안에서 고유하다.
     */
    @Column(name = "slot_index", nullable = false)
    private Integer slotIndex;

    @Builder
    public UserDeckMember(UserDeck deck, Mercenary mercenary, Integer slotIndex) {
        this.deck = deck;
        this.mercenary = mercenary;
        this.slotIndex = slotIndex;
    }
}
