package org.example.gersangtrade.domain.calculator;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.gersangtrade.calculator.dto.response.PriceSource;
import org.example.gersangtrade.calculator.overlay.MercenaryMode;
import org.example.gersangtrade.calculator.overlay.ScenarioItemType;
import org.example.gersangtrade.domain.catalog.Monster;
import org.example.gersangtrade.domain.catalog.Server;
import org.example.gersangtrade.domain.hunt.DeckSnapshot;
import org.example.gersangtrade.domain.user.User;

import java.time.LocalDateTime;

/**
 * DPS 가성비 평가 결과 엔티티.
 * before/after DPS 비교 + 가성비 수치를 저장한다.
 * persist=false 요청은 이 row를 생성하지 않는다.
 */
@Entity
@Table(
        name = "dps_value_evaluations",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_dps_eval_user_hash",
                columnNames = {"user_id", "evaluation_hash"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DpsValueEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 평가 기준 덱 ID — FK 없음 (덱 삭제 후에도 평가 기록 유지) */
    @Column(name = "deck_id", nullable = false)
    private Long deckId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "monster_id", nullable = false)
    private Monster monster;

    /** after 시나리오 덱 스냅샷 — persist=true 시에만 존재 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scenario_deck_snapshot_id")
    private DeckSnapshot scenarioDeckSnapshot;

    /** 평가 후보 유형 */
    @Enumerated(EnumType.STRING)
    @Column(name = "candidate_type", nullable = false, length = 20)
    private ScenarioItemType candidateType;

    /** 후보 참조 ID — itemId / setId / mercenaryId */
    @Column(name = "candidate_ref", nullable = false)
    private Long candidateRef;

    /** 용병 편성 방식 — MERCENARY 시나리오에서만 사용 */
    @Enumerated(EnumType.STRING)
    @Column(name = "mercenary_mode", length = 10)
    private MercenaryMode mercenaryMode;

    /** 변경 대상 덱 멤버 ID — MERCENARY APPEND 시 null */
    @Column(name = "affected_member_id")
    private Long affectedMemberId;

    /** 시세 조회에 사용된 서버 — ITEM_* 시나리오에서만 사용 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "server_id")
    private Server server;

    /** 평가에 사용된 가격 (전 단위) — 가성비 산출 불가 시 null */
    @Column(name = "price")
    private Long price;

    @Enumerated(EnumType.STRING)
    @Column(name = "price_source", nullable = false, length = 15)
    private PriceSource priceSource;

    /** 세트 피스별 가격 breakdown JSON (선택) */
    @Column(name = "price_json", columnDefinition = "TEXT")
    private String priceJson;

    @Column(name = "raw_dps_before", nullable = false)
    private Long rawDpsBefore;

    @Column(name = "raw_dps_after", nullable = false)
    private Long rawDpsAfter;

    @Column(name = "adjust_dps_before", nullable = false)
    private Long adjustDpsBefore;

    @Column(name = "adjust_dps_after", nullable = false)
    private Long adjustDpsAfter;

    @Column(name = "final_dps_before", nullable = false)
    private Long finalDpsBefore;

    @Column(name = "final_dps_after", nullable = false)
    private Long finalDpsAfter;

    @Column(name = "raw_dps_delta", nullable = false)
    private Long rawDpsDelta;

    @Column(name = "raw_dps_increase_rate", nullable = false)
    private Double rawDpsIncreaseRate;

    @Column(name = "adjust_dps_delta", nullable = false)
    private Long adjustDpsDelta;

    @Column(name = "adjust_dps_increase_rate", nullable = false)
    private Double adjustDpsIncreaseRate;

    @Column(name = "final_dps_delta", nullable = false)
    private Long finalDpsDelta;

    @Column(name = "final_dps_increase_rate", nullable = false)
    private Double finalDpsIncreaseRate;

    /** raw 가성비 (증가율 / 가격 억) — price null 시 null */
    @Column(name = "efficiency_per_eok_raw")
    private Double efficiencyPerEokRaw;

    /** adjust 가성비 */
    @Column(name = "efficiency_per_eok_adjust")
    private Double efficiencyPerEokAdjust;

    /** final 가성비 — UI 기본 정렬·추천 기준 */
    @Column(name = "efficiency_per_eok_final")
    private Double efficiencyPerEokFinal;

    /** 요청 canonical hash (SHA-256) — 중복 저장 방지 */
    @Column(name = "evaluation_hash", nullable = false, length = 64)
    private String evaluationHash;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public DpsValueEvaluation(
            User user, Long deckId, Monster monster, DeckSnapshot scenarioDeckSnapshot,
            ScenarioItemType candidateType, Long candidateRef,
            MercenaryMode mercenaryMode, Long affectedMemberId,
            Server server, Long price, PriceSource priceSource, String priceJson,
            Long rawDpsBefore, Long rawDpsAfter,
            Long adjustDpsBefore, Long adjustDpsAfter,
            Long finalDpsBefore, Long finalDpsAfter,
            Long rawDpsDelta, Double rawDpsIncreaseRate,
            Long adjustDpsDelta, Double adjustDpsIncreaseRate,
            Long finalDpsDelta, Double finalDpsIncreaseRate,
            Double efficiencyPerEokRaw, Double efficiencyPerEokAdjust, Double efficiencyPerEokFinal,
            String evaluationHash) {
        this.user = user;
        this.deckId = deckId;
        this.monster = monster;
        this.scenarioDeckSnapshot = scenarioDeckSnapshot;
        this.candidateType = candidateType;
        this.candidateRef = candidateRef;
        this.mercenaryMode = mercenaryMode;
        this.affectedMemberId = affectedMemberId;
        this.server = server;
        this.price = price;
        this.priceSource = priceSource;
        this.priceJson = priceJson;
        this.rawDpsBefore = rawDpsBefore;
        this.rawDpsAfter = rawDpsAfter;
        this.adjustDpsBefore = adjustDpsBefore;
        this.adjustDpsAfter = adjustDpsAfter;
        this.finalDpsBefore = finalDpsBefore;
        this.finalDpsAfter = finalDpsAfter;
        this.rawDpsDelta = rawDpsDelta;
        this.rawDpsIncreaseRate = rawDpsIncreaseRate;
        this.adjustDpsDelta = adjustDpsDelta;
        this.adjustDpsIncreaseRate = adjustDpsIncreaseRate;
        this.finalDpsDelta = finalDpsDelta;
        this.finalDpsIncreaseRate = finalDpsIncreaseRate;
        this.efficiencyPerEokRaw = efficiencyPerEokRaw;
        this.efficiencyPerEokAdjust = efficiencyPerEokAdjust;
        this.efficiencyPerEokFinal = efficiencyPerEokFinal;
        this.evaluationHash = evaluationHash;
        this.createdAt = LocalDateTime.now();
    }
}
