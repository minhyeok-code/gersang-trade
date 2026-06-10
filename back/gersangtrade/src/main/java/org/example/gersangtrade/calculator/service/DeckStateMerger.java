package org.example.gersangtrade.calculator.service;

import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.calculator.dto.request.MemberDpsInput;
import org.example.gersangtrade.calculator.overlay.DeckCalculationState;
import org.example.gersangtrade.calculator.overlay.DpsScenarioOverlay;
import org.example.gersangtrade.calculator.overlay.DpsScenarioOverlay.ItemScenarioOverlay;
import org.example.gersangtrade.calculator.overlay.DpsScenarioOverlay.MercenaryScenarioOverlay;
import org.example.gersangtrade.calculator.overlay.LoadedDeckState;
import org.example.gersangtrade.calculator.overlay.LoadedMember;
import org.example.gersangtrade.calculator.overlay.MercenaryMode;
import org.example.gersangtrade.calculator.overlay.ScenarioItemType;
import org.example.gersangtrade.calculator.overlay.ScenarioLine;
import org.example.gersangtrade.catalog.repository.EquipmentItemRepository;
import org.example.gersangtrade.catalog.repository.EquipmentSetPieceRepository;
import org.example.gersangtrade.catalog.repository.MercenaryCharacteristicRepository;
import org.example.gersangtrade.catalog.repository.MercenaryRepository;
import org.example.gersangtrade.catalog.repository.RitualRepository;
import org.example.gersangtrade.deck.service.DeckEquipmentValidator;
import org.example.gersangtrade.domain.catalog.EquipmentItem;
import org.example.gersangtrade.domain.catalog.EquipmentSetPiece;
import org.example.gersangtrade.domain.catalog.Mercenary;
import org.example.gersangtrade.domain.catalog.MercenaryCharacteristic;
import org.example.gersangtrade.domain.catalog.Ritual;
import org.example.gersangtrade.domain.catalog.enums.EquipmentSlot;
import org.example.gersangtrade.domain.deck.UserDeckMemberCharacteristic;
import org.example.gersangtrade.domain.deck.UserDeckMemberSlot;
import org.example.gersangtrade.domain.deck.UserDeckMemberSlotRitual;
import org.example.gersangtrade.domain.deck.enums.EquipSlot;
import org.example.gersangtrade.domain.listing.enums.RitualOutcome;
import org.example.gersangtrade.hunt.dto.DeckSnapshotContent.CharacteristicSelection;
import org.example.gersangtrade.listing.dto.request.EquipmentDetailRequest;
import org.example.gersangtrade.listing.dto.request.RitualResultRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * LoadedDeckState + overlay 명세 → DeckCalculationState.
 * DB write 없이 in-memory 상태만 변경한다.
 */
@Service
@RequiredArgsConstructor
public class DeckStateMerger {

    private final DeckEquipmentValidator deckEquipmentValidator;
    private final MercenaryRepository mercenaryRepository;
    private final MercenaryCharacteristicRepository characteristicRepository;
    private final EquipmentItemRepository equipmentItemRepository;
    private final EquipmentSetPieceRepository equipmentSetPieceRepository;
    private final RitualRepository ritualRepository;

    /**
     * LoadedDeckState에 overlay를 적용해 DPS 파이프라인용 상태를 생성한다.
     */
    public DeckCalculationState merge(LoadedDeckState loaded, DpsScenarioOverlay overlay) {
        if (overlay.mercenary() != null) {
            return applyMercenaryOverlay(loaded, overlay.mercenary());
        }
        return applyItemOverlay(loaded, overlay.item());
    }

    // ── 용병 overlay ────────────────────────────────────────────────────────

    private DeckCalculationState applyMercenaryOverlay(LoadedDeckState loaded,
                                                        MercenaryScenarioOverlay ov) {
        Mercenary newMercenary = mercenaryRepository.findById(ov.mercenaryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "용병을 찾을 수 없습니다. id=" + ov.mercenaryId()));

        List<UserDeckMemberCharacteristic> newCharacteristics =
                resolveCharacteristics(ov.characteristics());

