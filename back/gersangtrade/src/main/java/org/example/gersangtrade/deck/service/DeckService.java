package org.example.gersangtrade.deck.service;

import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.catalog.repository.EquipmentItemRepository;
import org.example.gersangtrade.catalog.repository.ItemStatRepository;
import org.example.gersangtrade.catalog.repository.MercenaryRepository;
import org.example.gersangtrade.catalog.repository.MercenaryStatRepository;
import org.example.gersangtrade.catalog.repository.RitualApplicabilityRepository;
import org.example.gersangtrade.catalog.repository.RitualRepository;
import org.example.gersangtrade.deck.dto.request.*;
import org.example.gersangtrade.deck.dto.response.*;
import org.example.gersangtrade.deck.repository.*;
import org.example.gersangtrade.domain.catalog.EquipmentItem;
import org.example.gersangtrade.domain.catalog.Mercenary;
import org.example.gersangtrade.domain.catalog.MercenaryStat;
import org.example.gersangtrade.domain.catalog.Ritual;
import org.example.gersangtrade.domain.catalog.enums.BuffTarget;
import org.example.gersangtrade.domain.catalog.enums.EquipmentKind;
import org.example.gersangtrade.domain.catalog.enums.EquipmentSlot;
import org.example.gersangtrade.domain.catalog.enums.StatType;
import org.example.gersangtrade.domain.deck.*;
import org.example.gersangtrade.domain.deck.enums.EquipSlot;
import org.example.gersangtrade.domain.listing.enums.RitualOutcome;
import org.example.gersangtrade.domain.user.User;
import org.example.gersangtrade.domain.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeckService {

    private static final int MAX_MEMBERS = 12;

    private final UserDeckRepository deckRepository;
    private final UserDeckMemberRepository memberRepository;
    private final UserDeckMemberSlotRepository slotRepository;
    private final UserDeckMemberSlotRitualRepository slotRitualRepository;
    private final UserRepository userRepository;
    private final MercenaryRepository mercenaryRepository;
    private final EquipmentItemRepository equipmentItemRepository;
    private final RitualRepository ritualRepository;
    private final RitualApplicabilityRepository ritualApplicabilityRepository;
    private final MercenaryStatRepository mercenaryStatRepository;
    private final ItemStatRepository itemStatRepository;

    // ── 덱 CRUD ────────────────────────────────────────────────────────────

    @Transactional
    public DeckSummaryResponse createDeck(Long userId, DeckCreateRequest req) {
        User user = userRepository.getReferenceById(userId);
        UserDeck deck = UserDeck.builder()
                .user(user)
                .name(req.name())
                .active(false)
                .build();
        deckRepository.save(deck);
        return DeckSummaryResponse.of(deck, 0);
    }

    @Transactional(readOnly = true)
    public List<DeckSummaryResponse> getMyDecks(Long userId) {
        return deckRepository.findByUserId(userId).stream()
                .map(deck -> DeckSummaryResponse.of(deck, memberRepository.countByDeckId(deck.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public DeckDetailResponse getDeckDetail(Long userId, Long deckId) {
        UserDeck deck = getDeckOrThrow(deckId);
        validateOwner(deck, userId);

        List<DeckMemberResponse> members = memberRepository.findByDeckIdWithMercenary(deckId).stream()
                .map(member -> DeckMemberResponse.of(
                        member,
                        slotRepository.findByDeckMemberIdWithDetails(member.getId())))
                .toList();

        return DeckDetailResponse.of(deck, members);
    }

    @Transactional
    public void updateDeck(Long userId, Long deckId, DeckUpdateRequest req) {
        UserDeck deck = getDeckOrThrow(deckId);
        validateOwner(deck, userId);

        if (req.name() != null) {
            deck.rename(req.name());
        }
        if (Boolean.TRUE.equals(req.active())) {
            deckRepository.findByUserIdAndActiveTrue(userId).ifPresent(UserDeck::deactivate);
            deck.activate();
        } else if (Boolean.FALSE.equals(req.active())) {
            deck.deactivate();
        }
    }

    @Transactional
    public void deleteDeck(Long userId, Long deckId) {
        UserDeck deck = getDeckOrThrow(deckId);
        validateOwner(deck, userId);
        deckRepository.delete(deck);
    }

    // ── 용병 ────────────────────────────────────────────────────────────────

    @Transactional
    public DeckMemberResponse addMember(Long userId, Long deckId, DeckMemberAddRequest req) {
        UserDeck deck = getDeckOrThrow(deckId);
        validateOwner(deck, userId);

        if (memberRepository.countByDeckId(deckId) >= MAX_MEMBERS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "덱에 용병을 최대 " + MAX_MEMBERS + "명까지 추가할 수 있습니다.");
        }
        if (memberRepository.existsByDeckIdAndMercenaryId(deckId, req.mercenaryId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 덱에 추가된 용병입니다.");
        }

        Mercenary mercenary = mercenaryRepository.findById(req.mercenaryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "용병을 찾을 수 없습니다."));

        UserDeckMember member = UserDeckMember.builder().deck(deck).mercenary(mercenary).build();
        memberRepository.save(member);
        return DeckMemberResponse.of(member, List.of());
    }

    @Transactional
    public void removeMember(Long userId, Long deckId, Long memberId) {
        UserDeck deck = getDeckOrThrow(deckId);
        validateOwner(deck, userId);
        memberRepository.delete(getMemberOrThrow(memberId, deckId));
    }

    // ── 용병 스탯 ───────────────────────────────────────────────────────────

    /**
     * 용병 기본 스탯 + 착용 장비(SELF scope) 스탯 합산 조회.
     * 동일 StatType의 여러 스탯(속성별 저항깎 등)은 모두 합산하여 반환한다.
     */
    @Transactional(readOnly = true)
    public MemberStatResponse getMemberStats(Long userId, Long deckId, Long memberId) {
        UserDeck deck = getDeckOrThrow(deckId);
        validateOwner(deck, userId);
        UserDeckMember member = getMemberOrThrow(memberId, deckId);

        // 용병 기본 스탯
        Map<StatType, Integer> baseStatMap = mercenaryStatRepository
                .findByMercenaryId(member.getMercenary().getId()).stream()
                .collect(Collectors.toMap(MercenaryStat::getStatKey, MercenaryStat::getStatValue));

        // 착용 슬롯 목록
        List<UserDeckMemberSlot> slots = slotRepository.findByDeckMemberIdWithDetails(memberId);

        // SELF scope 장비 스탯 합산 (StatType별)
        Map<StatType, Integer> equipStatMap = new HashMap<>();
        if (!slots.isEmpty()) {
            List<Long> itemIds = slots.stream().map(s -> s.getEquipmentItem().getItemId()).toList();
            itemStatRepository.findByItemIdIn(itemIds).stream()
                    .filter(ist -> ist.getScope() == BuffTarget.SELF)
                    .forEach(ist -> equipStatMap.merge(ist.getStatType(), ist.getValue(), Integer::sum));
        }

        // 합산
        Map<StatType, Integer> totalMap = new HashMap<>(baseStatMap);
        equipStatMap.forEach((k, v) -> totalMap.merge(k, v, Integer::sum));

        return MemberStatResponse.of(member, baseStatMap, equipStatMap, totalMap, slots);
    }

    // ── 장비 슬롯 ───────────────────────────────────────────────────────────

    @Transactional
    public void equipSlot(Long userId, Long deckId, Long memberId, EquipSlot slot, SlotEquipRequest req) {
        UserDeck deck = getDeckOrThrow(deckId);
        validateOwner(deck, userId);
        UserDeckMember member = getMemberOrThrow(memberId, deckId);

        EquipmentItem item = equipmentItemRepository.findWithItemByItemId(req.itemId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "아이템을 찾을 수 없습니다."));
        validateSlotCompatibility(slot, item);

        slotRepository.findByDeckMemberIdAndSlot(memberId, slot)
                .ifPresentOrElse(
                        existing -> existing.changeItem(item),
                        () -> slotRepository.save(UserDeckMemberSlot.of(member, slot, item))
                );
    }

    @Transactional
    public void unequipSlot(Long userId, Long deckId, Long memberId, EquipSlot slot) {
        UserDeck deck = getDeckOrThrow(deckId);
        validateOwner(deck, userId);
        getMemberOrThrow(memberId, deckId);

        UserDeckMemberSlot deckSlot = slotRepository.findByDeckMemberIdAndSlot(memberId, slot)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "착용된 아이템이 없습니다."));
        slotRepository.delete(deckSlot);
    }

    // ── 주술 ────────────────────────────────────────────────────────────────

    @Transactional
    public void applyRitual(Long userId, Long deckId, Long memberId, EquipSlot slot, SlotRitualRequest req) {
        UserDeck deck = getDeckOrThrow(deckId);
        validateOwner(deck, userId);
        getMemberOrThrow(memberId, deckId);

        UserDeckMemberSlot deckSlot = slotRepository.findByDeckMemberIdAndSlot(memberId, slot)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "해당 슬롯에 착용된 아이템이 없습니다. 주술 등록 전 아이템을 먼저 착용해주세요."));

        Long itemId = deckSlot.getEquipmentItem().getItemId();
        if (!ritualApplicabilityRepository.existsByRitual_IdAndEquipmentItem_ItemId(req.ritualId(), itemId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "해당 아이템에 적용할 수 없는 주술입니다.");
        }

        Ritual ritual = ritualRepository.findById(req.ritualId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "주술을 찾을 수 없습니다."));

        if (req.outcome() == RitualOutcome.GREAT_SUCCESS && ritual.getGreatSuccessMark() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "해당 주술은 대성공이 없습니다.");
        }

        deckSlot.applyRitual(UserDeckMemberSlotRitual.of(deckSlot, ritual, req.outcome()));
    }

    @Transactional
    public void removeRitual(Long userId, Long deckId, Long memberId, EquipSlot slot) {
        UserDeck deck = getDeckOrThrow(deckId);
        validateOwner(deck, userId);
        getMemberOrThrow(memberId, deckId);

        UserDeckMemberSlot deckSlot = slotRepository.findByDeckMemberIdAndSlot(memberId, slot)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "해당 슬롯을 찾을 수 없습니다."));
        if (deckSlot.getRitual() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "등록된 주술이 없습니다.");
        }
        deckSlot.removeRitual();
    }

    // ── 내부 유틸 ───────────────────────────────────────────────────────────

    private UserDeck getDeckOrThrow(Long deckId) {
        return deckRepository.findById(deckId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "덱을 찾을 수 없습니다."));
    }

    private UserDeckMember getMemberOrThrow(Long memberId, Long deckId) {
        return memberRepository.findByIdAndDeckId(memberId, deckId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "덱 멤버를 찾을 수 없습니다."));
    }

    private void validateOwner(UserDeck deck, Long userId) {
        if (!deck.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인의 덱만 수정할 수 있습니다.");
        }
    }

    private void validateSlotCompatibility(EquipSlot slot, EquipmentItem item) {
        if (slot.name().startsWith("APP_")) {
            if (item.getEquipmentKind() != EquipmentKind.APPEARANCE) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "외변 슬롯에는 외변 아이템만 착용 가능합니다.");
            }
            if (item.getEquipSlot() != slot) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "해당 외변 슬롯에 착용 불가능한 아이템입니다.");
            }
        } else if (slot == EquipSlot.RING_1 || slot == EquipSlot.RING_2) {
            if (item.getSlot() != EquipmentSlot.RING) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "반지 슬롯에는 반지 아이템만 착용 가능합니다.");
            }
        } else {
            if (item.getEquipSlot() != slot) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "해당 슬롯에 착용 불가능한 아이템입니다.");
            }
        }
    }
}
