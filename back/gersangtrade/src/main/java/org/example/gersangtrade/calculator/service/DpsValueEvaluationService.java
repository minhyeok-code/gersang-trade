package org.example.gersangtrade.calculator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.calculator.dto.request.DpsEvaluationRequest;
import org.example.gersangtrade.calculator.dto.request.DpsRequest;
import org.example.gersangtrade.calculator.dto.request.ResistanceType;
import org.example.gersangtrade.calculator.dto.response.DpsResponse;
import org.example.gersangtrade.calculator.dto.response.DpsValueEvaluationResponse;
import org.example.gersangtrade.calculator.dto.response.ItemPriceLine;
import org.example.gersangtrade.calculator.dto.response.PriceResolution;
import org.example.gersangtrade.calculator.dto.response.PriceSource;
import org.example.gersangtrade.calculator.overlay.DpsScenarioOverlay;
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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/**
 * DPS 가성비 평가 오케스트레이션 서비스.
 * before/after DPS 계산 → 가격 조회 → 수치 계산 → (선택) 저장.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DpsValueEvaluationService {

    private final DpsCalculatorService dpsCalculatorService;
    private final DpsScenarioOverlayFactory overlayFactory;
    private final CatalogPriceResolverService priceResolverService;
    private final EvaluationSnapshotBuilder snapshotBuilder;
    private final DpsValueEvaluationRepository evaluationRepository;
    private final UserRepository userRepository;
    private final MonsterRepository monsterRepository;
    private final EquipmentSetPieceRepository equipmentSetPieceRepository;
    private final DeckSnapshotHashUtil hashUtil;
    private final ObjectMapper objectMapper;

    // ──────────────────────────────────────────────────────────────────────
    // 평가 실행
    // ──────────────────────────────────────────────────────────────────────

    @Transactional
    public DpsValueEvaluationResponse evaluate(Long userId, DpsEvaluationRequest req) {
        User user = loadUser(userId);
        validateRequest(req, user);

        DpsRequest dpsReq = toDpsRequest(req);
        DpsResponse before = dpsCalculatorService.calculateWithOverlay(dpsReq, null);

        ResistanceType resistanceType = req.resistanceType() != null
                ? req.resistanceType()
                : DeckResistanceTypeResolver.resolveLoaded(
                        dpsCalculatorService.prepareState(dpsReq, null).members());

        // 평가 당시 기준(before) 덱 스냅샷
        var baselineResult = snapshotBuilder.buildOrReuse(
                dpsCalculatorService.prepareState(dpsReq, null),
                before,
                resistanceType);

        // baseline 포함 해시 — 덱이 바뀌면 별도 평가 row
        String evalHash = buildEvaluationHash(userId, baselineResult.contentHash(), req);
        var existing = evaluationRepository.findByUserIdAndEvaluationHash(userId, evalHash);
        if (existing.isPresent()) {
            return DpsValueEvaluationResponse.ofStored(existing.get());
        }

        DpsScenarioOverlay overlay = overlayFactory.from(req.scenario());
        DpsResponse after = dpsCalculatorService.calculateWithOverlay(dpsReq, overlay);
        PriceResolution priceResolution = resolvePrice(req, user);

        var scenarioResult = snapshotBuilder.buildOrReuse(
                dpsCalculatorService.prepareState(dpsReq, overlay),
                after,
                resistanceType);

        Monster monster = loadMonster(req.monsterId());
        DpsValueEvaluation evaluation = buildEvaluation(
                user,
                monster,
                baselineResult.snapshot(),
                scenarioResult.snapshot(),
                serializeRequest(req),
                req,
                priceResolution,
                before,
                after,
                evalHash);
        DpsValueEvaluation saved = evaluationRepository.save(evaluation);

        return DpsValueEvaluationResponse.ofPersisted(saved, before, after);
    }

    // ──────────────────────────────────────────────────────────────────────
    // 내부 헬퍼
    // ──────────────────────────────────────────────────────────────────────

    private DpsRequest toDpsRequest(DpsEvaluationRequest req) {
        return new DpsRequest(req.deckId(), req.monsterId(),
                req.resistanceType(), req.memberInputs());
    }

    private void validateRequest(DpsEvaluationRequest req, User user) {
        ScenarioItemType type = req.scenario().type();
        if (type != ScenarioItemType.MERCENARY && user.getServer() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "아이템 시나리오 평가는 프로필에서 서버를 설정해야 합니다.");
        }
        if (type == ScenarioItemType.MERCENARY && (req.price() == null || req.price() == 0)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "용병 시나리오는 price가 필수입니다.");
        }
    }

    private PriceResolution resolvePrice(DpsEvaluationRequest req, User user) {
        ScenarioItemType type = req.scenario().type();
        Map<String, Long> overrides = req.priceOverrides() != null ? req.priceOverrides() : Map.of();

        return switch (type) {
            case MERCENARY -> {
                if (req.price() == null || req.price() == 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "용병 시나리오는 price가 필수입니다.");
                }
                yield new PriceResolution(req.price(), PriceSource.USER_INPUT, null, null, List.of());
            }
            case ITEM_SINGLE -> {
                Long itemId = req.scenario().lines().get(0).itemId();
                Integer serverId = user.getServer().getServerId();
                yield priceResolverService.resolveItem(itemId, serverId, overrides);
            }
            case ITEM_SET -> {
                Long setId = req.scenario().setId();
                Integer serverId = user.getServer().getServerId();
                // lines가 있으면 포함 피스만 가격 합산 (부분 세트)
                List<Long> pieceItemIds = (req.scenario().lines() != null && !req.scenario().lines().isEmpty())
                        ? req.scenario().lines().stream().map(l -> l.itemId()).toList()
                        : loadPieceItemIds(setId);
                yield priceResolverService.resolveSet(setId, serverId, pieceItemIds, overrides);
            }
        };
    }

    private List<Long> loadPieceItemIds(Long setId) {
        return equipmentSetPieceRepository.findWithItemByEquipmentSetId(setId).stream()
                .map(p -> p.getEquipmentItem().getItemId())
                .toList();
    }

    private DpsValueEvaluation buildEvaluation(
            User user,
            Monster monster,
            DeckSnapshot baselineSnapshot,
            DeckSnapshot scenarioSnapshot,
            String requestJson,
            DpsEvaluationRequest req,
            PriceResolution price,
            DpsResponse before,
            DpsResponse after,
            String evalHash) {

        long rawBefore    = before.rawTotalDps();
        long adjustBefore = before.adjustTotalDps();
        long finalBefore  = before.totalDps();
        long rawAfter     = after.rawTotalDps();
        long adjustAfter  = after.adjustTotalDps();
        long finalAfter   = after.totalDps();

        double rawRate    = increaseRate(rawBefore,    rawAfter);
        double adjustRate = increaseRate(adjustBefore, adjustAfter);
        double finalRate  = increaseRate(finalBefore,  finalAfter);

        Server server = (req.scenario().type() != ScenarioItemType.MERCENARY)
                ? user.getServer() : null;

        return DpsValueEvaluation.builder()
                .user(user)
                .deckId(req.deckId())
                .monster(monster)
                .baselineDeckSnapshot(baselineSnapshot)
                .scenarioDeckSnapshot(scenarioSnapshot)
                .requestJson(requestJson)
                .candidateType(req.scenario().type())
                .candidateRef(resolveCandidateRef(req))
                .mercenaryMode(req.scenario().mode())
                .affectedMemberId(req.scenario().affectedMemberId())
                .server(server)
                .price(price.totalPrice())
                .priceSource(price.source())
                .priceJson(breakdownJson(price.breakdown()))
                .rawDpsBefore(rawBefore).rawDpsAfter(rawAfter)
                .adjustDpsBefore(adjustBefore).adjustDpsAfter(adjustAfter)
                .finalDpsBefore(finalBefore).finalDpsAfter(finalAfter)
                .rawDpsDelta(rawAfter - rawBefore).rawDpsIncreaseRate(rawRate)
                .adjustDpsDelta(adjustAfter - adjustBefore).adjustDpsIncreaseRate(adjustRate)
                .finalDpsDelta(finalAfter - finalBefore).finalDpsIncreaseRate(finalRate)
                .efficiencyPerEokRaw(efficiency(rawRate,       price.totalPrice()))
                .efficiencyPerEokAdjust(efficiency(adjustRate, price.totalPrice()))
                .efficiencyPerEokFinal(efficiency(finalRate,   price.totalPrice()))
                .evaluationHash(evalHash)
                .build();
    }

    private Long resolveCandidateRef(DpsEvaluationRequest req) {
        return switch (req.scenario().type()) {
            case ITEM_SINGLE -> req.scenario().lines().get(0).itemId();
            case ITEM_SET    -> req.scenario().setId();
            case MERCENARY   -> req.scenario().mercenaryId();
        };
    }

    private String buildEvaluationHash(Long userId, String baselineContentHash, DpsEvaluationRequest req) {
        try {
            record HashPayload(Long userId, String baselineContentHash, DpsEvaluationRequest req) {}
            String json = hashUtil.toCanonicalJson(new HashPayload(userId, baselineContentHash, req));
            return hashUtil.sha256Hex(json);
        } catch (Exception e) {
            throw new IllegalStateException("evaluation_hash 생성 실패", e);
        }
    }

    private String serializeRequest(DpsEvaluationRequest req) {
        try {
            return hashUtil.toCanonicalJson(req);
        } catch (Exception e) {
            throw new IllegalStateException("request_json 직렬화 실패", e);
        }
    }

    private String breakdownJson(List<ItemPriceLine> breakdown) {
        if (breakdown == null || breakdown.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(breakdown);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private static double increaseRate(long before, long after) {
        if (before <= 0) return 0.0;
        return (after / (double) before - 1.0) * 100.0;
    }

    private static Double efficiency(double increaseRate, Long price) {
        if (price == null || price == 0) return null;
        return increaseRate / (price / 100_000_000.0);
    }

    private User loadUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
    }

    private Monster loadMonster(Long monsterId) {
        return monsterRepository.findById(monsterId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "몬스터를 찾을 수 없습니다. id=" + monsterId));
    }

}