        List<LoadedMember> newMembers = new ArrayList<>(loaded.members());
        Map<Long, MemberDpsInput> newInputs = new HashMap<>(loaded.memberInputs());

        if (ov.mode() == MercenaryMode.REPLACE) {
            return applyMercenaryReplace(loaded, ov, newMercenary, newCharacteristics,
                    newMembers, newInputs);
        } else {
            return applyMercenaryAppend(loaded, ov, newMercenary, newCharacteristics,
                    newMembers, newInputs);
        }
    }

    private DeckCalculationState applyMercenaryReplace(LoadedDeckState loaded,
                                                         MercenaryScenarioOverlay ov,
                                                         Mercenary newMercenary,
                                                         List<UserDeckMemberCharacteristic> chars,
                                                         List<LoadedMember> members,
                                                         Map<Long, MemberDpsInput> inputs) {
        long replacedIdx = -1;
        for (int i = 0; i < members.size(); i++) {
            if (members.get(i).memberId().equals(ov.affectedMemberId())) {
                replacedIdx = i;
                break;
            }
        }
        if (replacedIdx < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "교체 대상 멤버를 덱에서 찾을 수 없습니다. memberId=" + ov.affectedMemberId());
        }

        // 명왕 구성 제약 검증 — 교체 대상 제거 후 나머지 멤버 기준
        List<LoadedMember> remainingWithoutReplaced = members.stream()
                .filter(m -> !m.memberId().equals(ov.affectedMemberId()))
                .toList();
        deckEquipmentValidator.validateMyeongwangComposition(
                toUserDeckMemberList(remainingWithoutReplaced), newMercenary);

        LoadedMember replaced = new LoadedMember(
                ov.affectedMemberId(), newMercenary, List.of(), chars);
        members.set((int) replacedIdx, replaced);

        inputs.put(ov.affectedMemberId(), new MemberDpsInput(
                ov.affectedMemberId(), ov.level(), ov.bonusTarget(), ov.bonusAmount()));

        return new DeckCalculationState(loaded.deckId(), loaded.deck(), members, inputs);
    }

    private DeckCalculationState applyMercenaryAppend(LoadedDeckState loaded,
                                                        MercenaryScenarioOverlay ov,
                                                        Mercenary newMercenary,
                                                        List<UserDeckMemberCharacteristic> chars,
                                                        List<LoadedMember> members,
                                                        Map<Long, MemberDpsInput> inputs) {
        // 명왕 구성 제약 검증 — 기존 멤버 전체 기준
        deckEquipmentValidator.validateMyeongwangComposition(
                toUserDeckMemberList(members), newMercenary);

        // 음수 ID로 가상 멤버 생성 (DB·API 비노출)
        long virtualId = -1L;
        LoadedMember appended = new LoadedMember(virtualId, newMercenary, List.of(), chars);
        members.add(appended);

        inputs.put(virtualId, new MemberDpsInput(
                virtualId, ov.level(), ov.bonusTarget(), ov.bonusAmount()));

        return new DeckCalculationState(loaded.deckId(), loaded.deck(), members, inputs);
    }

    // ── 아이템 overlay ───────────────────────────────────────────────────────

    private DeckCalculationState applyItemOverlay(LoadedDeckState loaded, ItemScenarioOverlay ov) {
        LoadedMember affected = loaded.members().stream()
                .filter(m -> m.memberId().equals(ov.affectedMemberId()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "대상 멤버를 덱에서 찾을 수 없습니다. memberId=" + ov.affectedMemberId()));

        List<UserDeckMemberSlot> newSlots = buildItemSlots(ov, affected);

        List<LoadedMember> newMembers = loaded.members().stream()
                .map(m -> m.memberId().equals(ov.affectedMemberId())
                        ? mergeSlots(m, newSlots) : m)
                .collect(Collectors.toCollection(ArrayList::new));

        return new DeckCalculationState(loaded.deckId(), loaded.deck(), newMembers,
                loaded.memberInputs());
    }

    private List<UserDeckMemberSlot> buildItemSlots(ItemScenarioOverlay ov, LoadedMember affected) {
        if (ov.type() == ScenarioItemType.ITEM_SINGLE) {
            return buildSingleItemSlots(ov, affected);
        }
        return buildSetSlots(ov, affected);
    }

    private List<UserDeckMemberSlot> buildSingleItemSlots(ItemScenarioOverlay ov,
                                                           LoadedMember affected) {
        if (ov.lines() == null || ov.lines().size() != 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "ITEM_SINGLE 시나리오는 lines 1개가 필수입니다.");
        }
        ScenarioLine line = ov.lines().get(0);
        EquipmentItem item = loadItem(line.itemId());

        EquipSlot targetSlot = resolveItemSlot(item);
        deckEquipmentValidator.validateSlotCompatibility(targetSlot, item);
        deckEquipmentValidator.validateMercenaryRestriction(affected.mercenary(), item);

        UserDeckMemberSlot slot = UserDeckMemberSlot.of(null, targetSlot, item);
        applyRitualIfPresent(slot, line.equipmentDetail());
        return List.of(slot);
    }

    private List<UserDeckMemberSlot> buildSetSlots(ItemScenarioOverlay ov,
                                                    LoadedMember affected) {
        List<EquipmentSetPiece> pieces =
                equipmentSetPieceRepository.findWithItemByEquipmentSetId(ov.setId());
        if (pieces.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "세트를 찾을 수 없습니다. setId=" + ov.setId());
        }

        // lines가 있으면 포함 피스만 장착 (부분 세트·피스별 주술)
        Map<Long, ScenarioLine> lineByItemId = Map.of();
        boolean partialMode = ov.lines() != null && !ov.lines().isEmpty();
        if (partialMode) {
            validateSetLines(pieces, ov.lines());
            lineByItemId = ov.lines().stream()
                    .collect(Collectors.toMap(ScenarioLine::itemId, l -> l));
        }

        List<UserDeckMemberSlot> result = new ArrayList<>();
        for (EquipmentSetPiece piece : pieces) {
            EquipmentItem item = piece.getEquipmentItem();
            if (partialMode && !lineByItemId.containsKey(item.getItemId())) {
                continue;
            }

            deckEquipmentValidator.validateMercenaryRestriction(affected.mercenary(), item);

            ScenarioLine line = lineByItemId.get(item.getItemId());

            if (piece.getSlot() == EquipmentSlot.RING || piece.getPieceCount() >= 2) {
                deckEquipmentValidator.validateSlotCompatibility(EquipSlot.RING_1, item);
                deckEquipmentValidator.validateSlotCompatibility(EquipSlot.RING_2, item);
                UserDeckMemberSlot ring1 = UserDeckMemberSlot.of(null, EquipSlot.RING_1, item);
                UserDeckMemberSlot ring2 = UserDeckMemberSlot.of(null, EquipSlot.RING_2, item);
                applyRitualIfPresent(ring1, line != null ? line.equipmentDetail() : null);
                applyRitualIfPresent(ring2, line != null ? line.equipmentDetail() : null);
                result.add(ring1);
                result.add(ring2);
            } else {
                EquipSlot targetSlot = deckEquipmentValidator.resolveSetEquipSlot(
                        piece.getSlot(), item);
                if (targetSlot == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "세트 피스 슬롯 매핑 실패. itemId=" + item.getItemId());
                }
                deckEquipmentValidator.validateSlotCompatibility(targetSlot, item);
                UserDeckMemberSlot slot = UserDeckMemberSlot.of(null, targetSlot, item);
                applyRitualIfPresent(slot, line != null ? line.equipmentDetail() : null);
                result.add(slot);
            }
        }
        return result;
    }

    // ── 유틸 ────────────────────────────────────────────────────────────────

    /** 기존 멤버 슬롯에 overlay 슬롯을 덮어쓴 새 LoadedMember 반환 */
    private LoadedMember mergeSlots(LoadedMember member, List<UserDeckMemberSlot> overlaySlots) {
        Map<EquipSlot, UserDeckMemberSlot> merged = member.slots().stream()
                .collect(Collectors.toMap(UserDeckMemberSlot::getSlot, s -> s));
        overlaySlots.forEach(s -> merged.put(s.getSlot(), s));
        return new LoadedMember(member.memberId(), member.mercenary(),
                List.copyOf(merged.values()), member.characteristics());
    }

    private EquipSlot resolveItemSlot(EquipmentItem item) {
        if (item.getEquipSlot() != null) {
            return item.getEquipSlot();
        }
        EquipSlot fallback = deckEquipmentValidator.fallbackEquipSlot(item.getSlot());
        if (fallback != null) {
            return fallback;
        }
        // RING: 기본 RING_1 배정
        if (item.getSlot() == EquipmentSlot.RING) {
            return EquipSlot.RING_1;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "아이템 슬롯을 결정할 수 없습니다. itemId=" + item.getItemId());
    }

    private EquipmentItem loadItem(Long itemId) {
        return equipmentItemRepository.findWithItemByItemId(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "아이템을 찾을 수 없습니다. itemId=" + itemId));
    }

    private void validateSetLines(List<EquipmentSetPiece> pieces, List<ScenarioLine> lines) {
        if (lines.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "lines가 비어 있습니다. 최소 1개 피스를 포함해야 합니다.");
        }
        java.util.Set<Long> pieceItemIds = pieces.stream()
                .map(p -> p.getEquipmentItem().getItemId())
                .collect(Collectors.toSet());
        java.util.Set<Long> seen = new java.util.HashSet<>();
        for (ScenarioLine line : lines) {
            if (!pieceItemIds.contains(line.itemId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "lines의 itemId(" + line.itemId() + ")가 세트 정의와 불일치합니다.");
            }
            if (!seen.add(line.itemId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "lines에 중복 itemId가 있습니다. itemId=" + line.itemId());
            }
        }
    }

    private void applyRitualIfPresent(UserDeckMemberSlot slot, EquipmentDetailRequest detail) {
        if (detail == null || !detail.hasRitual()) return;
        if (!detail.isRitualConsistent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "hasRitual=true이면 rituals가 1개 이상 필수입니다.");
        }
        RitualResultRequest ritualReq = detail.rituals().get(0);
        Ritual ritual = ritualRepository.findById(ritualReq.ritualId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "주술을 찾을 수 없습니다. ritualId=" + ritualReq.ritualId()));
        RitualOutcome outcome = ritualReq.outcome();
        slot.applyRitual(UserDeckMemberSlotRitual.of(slot, ritual, outcome));
    }

    private List<UserDeckMemberCharacteristic> resolveCharacteristics(
            List<CharacteristicSelection> selections) {
        if (selections == null || selections.isEmpty()) return List.of();
        List<Long> ids = selections.stream().map(CharacteristicSelection::characteristicId).toList();
        Map<Long, MercenaryCharacteristic> charById =
                characteristicRepository.findAllById(ids).stream()
                        .collect(Collectors.toMap(MercenaryCharacteristic::getId, c -> c));
        return selections.stream()
                .map(sel -> {
                    MercenaryCharacteristic ch = charById.get(sel.characteristicId());
                    if (ch == null) {
                        throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "특성을 찾을 수 없습니다. characteristicId=" + sel.characteristicId());
                    }
                    return UserDeckMemberCharacteristic.builder()
                            .characteristic(ch)
                            .selectedLevel(sel.selectedLevel())
                            .build();
                })
                .toList();
    }

    /**
     * LoadedMember 목록을 UserDeckMember 호환 목록으로 변환.
     * DeckEquipmentValidator.validateMyeongwangComposition 시그니처가
     * List<UserDeckMember>를 받으므로 wrapper를 사용한다.
     */
    private List<org.example.gersangtrade.domain.deck.UserDeckMember> toUserDeckMemberList(
            List<LoadedMember> members) {
        return members.stream()
                .map(m -> org.example.gersangtrade.domain.deck.UserDeckMember.builder()
                        .mercenary(m.mercenary())
                        .build())
                .toList();
    }
}
