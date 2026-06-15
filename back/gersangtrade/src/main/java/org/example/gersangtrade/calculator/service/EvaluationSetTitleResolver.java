package org.example.gersangtrade.calculator.service;

import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.calculator.overlay.ScenarioLine;
import org.example.gersangtrade.catalog.repository.EquipmentItemRepository;
import org.example.gersangtrade.catalog.repository.EquipmentSetPieceRepository;
import org.example.gersangtrade.catalog.repository.EquipmentSetRepository;
import org.example.gersangtrade.catalog.repository.RitualRepository;
import org.example.gersangtrade.domain.catalog.EquipmentItem;
import org.example.gersangtrade.domain.catalog.EquipmentSet;
import org.example.gersangtrade.domain.catalog.EquipmentSetPiece;
import org.example.gersangtrade.domain.catalog.Ritual;
import org.example.gersangtrade.listing.dto.request.EquipmentDetailRequest;
import org.example.gersangtrade.listing.dto.request.RitualResultRequest;
import org.example.gersangtrade.listing.service.SetTitleGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 가성비 평가 ITEM_SET 시나리오의 거래 표기 제목을 생성한다.
 * 판매·구매 희망의 {@link org.example.gersangtrade.listing.service.SetTitleResolver}와 동일 규칙.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EvaluationSetTitleResolver {

    private final EquipmentSetRepository equipmentSetRepository;
    private final EquipmentSetPieceRepository equipmentSetPieceRepository;
    private final EquipmentItemRepository equipmentItemRepository;
    private final RitualRepository ritualRepository;

    /**
     * @param setId 세트 ID
     * @param lines 시나리오 피스 라인 — null·empty이면 세트 전 피스(풀 등)로 간주
     */
    public Optional<String> resolve(Long setId, List<ScenarioLine> lines) {
        EquipmentSet set = equipmentSetRepository.findById(setId).orElse(null);
        if (set == null) {
            return Optional.empty();
        }

        List<SetTitleGenerator.PieceTitleInput> pieceInputs = buildPieceInputs(setId, lines);
        if (pieceInputs.isEmpty()) {
            return Optional.of(set.getName());
        }
        return Optional.of(SetTitleGenerator.generate(set.getName(), pieceInputs));
    }

    private List<SetTitleGenerator.PieceTitleInput> buildPieceInputs(Long setId, List<ScenarioLine> lines) {
        boolean partialMode = lines != null && !lines.isEmpty();
        if (partialMode) {
            return buildFromScenarioLines(lines);
        }
        return buildFromFullSetPieces(setId);
    }

    /** 부분 세트·주술 포함 — 요청 lines 기준 */
    private List<SetTitleGenerator.PieceTitleInput> buildFromScenarioLines(List<ScenarioLine> lines) {
        List<ScenarioLine> sorted = lines.stream()
                .sorted(Comparator.comparingInt(ScenarioLine::sortOrder))
                .toList();

        List<Long> itemIds = sorted.stream().map(ScenarioLine::itemId).toList();
        Map<Long, EquipmentItem> equipmentByItemId = equipmentItemRepository
                .findWithItemAndSetByItemIdIn(itemIds).stream()
                .collect(Collectors.toMap(EquipmentItem::getItemId, Function.identity()));

        List<SetTitleGenerator.PieceTitleInput> inputs = new ArrayList<>();
        for (ScenarioLine line : sorted) {
            EquipmentItem equipment = equipmentByItemId.get(line.itemId());
            if (equipment == null) {
                continue;
            }
            inputs.add(new SetTitleGenerator.PieceTitleInput(
                    equipment.getSlot(),
                    resolveRitualMark(line.equipmentDetail())));
        }
        return inputs;
    }

    /** lines 미전송(풀·무주술 등) — 세트 정의 피스 전체 */
    private List<SetTitleGenerator.PieceTitleInput> buildFromFullSetPieces(Long setId) {
        List<EquipmentSetPiece> pieces = equipmentSetPieceRepository.findWithItemByEquipmentSetId(setId);
        return pieces.stream()
                .map(p -> new SetTitleGenerator.PieceTitleInput(p.getSlot(), null))
                .toList();
    }

    private String resolveRitualMark(EquipmentDetailRequest detail) {
        if (detail == null || !detail.hasRitual()) {
            return null;
        }
        if (detail.rituals() == null || detail.rituals().isEmpty()) {
            return null;
        }
        RitualResultRequest first = detail.rituals().get(0);
        Ritual ritual = ritualRepository.findById(first.ritualId()).orElse(null);
        if (ritual == null) {
            return null;
        }
        return SetTitleGenerator.buildTitleMark(ritual, first.outcome());
    }
}
