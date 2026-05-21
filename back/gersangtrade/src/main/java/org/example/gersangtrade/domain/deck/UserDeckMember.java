package org.example.gersangtrade.domain.deck;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.gersangtrade.domain.catalog.Mercenary;

import java.util.ArrayList;
import java.util.List;

/**
 * 덱 내 용병 엔티티.
 * 하나의 UserDeck은 최대 12개의 UserDeckMember를 가진다.
 * 동일 덱 내 같은 용병은 중복 불가.
 * 주인공(PROTAGONIST)도 Mercenary로 통일해 이 엔티티로 관리한다.
 */
@Entity
@Table(
        name = "user_deck_members",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_user_deck_members_deck_mercenary",
                columnNames = {"deck_id", "mercenary_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserDeckMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deck_id", nullable = false)
    private UserDeck deck;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mercenary_id", nullable = false)
    private Mercenary mercenary;

    @OneToMany(mappedBy = "deckMember", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserDeckMemberSlot> slots = new ArrayList<>();

    @Builder
    public UserDeckMember(UserDeck deck, Mercenary mercenary) {
        this.deck = deck;
        this.mercenary = mercenary;
    }
}
