package org.example.gersangtrade.domain.deck;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.gersangtrade.domain.catalog.Ritual;
import org.example.gersangtrade.domain.listing.enums.RitualOutcome;

/**
 * 덱 장비 슬롯의 주술 정보 엔티티.
 * UserDeckMemberSlot과 1:1 관계. 주술이 없으면 row가 없다.
 * 슬롯당 주술은 최대 1개.
 */
@Entity
@Table(name = "user_deck_member_slot_rituals")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserDeckMemberSlotRitual {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 주술이 적용된 슬롯 (1:1) */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deck_member_slot_id", nullable = false, unique = true)
    private UserDeckMemberSlot deckMemberSlot;

    /**
     * 주술 종류.
     * RitualApplicability 기준으로 해당 아이템에 적용 가능한 주술만 선택 가능.
     * 서비스 레이어에서 검증한다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ritual_id", nullable = false)
    private Ritual ritual;

    /**
     * 주술 강화 결과.
     * SUCCESS: 일반 성공, GREAT_SUCCESS: 대성공(북두칠성 등).
     * 유저가 UI에서 카탈로그 기반 선택지로 선택한다.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false, length = 20)
    private RitualOutcome outcome;

    public static UserDeckMemberSlotRitual of(UserDeckMemberSlot slot, Ritual ritual, RitualOutcome outcome) {
        UserDeckMemberSlotRitual r = new UserDeckMemberSlotRitual();
        r.deckMemberSlot = slot;
        r.ritual = ritual;
        r.outcome = outcome;
        return r;
    }
}
