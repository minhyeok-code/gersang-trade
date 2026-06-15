package org.example.gersangtrade.calculator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.calculator.dto.request.DpsEvaluationRequest;
import org.example.gersangtrade.calculator.dto.request.ScenarioRequest;
import org.example.gersangtrade.calculator.overlay.MercenaryMode;
import org.example.gersangtrade.calculator.overlay.ScenarioItemType;
import org.example.gersangtrade.calculator.overlay.ScenarioLine;
import org.example.gersangtrade.catalog.repository.ItemRepository;
import org.example.gersangtrade.catalog.repository.MercenaryRepository;
import org.example.gersangtrade.domain.calculator.DpsValueEvaluation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 평가 후보를 화면 표시용 이름으로 변환한다.
 * request_json의 시나리오를 우선 사용해 아이템·세트 평가가 용병명으로 표시되지 않게 한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EvaluationCandidateLabelResolver {

    private final ItemRepository itemRepository;
    private final MercenaryRepository mercenaryRepository;
    private final EvaluationSetTitleResolver setTitleResolver;
    private final ObjectMapper objectMapper;

    /** 저장 row 기준 — request_json 우선 */
    public String resolve(DpsValueEvaluation evaluation) {
        DpsEvaluationRequest request = parseRequest(evaluation.getRequestJson());
        if (request != null && request.scenario() != null && request.scenario().type() != null) {
            return resolveFromScenario(request.scenario());
        }
        return resolveFromStored(evaluation.getCandidateType(), evaluation.getCandidateRef(),
                evaluation.getMercenaryMode());
    }

    public String resolve(ScenarioItemType type, Long candidateRef) {
        return resolveFromStored(type, candidateRef, null);
    }

    private String resolveFromStored(ScenarioItemType type, Long candidateRef, MercenaryMode mercenaryMode) {
        if (candidateRef == null) {
            return "—";
        }
        // 아이템·세트 평가인데 MERCENARY로 잘못 저장된 경우 용병명 대신 아이템·세트명 시도
        if (type == ScenarioItemType.MERCENARY && mercenaryMode == null) {
            String itemName = lookupItemName(candidateRef);
            if (itemName != null) {
                return itemName;
            }
            String setTitle = resolveSetTitle(candidateRef, null);
            if (setTitle != null) {
                return setTitle;
            }
        }
        return switch (type) {
            case ITEM_SINGLE -> {
                String name = lookupItemName(candidateRef);
                yield name != null ? name : "삭제된 아이템";
            }
            case ITEM_SET -> {
                String title = resolveSetTitle(candidateRef, null);
                yield title != null ? title : "삭제된 세트";
            }
            case MERCENARY -> {
                String name = lookupMercenaryName(candidateRef);
                yield name != null ? name : "삭제된 용병";
            }
        };
    }

    private String resolveFromScenario(ScenarioRequest scenario) {
        return switch (scenario.type()) {
            case ITEM_SINGLE -> {
                if (scenario.lines() == null || scenario.lines().isEmpty()) {
                    yield "—";
                }
                String name = lookupItemName(scenario.lines().get(0).itemId());
                yield name != null ? name : "삭제된 아이템";
            }
            case ITEM_SET -> {
                if (scenario.setId() == null) {
                    yield "—";
                }
                String title = resolveSetTitle(scenario.setId(), scenario.lines());
                yield title != null ? title : "삭제된 세트";
            }
            case MERCENARY -> {
                if (scenario.mercenaryId() == null) {
                    yield "—";
                }
                String name = lookupMercenaryName(scenario.mercenaryId());
                yield name != null ? name : "삭제된 용병";
            }
        };
    }

    private String lookupItemName(Long itemId) {
        return itemRepository.findById(itemId).map(i -> i.getName()).orElse(null);
    }

    private String resolveSetTitle(Long setId, java.util.List<ScenarioLine> lines) {
        return setTitleResolver.resolve(setId, lines).orElse(null);
    }

    private String lookupMercenaryName(Long mercenaryId) {
        return mercenaryRepository.findById(mercenaryId).map(m -> m.getName()).orElse(null);
    }

    private DpsEvaluationRequest parseRequest(String requestJson) {
        if (requestJson == null || requestJson.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(requestJson, DpsEvaluationRequest.class);
        } catch (Exception e) {
            return null;
        }
    }
}
