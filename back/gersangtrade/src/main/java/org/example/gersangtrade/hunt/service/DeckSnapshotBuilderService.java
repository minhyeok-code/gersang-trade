package org.example.gersangtrade.hunt.service;

import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.calculator.dto.request.MemberDpsInput;
import org.example.gersangtrade.calculator.dto.request.ResistanceType;
import org.example.gersangtrade.calculator.dto.response.MemberDpsResult;
import org.example.gersangtrade.deck.dto.response.DeckDetailResponse;
import org.example.gersangtrade.deck.dto.response.DeckMemberResponse;
import org.example.gersangtrade.deck.dto.response.MemberCharacteristicResponse;
import org.example.gersangtrade.deck.service.DeckService;
import org.example.gersangtrade.domain.hunt.DeckSnapshot;
import org.example.gersangtrade.hunt.dto.DeckSnapshotContent;
import org.example.gersangtrade.hunt.repository.DeckSnapshotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * 클리어타임 저장 시 덱 스냅샷 생성·content_hash 기반 재사용.
 */
@Service
@RequiredArgsConstructor
public class DeckSnapshotBuilderService {

    private final DeckService deckService;
    private final DeckSnapshotRepository snapshotRepository;
    private final DeckSnapshotHashUtil hashUtil;

    public record BuildResult(DeckSnapshot snapshot, String contentHash) {}

    @Transactional
    public BuildResult buildOrReuse(Long userId,
                                    Long deckId,
                                    ResistanceType resistanceType,
                                    List<MemberDpsInput> memberInputs,
                                    List<MemberDpsResult> memberDpsResults) {
        DeckDetailResponse detail = deckService.getDeckDetail(userId, deckId);

        List<DeckSnapshotContent.SnapshotMember> members = detail.members().stream()
                .map(member -> toSnapshotMember(userId, deckId, member))
                .toList();

        DeckSnapshotContent content = new DeckSnapshotContent(
                detail.id(),
                detail.name(),
                detail.attrXValue(),
                detail.totalResDown(),
                detail.effects(),
                members,
                new DeckSnapshotContent.DpsContext(
                        resistanceType,
                        memberInputs,
                        toMemberElementValues(memberDpsResults))
        );

        String canonicalJson = hashUtil.toCanonicalJson(content);
        String contentHash = hashUtil.sha256Hex(canonicalJson);

        DeckSnapshot snapshot = snapshotRepository.findByContentHash(contentHash)
                .orElseGet(() -> snapshotRepository.save(new DeckSnapshot(canonicalJson, contentHash)));

        return new BuildResult(snapshot, contentHash);
    }

    private List<DeckSnapshotContent.DpsContext.MemberElementValue> toMemberElementValues(
            List<MemberDpsResult> memberDpsResults) {
        if (memberDpsResults == null || memberDpsResults.isEmpty()) {
            return List.of();
        }
        return memberDpsResults.stream()
                .map(r -> new DeckSnapshotContent.DpsContext.MemberElementValue(r.memberId(), r.elementValue()))
                .toList();
    }

    private DeckSnapshotContent.SnapshotMember toSnapshotMember(Long userId, Long deckId, DeckMemberResponse member) {
        MemberCharacteristicResponse chars =
                deckService.getMemberCharacteristics(userId, deckId, member.id());
        List<DeckSnapshotContent.CharacteristicSelection> selections = chars.characteristics().stream()
                .filter(c -> c.selectedLevel() != null && c.selectedLevel() > 0)
                .map(c -> new DeckSnapshotContent.CharacteristicSelection(c.characteristicId(), c.selectedLevel()))
                .sorted(Comparator.comparing(DeckSnapshotContent.CharacteristicSelection::characteristicId))
                .toList();
        return new DeckSnapshotContent.SnapshotMember(member, selections);
    }
}
