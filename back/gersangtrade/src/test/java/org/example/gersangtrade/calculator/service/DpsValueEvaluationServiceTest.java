package org.example.gersangtrade.calculator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.gersangtrade.calculator.dto.request.DpsEvaluationRequest;
import org.example.gersangtrade.calculator.dto.request.ResistanceType;
import org.example.gersangtrade.calculator.dto.response.DpsResponse;
import org.example.gersangtrade.calculator.dto.response.DpsValueEvaluationResponse;
import org.example.gersangtrade.calculator.dto.response.PriceResolution;
import org.example.gersangtrade.calculator.dto.response.PriceSource;
import org.example.gersangtrade.calculator.overlay.DeckCalculationState;
import org.example.gersangtrade.calculator.overlay.DpsScenarioOverlay;
import org.example.gersangtrade.calculator.overlay.MercenaryMode;
import org.example.gersangtrade.calculator.overlay.ScenarioItemType;
import org.example.gersangtrade.calculator.repository.DpsValueEvaluationRepository;
import org.example.gersangtrade.catalog.repository.EquipmentSetPieceRepository;
import org.example.gersangtrade.catalog.repository.MonsterRepository;
import org.example.gersangtrade.domain.calculator.DpsValueEvaluation;
import org.example.gersangtrade.domain.catalog.Monster;
import org.example.gersangtrade.domain.catalog.Server;
import org.example.gersangtrade.domain.hunt.DeckSnapshot;
import org.example.gersangtrade.domain.user.User;
import org.example.gersangtrade.domain.user.UserRepository;
import org.example.gersangtrade.hunt.service.DeckSnapshotHashUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * DpsValueEvaluationService 단위 테스트.
 *
 * <p>검증 시나리오:
 * <ul>
 *   <li>persist=false → 저장 없이 transient 응답
 *   <li>persist=true (신규) → 저장 후 persisted 응답
 *   <li>persist=true (중복 hash) → 기존 row 반환, 추가 저장 없음
 *   <li>ITEM 시나리오 + 서버 미설정 → 400
 *   <li>MERCENARY 시나리오 + price 누락 → 400
 *   <li>사용자 없음 → 404
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DpsValueEvaluationServiceTest {

    @Mock private DpsCalculatorService dpsCalculatorService;
    @Mock private DpsScenarioOverlayFactory overlayFactory;
    @Mock private CatalogPriceResolverService priceResolverService;
    @Mock private EvaluationSnapshotBuilder snapshotBuilder;
    @Mock private DpsValueEvaluationRepository evaluationRepository;
    @Mock private UserRepository userRepository;
    @Mock private MonsterRepository monsterRepository;
    @Mock private EquipmentSetPieceRepository equipmentSetPieceRepository;
    @Mock private DeckSnapshotHashUtil hashUtil;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks
    private DpsValueEvaluationService service;

    private static final Long USER_ID    = 1L;
    private static final Long DECK_ID    = 10L;
    private static final Long MONSTER_ID = 20L;
    private static final Long MERC_ID    = 30L;
    private static final String EVAL_HASH = "abc123hash";

    private User user;
    private Server server;
    private Monster monster;
    private DpsResponse beforeDps;
    private DpsResponse afterDps;
    private DpsScenarioOverlay overlay;

    @BeforeEach
    void setUp() {
        server = Server.builder().serverId(1).name("백호").isActive(true).build();

        user = mock(User.class);
        when(user.getServer()).thenReturn(server);

        monster = mock(Monster.class);
        when(monster.getId()).thenReturn(MONSTER_ID);
        when(monster.getName()).thenReturn("테스트몬스터");

        // before DPS: raw=10000, adjust=8000, total=3000
        beforeDps = new DpsResponse(MONSTER_ID, "테스트몬스터",
                100, 50, 43.0, 0, 0,
                10_000L, 8_000L, 3_000L, List.of());

        // after DPS: raw=12000, adjust=10000, total=4000 (20%/25%/33% 상승)
        afterDps = new DpsResponse(MONSTER_ID, "테스트몬스터",
                120, 30, 43.0, 0, 0,
                12_000L, 10_000L, 4_000L, List.of());

        overlay = mock(DpsScenarioOverlay.class);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(monsterRepository.findById(MONSTER_ID)).thenReturn(Optional.of(monster));
        when(overlayFactory.from(any())).thenReturn(overlay);
        when(dpsCalculatorService.calculateWithOverlay(any(), isNull())).thenReturn(beforeDps);
        when(dpsCalculatorService.calculateWithOverlay(any(), eq(overlay))).thenReturn(afterDps);
        when(hashUtil.toCanonicalJson(any())).thenReturn("{}");
        when(hashUtil.sha256Hex(anyString())).thenReturn(EVAL_HASH);
    }

    // ── 신규 저장 ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("persist_true_신규요청_저장후_persisted_반환")
    void persist_true_신규요청_저장후_persisted_반환() {
        DpsEvaluationRequest req = mercenaryRequest(true, 200_000_000L);

        when(evaluationRepository.findByUserIdAndEvaluationHash(USER_ID, EVAL_HASH))
                .thenReturn(Optional.empty());
        when(dpsCalculatorService.prepareState(any(), any()))
                .thenReturn(mock(DeckCalculationState.class));

        DeckSnapshot snapshot = mock(DeckSnapshot.class);
        when(snapshot.getId()).thenReturn(99L);
        EvaluationSnapshotBuilder.BuildResult buildResult =
                new EvaluationSnapshotBuilder.BuildResult(snapshot, "contenthash");
        when(snapshotBuilder.buildOrReuse(any(), any(), any())).thenReturn(buildResult);

        DpsValueEvaluation savedEval = mockSavedEvaluation(42L, snapshot, snapshot);
        when(evaluationRepository.save(any())).thenReturn(savedEval);

        DpsValueEvaluationResponse resp = service.evaluate(USER_ID, req);

        assertThat(resp.persisted()).isTrue();
        assertThat(resp.evaluationId()).isEqualTo(42L);
        assertThat(resp.baselineDeckSnapshotId()).isEqualTo(99L);
        assertThat(resp.scenarioDeckSnapshotId()).isEqualTo(99L);
        verify(snapshotBuilder, org.mockito.Mockito.times(2)).buildOrReuse(any(), any(), any());
        verify(evaluationRepository).save(any(DpsValueEvaluation.class));
    }

    // ── persist=true (중복) ───────────────────────────────────────────────────

    @Test
    @DisplayName("persist_true_중복hash_기존결과_반환_추가저장없음")
    void persist_true_중복hash_기존결과_반환_추가저장없음() {
        DpsEvaluationRequest req = mercenaryRequest(true, 200_000_000L);

        DeckSnapshot snapshot = mock(DeckSnapshot.class);
        when(snapshot.getId()).thenReturn(77L);
        DpsValueEvaluation existing = mockSavedEvaluation(55L, snapshot, snapshot);
        when(evaluationRepository.findByUserIdAndEvaluationHash(USER_ID, EVAL_HASH))
                .thenReturn(Optional.of(existing));
        when(dpsCalculatorService.prepareState(any(), any()))
                .thenReturn(mock(DeckCalculationState.class));
        when(snapshotBuilder.buildOrReuse(any(), any(), any())).thenReturn(
                new EvaluationSnapshotBuilder.BuildResult(snapshot, "contenthash"));

        DpsValueEvaluationResponse resp = service.evaluate(USER_ID, req);

        assertThat(resp.persisted()).isTrue();
        assertThat(resp.evaluationId()).isEqualTo(55L);
        // 중복이므로 baseline 스냅샷 1회만 빌드, scenario·저장 없음
        verify(snapshotBuilder, org.mockito.Mockito.times(1)).buildOrReuse(any(), any(), any());
        verify(evaluationRepository, never()).save(any());
    }

    // ── 검증 오류 ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("ITEM_SINGLE_서버미설정_400_반환")
    void ITEM_SINGLE_서버미설정_400_반환() {
        when(user.getServer()).thenReturn(null);
        DpsEvaluationRequest req = itemSingleRequest(false);

        assertThatThrownBy(() -> service.evaluate(USER_ID, req))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("서버를 설정해야 합니다");
    }

    @Test
    @DisplayName("MERCENARY_price_null_400_반환")
    void MERCENARY_price_null_400_반환() {
        DpsEvaluationRequest req = mercenaryRequest(false, null);

        assertThatThrownBy(() -> service.evaluate(USER_ID, req))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("price가 필수입니다");
    }

    @Test
    @DisplayName("MERCENARY_price_0_400_반환")
    void MERCENARY_price_0_400_반환() {
        DpsEvaluationRequest req = mercenaryRequest(false, 0L);

        assertThatThrownBy(() -> service.evaluate(USER_ID, req))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("price가 필수입니다");
    }

    @Test
    @DisplayName("사용자없음_404_반환")
    void 사용자없음_404_반환() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());
        DpsEvaluationRequest req = mercenaryRequest(false, 100_000_000L);

        assertThatThrownBy(() -> service.evaluate(USER_ID, req))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("사용자를 찾을 수 없습니다");
    }

    // ── 헬퍼 ─────────────────────────────────────────────────────────────────

    /** MERCENARY 시나리오 요청 생성 */
    private DpsEvaluationRequest mercenaryRequest(boolean persist, Long price) {
        var scenario = new org.example.gersangtrade.calculator.dto.request.ScenarioRequest(
                ScenarioItemType.MERCENARY,
                null, null, null,
                MERC_ID, MercenaryMode.APPEND,
                250, null, 0, List.of()
        );
        return new DpsEvaluationRequest(
                DECK_ID, MONSTER_ID,
                ResistanceType.MAGIC,
                scenario,
                null, price, null, persist
        );
    }

    /** ITEM_SINGLE 시나리오 요청 생성 */
    private DpsEvaluationRequest itemSingleRequest(boolean persist) {
        var line = new org.example.gersangtrade.calculator.overlay.ScenarioLine(
                200L, 1, 1, null);
        var scenario = new org.example.gersangtrade.calculator.dto.request.ScenarioRequest(
                ScenarioItemType.ITEM_SINGLE,
                null, 50L, List.of(line),
                null, null, null, null, null, null
        );
        return new DpsEvaluationRequest(
                DECK_ID, MONSTER_ID,
                ResistanceType.MAGIC,
                scenario,
                null, null, null, persist
        );
    }

    /** 저장된 평가 엔티티 mock — id·snapshot·DPS 필드 최소 설정 */
    private DpsValueEvaluation mockSavedEvaluation(Long id, DeckSnapshot baseline, DeckSnapshot scenario) {
        DpsValueEvaluation eval = mock(DpsValueEvaluation.class);
        when(eval.getId()).thenReturn(id);
        when(eval.getBaselineDeckSnapshot()).thenReturn(baseline);
        when(eval.getScenarioDeckSnapshot()).thenReturn(scenario);
        when(eval.getPrice()).thenReturn(200_000_000L);
        when(eval.getPriceSource()).thenReturn(PriceSource.USER_INPUT);
        when(eval.getRawDpsBefore()).thenReturn(10_000L);
        when(eval.getAdjustDpsBefore()).thenReturn(8_000L);
        when(eval.getFinalDpsBefore()).thenReturn(3_000L);
        when(eval.getRawDpsAfter()).thenReturn(12_000L);
        when(eval.getAdjustDpsAfter()).thenReturn(10_000L);
        when(eval.getFinalDpsAfter()).thenReturn(4_000L);
        return eval;
    }
}
