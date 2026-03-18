package org.example.gersangtrade.domain.wanted;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.gersangtrade.domain.catalog.Ritual;
import org.example.gersangtrade.domain.wanted.enums.PreferredOutcome;

/**
 * 구매 희망 주술 조건 엔티티.
 * 구매자가 원하는 주술 종류와 선호 결과(성공/대성공/무관)를 정의한다.
 * 동일 아이템에 동일 주술이 중복 등록되는 것을 방지하기 위해 UNIQUE 제약이 적용된다.
 *
 * <p>판매 등록의 BundleEquipmentRitual(확정된 결과 스냅샷)과 달리,
 * 여기서는 수락 가능한 결과 조건(ANY / SUCCESS / GREAT_SUCCESS)을 저장한다.
 */
@Entity
@Table(
        name = "wanted_ritual_conditions",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_wanted_ritual_conditions_item_ritual",
                columnNames = {"wanted_item_id", "ritual_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WantedRitualCondition {

    /** 구매 희망 주술 조건 고유 식별자 (자동 증가) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 이 조건이 속하는 구매 희망 아이템 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wanted_item_id", nullable = false)
    private WantedItem wantedItem;

    /** 원하는 주술 종류 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ritual_id", nullable = false)
    private Ritual ritual;

    /**
     * 허용 가능한 주술 결과.
     * ANY: 성공/대성공 모두 수락.
     * SUCCESS: 성공 이상 수락.
     * GREAT_SUCCESS: 대성공만 수락.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_outcome", nullable = false, length = 20)
    private PreferredOutcome preferredOutcome;

    @Builder
    public WantedRitualCondition(WantedItem wantedItem, Ritual ritual,
                                  PreferredOutcome preferredOutcome) {
        this.wantedItem = wantedItem;
        this.ritual = ritual;
        this.preferredOutcome = preferredOutcome;
    }
}
