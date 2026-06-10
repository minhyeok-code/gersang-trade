package org.example.gersangtrade.domain.user;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.gersangtrade.domain.catalog.Monster;
import org.example.gersangtrade.domain.hunt.ClearTimeRecordStatus;
import org.example.gersangtrade.domain.hunt.DeckSnapshot;

import java.time.LocalDateTime;

/**
 * 유저 클리어타임 기록 엔티티.
 * 유저가 특정 몬스터를 클리어한 시간을 저장한다.
 * 데이터 제공 기여에 대한 보상으로 소량의 EXP가 지급된다.
 */
@Entity
@Table(name = "user_clear_times")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserClearTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "monster_id", nullable = false)
    private Monster monster;

    /** 사용한 덱 ID — 덱 삭제 시에도 기록은 유지되어야 하므로 FK 대신 단순 컬럼으로 저장 */
    @Column(name = "deck_id", nullable = false)
    private Long deckId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deck_snapshot_id", nullable = false)
    private DeckSnapshot deckSnapshot;

    /** 저장 시점 DPS 계산 총 저항깎 */
    @Column(name = "total_resist_pierce")
    private Integer totalResistPierce;

    /** 저장 시점 DPS 계산 총 속성깎 */
    @Column(name = "total_element_pierce")
    private Integer totalElementPierce;

    /** 저장 시점 서버 재계산 Raw DPS (보정 없음) */
    @Column(name = "raw_dps")
    private Long rawDps;

    /** 저장 시점 서버 재계산 Adjust DPS (속성 보정, 저항 미적용) */
    @Column(name = "adjust_dps")
    private Long adjustDps;

    /** 저장 시점 서버 재계산 Final DPS (저항 포함) */
    @Column(name = "final_dps", nullable = false)
    private Long finalDps;

    /** 저장 시점 DPS 계산 — 디버프 후 적용 저항 */
    @Column(name = "resist_after_debuff")
    private Integer resistAfterDebuff;

    /** 저장 시점 DPS 계산 — 속성깎 반영 후 몬스터 속성값 */
    @Column(name = "effective_monster_element")
    private Integer effectiveMonsterElement;

    /** 저장 시점 DPS 계산 — 저항 통과율(%) */
    @Column(name = "resist_pass_rate")
    private Double resistPassRate;

    /** 클리어 소요 시간 (초 단위) */
    @Column(name = "clear_time_seconds", nullable = false)
    private Integer clearTimeSeconds;

    /**
     * 사냥 허브 랭킹·공개 API 노출 여부.
     * {@link org.example.gersangtrade.hunt.config.HuntHubProperties#isClearTimePublicToggleEnabled()} 가 false이면
     * 저장 시 항상 true로 고정된다.
     */
    @Column(name = "is_public", nullable = false)
    private boolean isPublic = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ClearTimeRecordStatus status = ClearTimeRecordStatus.ACTIVE;

    /** 이번 저장으로 EXP가 지급되었는지 (일일 상한·중복 hash 판별용) */
    @Column(name = "exp_granted", nullable = false)
    private boolean expGranted;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    @Builder
    public UserClearTime(User user,
                         Monster monster,
                         Long deckId,
                         DeckSnapshot deckSnapshot,
                         Integer totalResistPierce,
                         Integer totalElementPierce,
                         Long rawDps,
                         Long adjustDps,
                         Long finalDps,
                         Integer resistAfterDebuff,
                         Integer effectiveMonsterElement,
                         Double resistPassRate,
                         Integer clearTimeSeconds,
                         Boolean isPublic,
                         boolean expGranted) {
        this.user = user;
        this.monster = monster;
        this.deckId = deckId;
        this.deckSnapshot = deckSnapshot;
        this.totalResistPierce = totalResistPierce;
        this.totalElementPierce = totalElementPierce;
        this.rawDps = rawDps;
        this.adjustDps = adjustDps;
        this.finalDps = finalDps;
        this.resistAfterDebuff = resistAfterDebuff;
        this.effectiveMonsterElement = effectiveMonsterElement;
        this.resistPassRate = resistPassRate;
        this.clearTimeSeconds = clearTimeSeconds;
        this.isPublic = isPublic != null ? isPublic : true;
        this.status = ClearTimeRecordStatus.ACTIVE;
        this.expGranted = expGranted;
        this.recordedAt = LocalDateTime.now();
    }
}
