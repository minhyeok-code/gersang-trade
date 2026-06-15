package org.example.gersangtrade.calculator.service;

import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.calculator.dto.request.MemberDpsInput;
import org.example.gersangtrade.calculator.dto.request.ResistanceType;
import org.example.gersangtrade.calculator.dto.response.DpsResponse;
import org.example.gersangtrade.calculator.dto.response.MemberDpsResult;
import org.example.gersangtrade.calculator.overlay.DeckCalculationState;
import org.example.gersangtrade.calculator.overlay.LoadedMember;
import org.example.gersangtrade.deck.dto.response.DeckMemberResponse;
import org.example.gersangtrade.deck.dto.response.DeckMemberSlotResponse;
import org.example.gersangtrade.domain.deck.UserDeckMemberCharacteristic;
import org.example.gersangtrade.domain.hunt.DeckSnapshot;
import org.example.gersangtrade.hunt.dto.DeckSnapshotContent;
import org.example.gersangtrade.hunt.repository.DeckSnapshotRepository;
import org.example.gersangtrade.hunt.service.DeckSnapshotHashUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 가성비 평가용 after-시나리오 덱 스냅샷 생성·content_hash 기반 재사용.
 *
 * <p>{@link org.example.gersangtrade.hunt.service.DeckSnapshotBuilderService}는
 * 클리어타임 전용이므로 평가 시나리오용 빌더를 별도로 운용한다.
 * 테이블(deck_snapshots)·해시 유틸은 공유한다.</p>
 */
@Service
@RequiredArgsConstructor
public class EvaluationSnapshotBuilder {

    private final DeckSnapshotRepository snapshotRepository;
    private final DeckSnapshotHashUtil hashUtil;

    public record BuildResult(DeckSnapshot snapshot, String contentHash) {}

    /** DB 저장 없이 스냅샷 JSON·해시만 계산 (stale 판별·diff용) */
    public record ContentResult(String canonicalJson, String contentHash) {}

    /**
     * overlay 적용 후 DeckCalculationState + after DPS 결과로 스냅샷을 생성하거나 재사용한다.
     *
     * @param calcState      overlay 적용 완료된 덱 계산 상태
     * @param afterDps       after 시나리오 DPS 계산 결과 (memberElementValues 소스)
     * @param resistanceType 저항 종류 (저항통과율 계산 기준)
     */
    @Transactional
    public BuildResult buildOrReuse(DeckCalculationState calcState,
                                    DpsResponse afterDps,
                                    ResistanceType resistanceType) {
        ContentResult content = buildContent(calcState, afterDps, resistanceType);
        DeckSnapshot snapshot = snapshotRepository.findByContentHash(content.contentHash())
                .orElseGet(() -> snapshotRepository.save(
                        new DeckSnapshot(content.canonicalJson(), content.contentHash())));
        return new BuildResult(snapshot, content.contentHash());
    }

    /** 스냅샷 JSON·해시 계산 — DB write 없음 */
    public ContentResult buildContent(DeckCalculationState calcState,
                                      DpsResponse afterDps,
                                      ResistanceType resistanceType) {
        List<DeckSnapshotContent.SnapshotMember> members = calcState.members().stream()
                .map(m -> toSnapshotMember(m, calcState.memberInputs()))
                .toList();

        DeckSnapshotContent content = new DeckSnapshotContent(
                calcState.deckId(),
                calcState.deck().getName(),
                calcState.deck().getAttrXValue(),
                afterDps.totalResistPierce(),
                null,   // effects — 평가 스냅샷은 덱 효과 메타 생략
                members,
                new DeckSnapshotContent.DpsContext(
                        resistanceType,
                        toSortedMemberInputs(calcState.memberInputs()),
                        toMemberElementValues(afterDps.memberResults()))
        );

        String canonicalJson = hashUtil.toCanonicalJson(content);
        String contentHash = hashUtil.sha256Hex(canonicalJson);
        return new ContentResult(canonicalJson, contentHash);
    }

    // ── 내부 변환 ────────────────────────────────────────────────────────────

    private DeckSnapshotContent.SnapshotMember toSnapshotMember(
            LoadedMember member, Map<Long, MemberDpsInput> memberInputs) {
        MemberDpsInput input = memberInputs.getOrDefault(
                member.memberId(), new MemberDpsInput(member.memberId(), 250, null, 0));

        List<DeckMemberSlotResponse> slotResponses = member.slots().stream()
                .map(DeckMemberSlotResponse::of)
                .toList();

        DeckMemberResponse memberResponse = new DeckMemberResponse(
                member.memberId(),
                member.mercenary().getId(),
                member.mercenary().getName(),
                member.mercenary().getImageUrl(),
                input.level(),
                input.bonusTarget(),
                input.bonusAmount(),
                slotResponses
        );

        List<DeckSnapshotContent.CharacteristicSelection> selections =
                toCharacteristicSelections(member.characteristics());

        return new DeckSnapshotContent.SnapshotMember(memberResponse, selections);
    }

    private List<DeckSnapshotContent.CharacteristicSelection> toCharacteristicSelections(
            List<UserDeckMemberCharacteristic> characteristics) {
        return characteristics.stream()
                .filter(c -> c.getSelectedLevel() != null && c.getSelectedLevel() > 0)
                .map(c -> new DeckSnapshotContent.CharacteristicSelection(
                        c.getCharacteristic().getId(), c.getSelectedLevel()))
                .sorted(Comparator.comparing(DeckSnapshotContent.CharacteristicSelection::characteristicId))
                .toList();
    }

    private List<MemberDpsInput> toSortedMemberInputs(Map<Long, MemberDpsInput> memberInputs) {
        return memberInputs.values().stream()
                .sorted(Comparator.comparing(MemberDpsInput::memberId))
                .toList();
    }

    private List<DeckSnapshotContent.DpsContext.MemberElementValue> toMemberElementValues(
            List<MemberDpsResult> memberDpsResults) {
        if (memberDpsResults == null || memberDpsResults.isEmpty()) return List.of();
        return memberDpsResults.stream()
                .map(r -> new DeckSnapshotContent.DpsContext.MemberElementValue(
                        r.memberId(), r.elementValue()))
                .toList();
    }
}
