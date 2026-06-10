package org.example.gersangtrade.calculator.service;

import org.example.gersangtrade.calculator.dto.response.DpsEvaluationSummary;
import org.example.gersangtrade.calculator.dto.response.DpsValueEvaluationResponse;
import org.example.gersangtrade.calculator.dto.response.PriceSource;
import org.example.gersangtrade.calculator.overlay.MercenaryMode;
import org.example.gersangtrade.calculator.overlay.ScenarioItemType;
import org.example.gersangtrade.calculator.repository.DpsValueEvaluationRepository;
import org.example.gersangtrade.domain.calculator.DpsValueEvaluation;
import org.example.gersangtrade.domain.catalog.Monster;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.offset;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * DpsValueEvaluationQueryService 단위 테스트.
 *
 * <p>검증 시나리오:
 * <ul>
 *   <li>목록 조회 — 페이징 매핑, summary 필드 정확성
 *   <li>상세 조회 — DB 저장 값 → DpsValueEvaluationResponse 복원
 *   <li>타인 평가 접근 → 404
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DpsValueEvaluationQueryServiceTest {

    @Mock private DpsValueEvaluationRepository evaluationRepository;
    @Mock private EvaluationCandidateLabelResolver candidateLabelResolver;

    @InjectMocks
    private DpsValueEvaluationQueryService queryService;

    private static final Long USER_ID = 1L;
    private static final Long EVAL_ID = 10L;

    // ── 목록 조회 ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getMyEvaluations_페이징결과_summary_필드_매핑")
    void getMyEvaluations_페이징결과_summary_필드_매핑() {
        DpsValueEvaluation eval = buildEvaluation(EVAL_ID);
        Page<DpsValueEvaluation> dbPage = new PageImpl<>(List.of(eval), PageRequest.of(0, 20), 1);
        when(evaluationRepository.findByUserIdWithMonster(eq(USER_ID), any())).thenReturn(dbPage);
        when(candidateLabelResolver.resolve(ScenarioItemType.MERCENARY, 99L)).thenReturn("테스트용병");

        Page<DpsEvaluationSummary> result = queryService.getMyEvaluations(USER_ID, PageRequest.of(0, 20));

        assertThat(result.getTotalElements()).isEqualTo(1);
        DpsEvaluationSummary summary = result.getContent().get(0);
        assertThat(summary.evaluationId()).isEqualTo(EVAL_ID);
        assertThat(summary.candidateType()).isEqualTo(ScenarioItemType.MERCENARY);
        assertThat(summary.candidateLabel()).isEqualTo("테스트용병");
        assertThat(summary.candidateRef()).isEqualTo(99L);
        assertThat(summary.mercenaryMode()).isEqualTo(MercenaryMode.REPLACE);
        assertThat(summary.monsterId()).isEqualTo(20L);
        assertThat(summary.monsterName()).isEqualTo("테스트몬스터");
        assertThat(summary.finalDpsIncreaseRate()).isCloseTo(10.0, offset(0.01));
        assertThat(summary.efficiencyPerEokFinal()).isCloseTo(5.0, offset(0.01));
        assertThat(summary.price()).isEqualTo(200_000_000L);
        assertThat(summary.formattedPrice()).isEqualTo("2억");
        assertThat(summary.priceSource()).isEqualTo(PriceSource.USER_INPUT);
    }

    @Test
    @DisplayName("getMyEvaluations_빈결과_빈페이지_반환")
    void getMyEvaluations_빈결과_빈페이지_반환() {
        when(evaluationRepository.findByUserIdWithMonster(eq(USER_ID), any()))
                .thenReturn(Page.empty());

        Page<DpsEvaluationSummary> result = queryService.getMyEvaluations(USER_ID, PageRequest.of(0, 20));

        assertThat(result.isEmpty()).isTrue();
    }

    // ── 상세 조회 ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getMyEvaluation_DB저장값_response로_복원")
    void getMyEvaluation_DB저장값_response로_복원() {
        DpsValueEvaluation eval = buildEvaluation(EVAL_ID);
        when(evaluationRepository.findByIdAndUserId(EVAL_ID, USER_ID)).thenReturn(Optional.of(eval));

        DpsValueEvaluationResponse resp = queryService.getMyEvaluation(USER_ID, EVAL_ID);

        assertThat(resp.persisted()).isTrue();
        assertThat(resp.evaluationId()).isEqualTo(EVAL_ID);
        assertThat(resp.scenarioDeckSnapshotId()).isNull();
        assertThat(resp.before().raw()).isEqualTo(10_000L);
        assertThat(resp.before().adjust()).isEqualTo(8_000L);
        assertThat(resp.before().finalDps()).isEqualTo(3_000L);
        assertThat(resp.after().raw()).isEqualTo(11_000L);
        assertThat(resp.after().finalDps()).isEqualTo(3_300L);
        assertThat(resp.delta().raw()).isEqualTo(1_000L);
        assertThat(resp.delta().finalDps()).isEqualTo(300L);
        assertThat(resp.increaseRate().finalDps()).isCloseTo(10.0, offset(0.01));
        assertThat(resp.efficiencyPerEok().finalDps()).isCloseTo(5.0, offset(0.01));
        assertThat(resp.price()).isEqualTo(200_000_000L);
        assertThat(resp.formattedPrice()).isEqualTo("2억");
        assertThat(resp.priceSource()).isEqualTo(PriceSource.USER_INPUT);
    }

    @Test
    @DisplayName("getMyEvaluation_타인평가_404")
    void getMyEvaluation_타인평가_404() {
        when(evaluationRepository.findByIdAndUserId(EVAL_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> queryService.getMyEvaluation(USER_ID, EVAL_ID))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("평가 결과를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("getMyEvaluation_스냅샷있으면_snapshotId_반환")
    void getMyEvaluation_스냅샷있으면_snapshotId_반환() {
        DpsValueEvaluation eval = buildEvaluation(EVAL_ID);
        var snapshot = mock(org.example.gersangtrade.domain.hunt.DeckSnapshot.class);
        when(snapshot.getId()).thenReturn(77L);
        when(eval.getScenarioDeckSnapshot()).thenReturn(snapshot);
        when(evaluationRepository.findByIdAndUserId(EVAL_ID, USER_ID)).thenReturn(Optional.of(eval));

        DpsValueEvaluationResponse resp = queryService.getMyEvaluation(USER_ID, EVAL_ID);

        assertThat(resp.scenarioDeckSnapshotId()).isEqualTo(77L);
    }

    // ── 삭제 ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteMyEvaluation_본인기록_삭제")
    void deleteMyEvaluation_본인기록_삭제() {
        DpsValueEvaluation eval = buildEvaluation(EVAL_ID);
        when(evaluationRepository.findByIdAndUserId(EVAL_ID, USER_ID)).thenReturn(Optional.of(eval));

        queryService.deleteMyEvaluation(USER_ID, EVAL_ID);

        verify(evaluationRepository).delete(eval);
    }

    @Test
    @DisplayName("deleteMyEvaluation_타인기록_404")
    void deleteMyEvaluation_타인기록_404() {
        when(evaluationRepository.findByIdAndUserId(EVAL_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> queryService.deleteMyEvaluation(USER_ID, EVAL_ID))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("평가 결과를 찾을 수 없습니다");
    }

    // ── 헬퍼 ─────────────────────────────────────────────────────────────────

    private DpsValueEvaluation buildEvaluation(Long id) {
        Monster monster = mock(Monster.class);
        when(monster.getId()).thenReturn(20L);
        when(monster.getName()).thenReturn("테스트몬스터");

        DpsValueEvaluation eval = mock(DpsValueEvaluation.class);
        when(eval.getId()).thenReturn(id);
        when(eval.getCandidateType()).thenReturn(ScenarioItemType.MERCENARY);
        when(eval.getCandidateRef()).thenReturn(99L);
        when(eval.getMercenaryMode()).thenReturn(MercenaryMode.REPLACE);
        when(eval.getMonster()).thenReturn(monster);
        when(eval.getScenarioDeckSnapshot()).thenReturn(null);
        when(eval.getPrice()).thenReturn(200_000_000L);
        when(eval.getPriceSource()).thenReturn(PriceSource.USER_INPUT);
        when(eval.getCreatedAt()).thenReturn(LocalDateTime.of(2026, 6, 10, 12, 0));

        // before
        when(eval.getRawDpsBefore()).thenReturn(10_000L);
        when(eval.getAdjustDpsBefore()).thenReturn(8_000L);
        when(eval.getFinalDpsBefore()).thenReturn(3_000L);
        // after
        when(eval.getRawDpsAfter()).thenReturn(11_000L);
        when(eval.getAdjustDpsAfter()).thenReturn(8_800L);
        when(eval.getFinalDpsAfter()).thenReturn(3_300L);
        // delta
        when(eval.getRawDpsDelta()).thenReturn(1_000L);
        when(eval.getAdjustDpsDelta()).thenReturn(800L);
        when(eval.getFinalDpsDelta()).thenReturn(300L);
        // rate: final = (3300/3000 - 1) * 100 = 10.0%
        when(eval.getRawDpsIncreaseRate()).thenReturn(10.0);
        when(eval.getAdjustDpsIncreaseRate()).thenReturn(10.0);
        when(eval.getFinalDpsIncreaseRate()).thenReturn(10.0);
        // efficiency: 10.0 / (200_000_000 / 100_000_000) = 5.0
        when(eval.getEfficiencyPerEokRaw()).thenReturn(5.0);
        when(eval.getEfficiencyPerEokAdjust()).thenReturn(5.0);
        when(eval.getEfficiencyPerEokFinal()).thenReturn(5.0);

        return eval;
    }
}
