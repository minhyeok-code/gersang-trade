package org.example.gersangtrade.domain.deck;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.gersangtrade.calculator.dto.request.BonusStatTarget;
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

    /** 용병 캐릭터 레벨 (250 또는 260). 특성 포인트·레벨 스탯 계산 기준. */
    @Column(name = "level", nullable = false)
    private int level = 250;

    /** 보너스 스탯 투자 대상 (주스탯 / 생명력). DPS 계산에 사용. */
    @Enumerated(EnumType.STRING)
    @Column(name = "bonus_target", nullable = false, length = 20)
    private BonusStatTarget bonusTarget = BonusStatTarget.MAIN_STAT;

    /** 보너스 스탯 총량 (300/500/700/900/1000 등). */
    @Column(name = "bonus_amount", nullable = false)
    private int bonusAmount = 0;

    @Builder
    public UserDeckMember(UserDeck deck, Mercenary mercenary) {
        this.deck = deck;
        this.mercenary = mercenary;
        this.level = 250;
        this.bonusTarget = BonusStatTarget.MAIN_STAT;
        this.bonusAmount = 0;
    }

    public void updateLevel(int level) {
        this.level = level;
    }

    /** 레벨·보너스 스탯 빌드 설정 일괄 저장 */
    public void updateBuild(int level, BonusStatTarget bonusTarget, int bonusAmount) {
        this.level = level;
        this.bonusTarget = bonusTarget;
        this.bonusAmount = bonusAmount;
    }
}
