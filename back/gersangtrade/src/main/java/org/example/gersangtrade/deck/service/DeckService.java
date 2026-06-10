package org.example.gersangtrade.deck.service;

import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.catalog.repository.DeckBuffSourceRepository;
import org.example.gersangtrade.catalog.repository.EquipmentItemRepository;
import org.example.gersangtrade.catalog.repository.EquipmentSetEffectRepository;
import org.example.gersangtrade.catalog.repository.EquipmentSetPieceRepository;
import org.example.gersangtrade.catalog.repository.ItemStatRepository;
import org.example.gersangtrade.catalog.repository.MercenaryCharacteristicLevelRepository;
import org.example.gersangtrade.catalog.repository.MercenaryCharacteristicRepository;
import org.example.gersangtrade.catalog.repository.MercenaryRepository;
import org.example.gersangtrade.catalog.repository.MercenaryStatRepository;
import org.example.gersangtrade.catalog.repository.RitualApplicabilityRepository;
import org.example.gersangtrade.catalog.repository.RitualRepository;
import org.example.gersangtrade.catalog.repository.RitualSetEffectRepository;
import org.example.gersangtrade.catalog.repository.RitualStatRepository;
import org.example.gersangtrade.catalog.repository.SpiritRepository;
import org.example.gersangtrade.domain.catalog.DeckBuff;
import org.example.gersangtrade.domain.catalog.DeckBuffSource;
import org.example.gersangtrade.domain.catalog.RitualStat;
import org.example.gersangtrade.domain.catalog.Spirit;
import org.example.gersangtrade.domain.catalog.SpiritBuff;
import org.example.gersangtrade.deck.dto.request.*;
import org.example.gersangtrade.deck.dto.response.*;
import org.example.gersangtrade.deck.repository.*;
import org.example.gersangtrade.domain.catalog.EquipmentSet;
import org.example.gersangtrade.domain.catalog.EquipmentSetEffect;
import org.example.gersangtrade.domain.catalog.EquipmentSetPiece;
import org.example.gersangtrade.domain.catalog.EquipmentItem;
import org.example.gersangtrade.domain.catalog.ItemStat;
import org.example.gersangtrade.domain.catalog.Mercenary;
import org.example.gersangtrade.domain.catalog.MercenaryCharacteristic;
import org.example.gersangtrade.domain.catalog.MercenaryCharacteristicLevel;
import org.example.gersangtrade.domain.catalog.MercenaryStat;
import org.example.gersangtrade.domain.catalog.Ritual;
import org.example.gersangtrade.calculator.service.DexterityCriticalChanceCalculator;
import org.example.gersangtrade.calculator.service.CharacteristicScopeResolver;
import org.example.gersangtrade.calculator.service.CharacteristicScopeResolver.ApplicationMode;
import org.example.gersangtrade.calculator.service.CharacteristicScopeResolver.ScopedEffect;
import org.example.gersangtrade.calculator.service.AwakenedMyungwangBuffCalculator;
import org.example.gersangtrade.calculator.service.GahoBuffCalculator;
import org.example.gersangtrade.calculator.service.GonmyeongBuffCalculator;
import org.example.gersangtrade.calculator.service.LegendGeneralBuffCalculator;
import org.example.gersangtrade.calculator.service.MemberBuildStatCalculator;
import org.example.gersangtrade.calculator.service.BudongMyungwangWeaponTransferCalculator;
import org.example.gersangtrade.calculator.service.MyungwangWeaponElementShareCalculator;
import org.example.gersangtrade.calculator.service.MyungwangStatTransferCalculator;
import org.example.gersangtrade.calculator.service.PlayerCharacterBuffCalculator;
import org.example.gersangtrade.calculator.service.PlayerCharacterStatResolver;
import org.example.gersangtrade.catalog.service.LegendGeneralLoadService;
import org.example.gersangtrade.domain.catalog.LegendGeneral;
import org.example.gersangtrade.domain.catalog.LegendGeneralCharacteristic;
import org.example.gersangtrade.domain.catalog.enums.BuffTarget;
import org.example.gersangtrade.domain.catalog.enums.BuffValueType;
import org.example.gersangtrade.domain.catalog.enums.DeckBuffSourceType;
import org.example.gersangtrade.domain.catalog.enums.Element;
import org.example.gersangtrade.domain.catalog.enums.EquipmentKind;
import org.example.gersangtrade.domain.catalog.enums.EquipmentSlot;
import org.example.gersangtrade.domain.catalog.enums.CharacteristicApplyType;
import org.example.gersangtrade.domain.catalog.enums.MercenaryCategory;
import org.example.gersangtrade.domain.catalog.enums.Nature;
import org.example.gersangtrade.domain.catalog.enums.SpiritGrade;
import org.example.gersangtrade.domain.catalog.enums.StatUnit;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeckService {

    private static final int MAX_MEMBERS = 12;
    private static final int PROTAGONIST_CHARACTERISTIC_POINTS = 16;
    private static final int DEFAULT_CHARACTERISTIC_POINTS = 17;

    private final UserDeckRepository deckRepository;
    private final UserDeckMemberRepository memberRepository;
    private final UserDeckMemberSlotRepository slotRepository;
    private final UserDeckMemberSlotRitualRepository slotRitualRepository;
    private final UserDeckMemberCharacteristicRepository memberCharacteristicRepository;
    private final UserDeckMemberEquipRepository memberEquipRepository;
    private final UserRepository userRepository;
    private final MercenaryRepository mercenaryRepository;
    private final EquipmentItemRepository equipmentItemRepository;
    private final EquipmentSetPieceRepository equipmentSetPieceRepository;
    private final EquipmentSetEffectRepository equipmentSetEffectRepository;
    private final RitualRepository ritualRepository;
    private final RitualApplicabilityRepository ritualApplicabilityRepository;
    private final MercenaryStatRepository mercenaryStatRepository;
    private final ItemStatRepository itemStatRepository;
    private final MercenaryCharacteristicRepository characteristicRepository;
    private final MercenaryCharacteristicLevelRepository characteristicLevelRepository;
    private final RitualStatRepository ritualStatRepository;
    private final RitualSetEffectRepository ritualSetEffectRepository;
    private final SpiritRepository spiritRepository;
    private final DeckBuffSourceRepository deckBuffSourceRepository;
    private final LegendGeneralLoadService legendGeneralLoadService;
    private final LegendGeneralBuffCalculator legendGeneralBuffCalculator;
    private final MyungwangStatTransferCalculator myungwangStatTransferCalculator;
    private final BudongMyungwangWeaponTransferCalculator budongMyungwangWeaponTransferCalculator;
    private final MyungwangWeaponElementShareCalculator myungwangWeaponElementShareCalculator;
    private final MemberBuildStatCalculator memberBuildStatCalculator;
    private final PlayerCharacterBuffCalculator playerCharacterBuffCalculator;
    private final AwakenedMyungwangBuffCalculator awakenedMyungwangBuffCalculator;
    private final GonmyeongBuffCalculator gonmyeongBuffCalculator;
    private final GahoBuffCalculator gahoBuffCalculator;
    private final PlayerCharacterStatResolver playerCharacterStatResolver;
    private final DeckEquipmentValidator deckEquipmentValidator;

    /** 땅 전설 정령의 2배 효과에서 제외되는 속도 계열 스탯 */
    private static final Set<StatType> SPIRIT_DOUBLE_EXCLUDED_STATS = Set.of(
            StatType.ATTACK_SPEED,
            StatType.MOVE_SPEED
    );

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

        return DeckDetailResponse.of(deck, buildDeckEffectResponse(deck), members);
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
        for (UserDeckMember member : memberRepository.findByDeckIdWithMercenary(deckId)) {
            purgeMemberRelatedData(member.getId());
            memberRepository.delete(member);
        }
        deckRepository.delete(deck);
    }

    // ── 덱 단위 효과 ────────────────────────────────────────────────────────

    /** 정령/진법/층진 선택지 목록 조회. */
    @Transactional(readOnly = true)
    public DeckEffectCatalogResponse getDeckEffectCatalog() {
        return new DeckEffectCatalogResponse(
                spiritRepository.findAll().stream()
                        .sorted(Comparator.comparing(Spirit::getId))
                        .map(DeckEffectResponse.SpiritEntry::of)
                        .toList(),
                deckBuffSourceRepository.findBySourceType(DeckBuffSourceType.JINBEOP).stream()
                        .sorted(Comparator.comparing(DeckBuffSource::getId))
                        .map(DeckEffectResponse.DeckBuffSourceEntry::of)
                        .toList(),
                deckBuffSourceRepository.findBySourceType(DeckBuffSourceType.CHEUNGJIN).stream()
                        .sorted(Comparator.comparing(DeckBuffSource::getId))
                        .map(DeckEffectResponse.DeckBuffSourceEntry::of)
                        .toList()
        );
    }

    /** 덱에 적용할 정령/진법/층진을 저장한다. */
    @Transactional
    public DeckEffectResponse updateDeckEffects(Long userId, Long deckId, DeckEffectUpdateRequest req) {
        UserDeck deck = getDeckOrThrow(deckId);
        validateOwner(deck, userId);

        if (req.spirit1Id() != null && req.spirit1Id().equals(req.spirit2Id())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "같은 정령을 중복 적용할 수 없습니다.");
        }

        Spirit spirit1 = req.spirit1Id() != null ? getSpiritOrThrow(req.spirit1Id()) : null;
        Spirit spirit2 = req.spirit2Id() != null ? getSpiritOrThrow(req.spirit2Id()) : null;
        DeckBuffSource jinbeop = req.jinbeopSourceId() != null
                ? getDeckBuffSourceOrThrow(req.jinbeopSourceId(), DeckBuffSourceType.JINBEOP)
                : null;
        DeckBuffSource cheungjin = req.cheungjinSourceId() != null
                ? getDeckBuffSourceOrThrow(req.cheungjinSourceId(), DeckBuffSourceType.CHEUNGJIN)
                : null;

        if (req.gonmyeongLevel() != null && (req.gonmyeongLevel() < 1 || req.gonmyeongLevel() > 30)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "공명 레벨은 1~30 범위여야 합니다.");
        }
        if (req.gahoLevel() != null && (req.gahoLevel() < 1 || req.gahoLevel() > 30)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "가호 레벨은 1~30 범위여야 합니다.");
        }

        deck.updateEffects(spirit1, spirit2, jinbeop, cheungjin, req.gonmyeongLevel(), req.gahoLevel());
        return buildDeckEffectResponse(deck);
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

        List<UserDeckMember> currentMembers = memberRepository.findByDeckIdWithMercenary(deckId);
        deckEquipmentValidator.validateMyeongwangComposition(currentMembers, mercenary);

        UserDeckMember member = UserDeckMember.builder().deck(deck).mercenary(mercenary).build();
        memberRepository.save(member);
        return DeckMemberResponse.of(member, List.of());
    }

    @Transactional
    public void removeMember(Long userId, Long deckId, Long memberId) {
        UserDeck deck = getDeckOrThrow(deckId);
        validateOwner(deck, userId);
        UserDeckMember member = getMemberOrThrow(memberId, deckId);
        purgeMemberRelatedData(memberId);
        memberRepository.delete(member);
    }

    // ── 용병 스탯 ───────────────────────────────────────────────────────────

    /**
     * 덱 전체 멤버의 속성값(ELEMENT_VALUE)을 한 번에 계산 — DPS 없이 카드에 표시하기 위해 사용.
     * computeMemberStatComponents를 멤버당 1회만 호출하고 명왕 이전을 직접 배치 계산하여 O(n) 유지.
     */
    @Transactional(readOnly = true)
    public List<MemberElementValueResponse> getMemberElementValues(Long userId, Long deckId) {
        UserDeck deck = getDeckOrThrow(deckId);
        validateOwner(deck, userId);

        List<UserDeckMember> allMembers = memberRepository.findByDeckIdWithMercenary(deckId);
        if (allMembers.isEmpty()) return List.of();

        // ① 멤버별 preTransferTotal 계산 (명왕 이전 전 합산)
        Map<Long, Map<StatType, Integer>> preTotals = new HashMap<>();
        for (UserDeckMember m : allMembers) {
            preTotals.put(m.getId(), computeMemberStatComponents(deck, m).preTransferTotal());
        }

        // ② 명왕 스탯 이전 — preTotals 재사용으로 computeMemberStatComponents 중복 호출 방지
        List<Long> memberIds = allMembers.stream().map(UserDeckMember::getId).toList();
        List<UserDeckMemberCharacteristic> allChars = memberCharacteristicRepository.findByDeckMemberIdIn(memberIds);
        Map<Long, List<UserDeckMemberCharacteristic>> charsByMemberId = allChars.stream()
                .collect(Collectors.groupingBy(c -> c.getDeckMember().getId()));
        List<Long> characteristicIds = allChars.stream().map(c -> c.getCharacteristic().getId()).distinct().toList();
        Map<Long, List<MercenaryCharacteristicLevel>> charLevelsByCharId = characteristicIds.isEmpty() ? Map.of()
                : characteristicLevelRepository.findByCharacteristicIdIn(characteristicIds).stream()
                        .collect(Collectors.groupingBy(l -> l.getCharacteristic().getId()));
        MyungwangStatTransferCalculator.ComputedTransfers transfers =
                myungwangStatTransferCalculator.computeReceivedTransfers(
                        allMembers, charsByMemberId, charLevelsByCharId, preTotals);

        // ③ 파티 버프 공통 데이터 (1회 로드)
        List<UserDeckMemberSlot> allSlots = slotRepository.findByDeckMemberIdIn(memberIds);
        List<Long> allItemIds = allSlots.stream().map(s -> s.getEquipmentItem().getItemId()).toList();
        List<ItemStat> allyElementStats = allItemIds.isEmpty() ? List.of()
                : itemStatRepository.findByItemIdIn(allItemIds).stream()
                        .filter(ist -> ist.getScope() == BuffTarget.ALLY
                                && ist.getStatType() == StatType.ELEMENT_VALUE)
                        .toList();

        long awakenedCount = allMembers.stream()
                .filter(m -> m.getMercenary().getCategory() == MercenaryCategory.MYEONG_KING_AWAKENING).count();
        AwakenedMyungwangBuffCalculator.AllyElementBuff awakenedAllyBuff =
                awakenedCount > 0 ? awakenedMyungwangBuffCalculator.getCommonAllyBuff() : null;

        Mercenary protagonistMerc = allMembers.stream()
                .filter(m -> m.getMercenary().getCategory() == MercenaryCategory.PROTAGONIST)
                .map(UserDeckMember::getMercenary)
                .findFirst().orElse(null);
        PlayerCharacterBuffCalculator.NationBuff nationBuff =
                protagonistMerc != null ? playerCharacterBuffCalculator.getNationBuff(protagonistMerc.getNation()) : null;

        MyungwangWeaponElementShareCalculator.ComputedShares awakenedWeaponShares =
                computeAwakenedWeaponElementShares(deck);

        // ④ 멤버별 ELEMENT_VALUE 합산
        List<MemberElementValueResponse> results = new ArrayList<>();
        for (UserDeckMember member : allMembers) {
            int ev = preTotals.get(member.getId()).getOrDefault(StatType.ELEMENT_VALUE, 0);
            ev += transfers.receivedByMemberId().getOrDefault(member.getId(), Map.of())
                    .getOrDefault(StatType.ELEMENT_VALUE, 0);

            Nature nature = member.getMercenary().getNature();
            if (nature != null) {
                // 주인공 국가 속성 버프
                if (nationBuff != null && nationBuff.value() > 0
                        && nationBuff.element() != Element.NONE
                        && nationBuff.element().name().equals(nature.name())) {
                    ev += Math.round(nationBuff.value());
                }
                // 각성 명왕 ALLY 속성값 버프 (EARTH는 2, 그 외 5 — 대체 관계)
                if (awakenedAllyBuff != null) {
                    float perBuff = nature == Nature.EARTH
                            ? awakenedAllyBuff.earthValue()
                            : awakenedAllyBuff.defaultValue();
                    ev += (int) (Math.round(perBuff) * awakenedCount);
                }
                // ALLY scope 장비 ELEMENT_VALUE
                for (ItemStat ist : allyElementStats) {
                    if (isDeckBuffElementApplicable(ist.getElement(), nature)) {
                        ev += ist.getValue();
                    }
                }
                // 명왕부·일반/고급 명왕 무기 → 동속성 사천왕
                ev += computeHeavenlyKingElementValueForMember(allMembers, allSlots, member);
                // 각성 명왕 무기 % 공유
                ev += awakenedWeaponShares.receivedElementValueByMemberId()
                        .getOrDefault(member.getId(), 0);
                // 전설장수 ALLY ELEMENT_VALUE
                ev += computeLgAllyContributions(allMembers, member)
                        .statMap().getOrDefault(StatType.ELEMENT_VALUE, 0);
            }
            results.add(new MemberElementValueResponse(member.getId(), ev));
        }
        return results;
    }

    /**
     * 용병 기본 스탯 + 착용 장비(SELF scope) + 선택 특성 스탯 합산 조회.
     * 동일 StatType의 여러 스탯은 모두 합산하여 반환한다.
     */
    @Transactional(readOnly = true)
    public MemberStatResponse getMemberStats(Long userId, Long deckId, Long memberId) {
        UserDeck deck = getDeckOrThrow(deckId);
        validateOwner(deck, userId);
        UserDeckMember member = getMemberOrThrow(memberId, deckId);

        MemberStatComponents components = computeMemberStatComponents(deck, member);
        MyungwangStatTransferCalculator.ComputedTransfers transfers = computeMyungwangTransfers(deck);
        BudongMyungwangWeaponTransferCalculator.ComputedTransfers budongTransfers =
                computeBudongWeaponTransfers(deck);
        MyungwangWeaponElementShareCalculator.ComputedShares awakenedWeaponShares =
                computeAwakenedWeaponElementShares(deck);
        Map<StatType, Integer> transferStatMap = new HashMap<>(
                transfers.receivedByMemberId().getOrDefault(memberId, Map.of()));
        budongTransfers.receivedByMemberId()
                .getOrDefault(memberId, Map.of())
                .forEach((k, v) -> transferStatMap.merge(k, v, Integer::sum));
        if (awakenedWeaponShares.receivedElementValueByMemberId().containsKey(memberId)) {
            transferStatMap.merge(StatType.ELEMENT_VALUE,
                    awakenedWeaponShares.receivedElementValueByMemberId().get(memberId), Integer::sum);
        }
        List<MemberStatResponse.MyungwangTransferDetail> transferDetails = new ArrayList<>();
        transfers.detailsByMemberId().getOrDefault(memberId, List.of()).stream()
                .map(d -> new MemberStatResponse.MyungwangTransferDetail(
                        d.sourceMercenaryName(), d.statType(), d.value()))
                .forEach(transferDetails::add);
        budongTransfers.detailsByMemberId().getOrDefault(memberId, List.of()).stream()
                .map(d -> new MemberStatResponse.MyungwangTransferDetail(
                        d.sourceLabel(), d.statType(), d.value()))
                .forEach(transferDetails::add);
        awakenedWeaponShares.detailsByMemberId().getOrDefault(memberId, List.of()).stream()
                .map(d -> new MemberStatResponse.MyungwangTransferDetail(
                        d.sourceLabel(), StatType.ELEMENT_VALUE, d.value()))
                .forEach(transferDetails::add);
        Map<StatType, Integer> totalMap = new HashMap<>(components.preTransferTotal());
        transferStatMap.forEach((k, v) -> totalMap.merge(k, v, Integer::sum));

        // 전체 멤버 로드 (주인공·각성명왕 버프 계산에 필요)
        List<UserDeckMember> allMembers = memberRepository.findByDeckIdWithMercenary(deckId);
        Map<StatType, Integer> protagonistBuffStatMap = new HashMap<>();
        Map<StatType, Integer> awakenedMyeongwangBuffStatMap = new HashMap<>();

        // 주인공 국가 속성 버프 — 해당 속성 아군 ELEMENT_VALUE 가산
        allMembers.stream()
                .filter(m -> m.getMercenary().getCategory() == MercenaryCategory.PROTAGONIST)
                .findFirst()
                .ifPresent(protagonist -> {
                    var nationBuff = playerCharacterBuffCalculator.getNationBuff(
                            protagonist.getMercenary().getNation());
                    if (nationBuff.value() > 0 && nationBuff.element() != Element.NONE
                            && nationBuff.element().name().equals(
                                    member.getMercenary().getNature().name())) {
                        int value = Math.round(nationBuff.value());
                        protagonistBuffStatMap.merge(StatType.ELEMENT_VALUE, value, Integer::sum);
                        totalMap.merge(StatType.ELEMENT_VALUE, value, Integer::sum);
                    }
                });

        // 각성 명왕 N명 × 아군 속성값 버프 (EARTH는 2, 그 외 5 — 대체 관계, 중첩 아님)
        long awakenedMyeongwangCount = allMembers.stream()
                .filter(m -> m.getMercenary().getCategory() == MercenaryCategory.MYEONG_KING_AWAKENING)
                .count();
        if (awakenedMyeongwangCount > 0) {
            var allyBuff = awakenedMyungwangBuffCalculator.getCommonAllyBuff();
            float perBuff = member.getMercenary().getNature() == Nature.EARTH
                    ? allyBuff.earthValue()
                    : allyBuff.defaultValue();
            int bonus = (int) (Math.round(perBuff) * awakenedMyeongwangCount);
            awakenedMyeongwangBuffStatMap.merge(StatType.ELEMENT_VALUE, bonus, Integer::sum);
            totalMap.merge(StatType.ELEMENT_VALUE, bonus, Integer::sum);
        }

        // 사인검 등 ALLY scope 장비 버프 — 덱 전체 장비 중 scope=ALLY이고 element가 일치하는 스탯을 이 멤버에게 적용
        Map<StatType, Integer> partyItemBuffStatMap = new HashMap<>();
        List<Long> allMemberIds = allMembers.stream().map(UserDeckMember::getId).toList();
        List<UserDeckMemberSlot> allSlots = slotRepository.findByDeckMemberIdIn(allMemberIds);
        if (!allSlots.isEmpty()) {
            List<Long> allItemIds = allSlots.stream().map(s -> s.getEquipmentItem().getItemId()).toList();
            itemStatRepository.findByItemIdIn(allItemIds).stream()
                    .filter(ist -> ist.getScope() == BuffTarget.ALLY)
                    .filter(ist -> isDeckBuffElementApplicable(ist.getElement(), member.getMercenary().getNature()))
                    .forEach(ist -> {
                        partyItemBuffStatMap.merge(ist.getStatType(), ist.getValue(), Integer::sum);
                        totalMap.merge(ist.getStatType(), ist.getValue(), Integer::sum);
                    });
        }

        // 명왕부·일반/고급 명왕 무기 — 동속성 사천왕 속성값 버프
        int heavenlyKingEv = computeHeavenlyKingElementValueForMember(allMembers, allSlots, member);
        if (heavenlyKingEv > 0) {
            totalMap.merge(StatType.ELEMENT_VALUE, heavenlyKingEv, Integer::sum);
        }

        // 전설장수 ALLY 속성 버프 — 다른 멤버의 속성 데미지 증가 등을 이 멤버에게 적용
        LgAllyContribution lgAlly = computeLgAllyContributions(allMembers, member);
        lgAlly.statMap().forEach((k, v) -> totalMap.merge(k, v, Integer::sum));

        // 민첩 1000당 크리티컬확률 2%p — 최종 스탯 CRITICAL_CHANCE에 반영
        DexterityCriticalChanceCalculator.applyToStats(totalMap);

        return MemberStatResponse.of(member, components.baseStatMap(), components.equipStatMap(),
                components.setEffectStatMap(), components.characteristicStatMap(),
                components.partyCharacteristicStatMap(), components.enemyDebuffStatMap(),
                components.ritualStatMap(), components.ritualSetEffectStatMap(), components.deckBuffStatMap(),
                components.deckBuffDetails(), components.levelBonusStatMap(), components.bonusStatMap(),
                protagonistBuffStatMap, awakenedMyeongwangBuffStatMap, partyItemBuffStatMap,
                components.resolvedMainStat(),
                transferStatMap, transferDetails, components.activeEquipmentSetEffects(),
                components.activeSetEffects(), totalMap, lgAlly.displayMap(), lgAlly.details(), components.slots());
    }

    private record LgAllyContribution(
            Map<StatType, Integer> statMap,                          // 스탯 합산용 (StatType 키)
            Map<String, Integer> displayMap,                         // 표시용 compound 키 (예: DAMAGE_PERCENT_FIRE)
            List<MemberStatResponse.LgAllyDetail> details            // 장수별 상세 내역
    ) {}

    /**
     * 전설장수 멤버들의 ALLY 속성 버프를 수신 멤버에게 배분 계산.
     * NONE 원소: 전원 적용. 특정 원소: Nature 일치 멤버에게만 적용.
     */
    private LgAllyContribution computeLgAllyContributions(
            List<UserDeckMember> allMembers, UserDeckMember receiver) {
        Map<StatType, Integer> statMap = new HashMap<>();
        Map<String, Integer> displayMap = new HashMap<>();
        List<MemberStatResponse.LgAllyDetail> details = new ArrayList<>();

        Nature receiverNature = receiver.getMercenary().getNature();

        for (UserDeckMember provider : allMembers) {
            if (provider.getId().equals(receiver.getId())) continue;
            if (provider.getMercenary().getCategory() != MercenaryCategory.LEGENDARY_GENERAL) continue;

            LegendGeneral lg = legendGeneralLoadService.loadForCalculation(provider.getMercenary().getId())
                    .orElse(null);
            if (lg == null) continue;

            String providerName = provider.getMercenary().getName();

            // 이 멤버가 선택한 특성 레벨 → characteristicIndex 맵
            List<UserDeckMemberCharacteristic> providerChars =
                    memberCharacteristicRepository.findByDeckMemberIdIn(List.of(provider.getId()));

            List<MercenaryCharacteristic> stubs = characteristicRepository
                    .findByMercenaryId(provider.getMercenary().getId()).stream()
                    .sorted(Comparator.comparing(MercenaryCharacteristic::getId))
                    .toList();
            Map<Long, Integer> charIndexByCharId = new HashMap<>();
            for (int i = 0; i < stubs.size(); i++) {
                charIndexByCharId.put(stubs.get(i).getId(), i);
            }

            Map<Integer, Integer> pointsMap = new HashMap<>();
            for (UserDeckMemberCharacteristic mc : providerChars) {
                Integer idx = charIndexByCharId.get(mc.getCharacteristic().getId());
                if (idx != null) pointsMap.put(idx, mc.getSelectedLevel());
            }

            // ALLY/ENEMY 효과 계산 (패시브 + 특성)
            legendGeneralBuffCalculator.calculate(lg, provider.getLevel(), pointsMap)
                    .forEach((key, value) -> {
                        if (key.target() != BuffTarget.ALLY || key.statType() == null) return;
                        // 원소 필터: NONE은 전원, 특정 원소는 Nature 일치 시만
                        if (key.element() != Element.NONE
                                && !key.element().name().equals(receiverNature.name())) return;

                        int rounded = Math.round(value);
                        statMap.merge(key.statType(), rounded, Integer::sum);

                        String displayKey = key.element() != Element.NONE
                                ? key.statType().name() + "_" + key.element().name()
                                : key.statType().name();
                        displayMap.merge(displayKey, rounded, Integer::sum);
                        details.add(new MemberStatResponse.LgAllyDetail(providerName, displayKey, rounded));
                    });
        }
        return new LgAllyContribution(statMap, displayMap, details);
    }

    /** 이전·합산 전 멤버별 스탯 구성요소 (명왕 스탯 이전 계산의 소스 스탯으로 사용) */
    private MemberStatComponents computeMemberStatComponents(UserDeck deck, UserDeckMember member) {
        Long memberId = member.getId();

        // 용병 기본 스탯
        Map<StatType, Integer> baseStatMap = mercenaryStatRepository
                .findByMercenaryId(member.getMercenary().getId()).stream()
                .collect(Collectors.toMap(MercenaryStat::getStatKey, MercenaryStat::getStatValue));

        // 착용 슬롯 목록
        List<UserDeckMemberSlot> slots = slotRepository.findByDeckMemberIdWithDetails(memberId);

        Nature memberNature = member.getMercenary().getNature();

        // SELF scope 장비 스탯 합산 — element 속성이 지정된 경우 용병 속성과 일치할 때만 적용
        Map<StatType, Integer> equipStatMap = new HashMap<>();
        if (!slots.isEmpty()) {
            List<Long> itemIds = slots.stream().map(s -> s.getEquipmentItem().getItemId()).toList();
            itemStatRepository.findByItemIdIn(itemIds).stream()
                    .filter(ist -> ist.getScope() == BuffTarget.SELF)
                    .filter(ist -> isDeckBuffElementApplicable(ist.getElement(), memberNature))
                    .forEach(ist -> equipStatMap.merge(ist.getStatType(), ist.getValue(), Integer::sum));
        }
        Map<StatType, Integer> setEffectStatMap = new HashMap<>();
        List<MemberStatResponse.ActiveEquipmentSetEffect> activeEquipmentSetEffects = new ArrayList<>();
        accumulateEquipmentSetEffects(slots, memberNature, setEffectStatMap, activeEquipmentSetEffects);

        // 선택 특성 — scope(SELF/ALLY/ENEMY)별 분류
        List<UserDeckMemberCharacteristic> memberCharacteristics =
                memberCharacteristicRepository.findByDeckMemberIdIn(List.of(memberId));
        Map<StatType, Integer> characteristicStatMap = new HashMap<>();
        Map<StatType, Integer> partyCharacteristicStatMap = new HashMap<>();
        Map<StatType, Integer> enemyDebuffStatMap = new HashMap<>();

        if (!memberCharacteristics.isEmpty()) {
            List<Long> characteristicIds = memberCharacteristics.stream()
                    .map(c -> c.getCharacteristic().getId()).toList();
            Map<Long, Integer> selectedLevelMap = memberCharacteristics.stream()
                    .collect(Collectors.toMap(c -> c.getCharacteristic().getId(),
                            UserDeckMemberCharacteristic::getSelectedLevel));

            List<MercenaryCharacteristic> mercCharacteristics =
                    characteristicRepository.findByMercenaryId(member.getMercenary().getId()).stream()
                            .sorted(Comparator.comparing(MercenaryCharacteristic::getId))
                            .toList();
            Map<Long, Integer> charIndexByCharId = new HashMap<>();
            for (int i = 0; i < mercCharacteristics.size(); i++) {
                charIndexByCharId.put(mercCharacteristics.get(i).getId(), i);
            }

            characteristicLevelRepository.findByCharacteristicIdIn(characteristicIds).stream()
                    .filter(cl -> cl.getAmountValue() != null)
                    .forEach(cl -> {
                        MercenaryCharacteristic parent = cl.getCharacteristic();
                        int selectedLevel = selectedLevelMap.getOrDefault(parent.getId(), 0);
                        if (!cl.getLevel().equals(selectedLevel)) return;

                        ScopedEffect scoped = CharacteristicScopeResolver.resolve(cl);
                        if (scoped == null || scoped.mode() != ApplicationMode.STAT || cl.getStatType() == null) return;

                        int value = Math.round(cl.getAmountValue());
                        Map<StatType, Integer> targetMap = switch (scoped.target()) {
                            case SELF -> characteristicStatMap;
                            case ALLY -> partyCharacteristicStatMap;
                            case ENEMY -> enemyDebuffStatMap;
                            // 명왕부·각성무기 scope는 특성에서 발생하지 않으므로 ALLY 버킷으로 fallback
                            case ALLY_HEAVENLY_KING, ALLY_SAME_ELEMENT -> partyCharacteristicStatMap;
                        };
                        targetMap.merge(cl.getStatType(), value, Integer::sum);
                    });

            if (member.getMercenary().getCategory() == MercenaryCategory.LEGENDARY_GENERAL) {
                legendGeneralLoadService.loadForCalculation(member.getMercenary().getId())
                        .ifPresent(lg -> accumulateLegendGeneralScopedStats(
                                lg, charIndexByCharId, memberCharacteristics, member.getLevel(),
                                characteristicStatMap, partyCharacteristicStatMap, enemyDebuffStatMap));
            }
        }

        // SELF_AUTO 특성 자동 적용 (user_deck_member_characteristics에 저장되지 않으므로 별도 처리)
        List<MercenaryCharacteristic> selfAutoChars = characteristicRepository
                .findByMercenaryId(member.getMercenary().getId()).stream()
                .filter(c -> c.getApplyType() == CharacteristicApplyType.SELF_AUTO)
                .toList();
        if (!selfAutoChars.isEmpty()) {
            List<Long> autoCharIds = selfAutoChars.stream().map(MercenaryCharacteristic::getId).toList();
            characteristicLevelRepository.findByCharacteristicIdIn(autoCharIds).stream()
                    .filter(cl -> cl.getAmountValue() != null)
                    .forEach(cl -> {
                        ScopedEffect scoped = CharacteristicScopeResolver.resolve(cl);
                        if (scoped == null || scoped.mode() != ApplicationMode.STAT || cl.getStatType() == null) return;
                        characteristicStatMap.merge(cl.getStatType(), Math.round(cl.getAmountValue()), Integer::sum);
                    });
        }

        // 주술 스탯 합산 (슬롯별 주술 outcome에 따라 RitualStat 조회)
        Map<StatType, Integer> ritualStatMap = new HashMap<>();
        List<MemberStatResponse.ActiveSetEffect> activeSetEffects = new ArrayList<>();
        Map<StatType, Integer> ritualSetEffectStatMap = new HashMap<>();

        List<UserDeckMemberSlot> slotsWithRitual = slots.stream()
                .filter(s -> s.getRitual() != null)
                .toList();

        if (!slotsWithRitual.isEmpty()) {
            List<Long> ritualIds = slotsWithRitual.stream()
                    .map(s -> s.getRitual().getRitual().getId())
                    .distinct().toList();

            Map<Long, List<RitualStat>> ritualStatsByRitualId = ritualStatRepository
                    .findByRitualIdIn(ritualIds).stream()
                    .collect(Collectors.groupingBy(rs -> rs.getRitual().getId()));

            for (UserDeckMemberSlot s : slotsWithRitual) {
                Long ritualId = s.getRitual().getRitual().getId();
                RitualOutcome outcome = s.getRitual().getOutcome();
                ritualStatsByRitualId.getOrDefault(ritualId, List.of()).stream()
                        .filter(rs -> rs.getOutcome() == outcome)
                        .filter(rs -> isDeckBuffElementApplicable(rs.getElement(), memberNature))
                        .forEach(rs -> ritualStatMap.merge(rs.getStatType(), rs.getStatValue(), Integer::sum));
            }

            Map<String, Integer> pieceCountMap = new HashMap<>();
            for (UserDeckMemberSlot s : slotsWithRitual) {
                if (s.getEquipmentItem().getEquipmentSet() == null) continue;
                String key = s.getRitual().getRitual().getId()
                        + "_" + s.getRitual().getOutcome().name()
                        + "_" + s.getEquipmentItem().getEquipmentSet().getId();
                pieceCountMap.merge(key, 1, Integer::sum);
            }

            if (!pieceCountMap.isEmpty()) {
                List<Long> setIds = slotsWithRitual.stream()
                        .filter(s -> s.getEquipmentItem().getEquipmentSet() != null)
                        .map(s -> s.getEquipmentItem().getEquipmentSet().getId())
                        .distinct().toList();

                ritualSetEffectRepository.findByRitualIdInAndEquipmentSetIdIn(ritualIds, setIds).stream()
                        .filter(eff -> isDeckBuffElementApplicable(eff.getElement(), memberNature))
                        .filter(eff -> {
                            String k = eff.getRitual().getId()
                                    + "_" + eff.getOutcome().name()
                                    + "_" + eff.getEquipmentSet().getId();
                            Integer count = pieceCountMap.get(k);
                            return count != null && count >= eff.getRequiredRitualPieces();
                        })
                        .forEach(eff -> {
                            String k = eff.getRitual().getId()
                                    + "_" + eff.getOutcome().name()
                                    + "_" + eff.getEquipmentSet().getId();
                            ritualSetEffectStatMap.merge(eff.getStatType(), eff.getStatValue(), Integer::sum);
                            activeSetEffects.add(new MemberStatResponse.ActiveSetEffect(
                                    eff.getRitual().getDisplayName(),
                                    eff.getEquipmentSet().getName(),
                                    resolveRitualOutcomeMark(eff.getRitual(), eff.getOutcome()),
                                    pieceCountMap.get(k),
                                    eff.getRequiredRitualPieces(),
                                    eff.getStatType(),
                                    eff.getStatValue(),
                                    eff.getStatUnit()
                            ));
                        });
            }
        }

        // 공명 주스텟 — 주인공 멤버에만 적용. 슬롯 목록은 이미 로드된 slots 사용.
        StatType gonmyeongMainStat = null;
        if (deck.getGonmyeongLevel() != null
                && member.getMercenary().getCategory() == MercenaryCategory.PROTAGONIST) {
            gonmyeongMainStat = playerCharacterStatResolver.resolve(member.getMercenary(), slots);
        }

        Map<StatType, Integer> deckBuffStatMap = calculateDeckBuffStatsForMember(
                deck, member.getMercenary().getNature(), true, gonmyeongMainStat);
        List<MemberStatResponse.DeckBuffDetail> deckBuffDetails = computeDeckBuffDetailsForMember(
                deck, member.getMercenary().getNature(), true, gonmyeongMainStat);

        MemberBuildStatCalculator.BuildStatBonus buildBonus =
                memberBuildStatCalculator.compute(member, slots);
        Map<StatType, Integer> levelBonusStatMap = buildBonus.levelBonusStats();
        Map<StatType, Integer> bonusStatMap = buildBonus.bonusStats();
        StatType resolvedMainStat = buildBonus.resolvedMainStat();

        Map<StatType, Integer> preTransferTotal = new HashMap<>(baseStatMap);
        equipStatMap.forEach((k, v) -> preTransferTotal.merge(k, v, Integer::sum));
        setEffectStatMap.forEach((k, v) -> preTransferTotal.merge(k, v, Integer::sum));
        characteristicStatMap.forEach((k, v) -> preTransferTotal.merge(k, v, Integer::sum));
        ritualStatMap.forEach((k, v) -> preTransferTotal.merge(k, v, Integer::sum));
        ritualSetEffectStatMap.forEach((k, v) -> preTransferTotal.merge(k, v, Integer::sum));
        deckBuffStatMap.forEach((k, v) -> preTransferTotal.merge(k, v, Integer::sum));
        levelBonusStatMap.forEach((k, v) -> preTransferTotal.merge(k, v, Integer::sum));
        bonusStatMap.forEach((k, v) -> preTransferTotal.merge(k, v, Integer::sum));

        return new MemberStatComponents(slots, baseStatMap, equipStatMap, setEffectStatMap,
                characteristicStatMap, partyCharacteristicStatMap, enemyDebuffStatMap, ritualStatMap,
                ritualSetEffectStatMap, deckBuffStatMap, deckBuffDetails, levelBonusStatMap, bonusStatMap,
                resolvedMainStat, activeEquipmentSetEffects, activeSetEffects, preTransferTotal);
    }

    /** 덱 내 명왕 스탯 이전 — 수신 멤버별 합산량·출처 내역 */
    private MyungwangStatTransferCalculator.ComputedTransfers computeMyungwangTransfers(UserDeck deck) {
        List<UserDeckMember> allMembers = memberRepository.findByDeckIdWithMercenary(deck.getId());
        if (allMembers.isEmpty()) {
            return new MyungwangStatTransferCalculator.ComputedTransfers(Map.of(), Map.of());
        }

        Map<Long, Map<StatType, Integer>> preTotals = new HashMap<>();
        for (UserDeckMember m : allMembers) {
            preTotals.put(m.getId(), computeMemberStatComponents(deck, m).preTransferTotal());
        }

        List<Long> memberIds = allMembers.stream().map(UserDeckMember::getId).toList();
        List<UserDeckMemberCharacteristic> allChars =
                memberCharacteristicRepository.findByDeckMemberIdIn(memberIds);
        Map<Long, List<UserDeckMemberCharacteristic>> charsByMemberId = allChars.stream()
                .collect(Collectors.groupingBy(c -> c.getDeckMember().getId()));

        List<Long> characteristicIds = allChars.stream()
                .map(c -> c.getCharacteristic().getId()).distinct().toList();
        Map<Long, List<MercenaryCharacteristicLevel>> charLevelsByCharId = characteristicIds.isEmpty() ? Map.of() :
                characteristicLevelRepository.findByCharacteristicIdIn(characteristicIds).stream()
                        .collect(Collectors.groupingBy(l -> l.getCharacteristic().getId()));

        return myungwangStatTransferCalculator.computeReceivedTransfers(
                allMembers, charsByMemberId, charLevelsByCharId, preTotals);
    }

    /** 부동명왕 무기(명왕월 10% / 고급명왕월 15%) 주스텟 이전 */
    private BudongMyungwangWeaponTransferCalculator.ComputedTransfers computeBudongWeaponTransfers(
            UserDeck deck) {
        List<UserDeckMember> allMembers = memberRepository.findByDeckIdWithMercenary(deck.getId());
        if (allMembers.isEmpty()) {
            return new BudongMyungwangWeaponTransferCalculator.ComputedTransfers(Map.of(), Map.of());
        }

        List<Long> memberIds = allMembers.stream().map(UserDeckMember::getId).toList();
        Map<Long, List<UserDeckMemberSlot>> slotsByMemberId =
                slotRepository.findByDeckMemberIdIn(memberIds).stream()
                        .collect(Collectors.groupingBy(s -> s.getDeckMember().getId()));

        for (UserDeckMember budong : allMembers) {
            if (!BudongMyungwangWeaponTransferCalculator.isBudongMyeongwang(budong.getMercenary())) {
                continue;
            }
            List<UserDeckMemberSlot> budongSlots = slotsByMemberId.getOrDefault(budong.getId(), List.of());
            Optional<BudongMyungwangWeaponTransferCalculator.WeaponContext> weaponCtx =
                    budongMyungwangWeaponTransferCalculator.findWeaponContext(budong, budongSlots);
            if (weaponCtx.isEmpty()) {
                continue;
            }

            MemberStatComponents budongComponents = computeMemberStatComponents(deck, budong);
            int sourceTotal = BudongMyungwangWeaponTransferCalculator.computeSourceTotalFromDeckComponents(
                    budongComponents.baseStatMap(),
                    budongComponents.equipStatMap(),
                    budongComponents.setEffectStatMap(),
                    budongComponents.ritualStatMap(),
                    budongComponents.ritualSetEffectStatMap(),
                    budongComponents.levelBonusStatMap(),
                    budongComponents.bonusStatMap());

            Map<Long, StatType> mainStatByMemberId = new HashMap<>();
            for (UserDeckMember m : allMembers) {
                List<UserDeckMemberSlot> slots = slotsByMemberId.getOrDefault(m.getId(), List.of());
                mainStatByMemberId.put(m.getId(),
                        memberBuildStatCalculator.compute(m, slots).resolvedMainStat());
            }

            return budongMyungwangWeaponTransferCalculator.computeForDeckMembers(
                    allMembers, weaponCtx.get(), sourceTotal, mainStatByMemberId);
        }
        return new BudongMyungwangWeaponTransferCalculator.ComputedTransfers(Map.of(), Map.of());
    }

    /** 각성 명왕 무기(동속아군 속성+n%) — 수신 멤버별 속성값 합산 */
    private MyungwangWeaponElementShareCalculator.ComputedShares computeAwakenedWeaponElementShares(
            UserDeck deck) {
        List<UserDeckMember> allMembers = memberRepository.findByDeckIdWithMercenary(deck.getId());
        if (allMembers.isEmpty()) {
            return new MyungwangWeaponElementShareCalculator.ComputedShares(Map.of(), Map.of());
        }

        List<Long> memberIds = allMembers.stream().map(UserDeckMember::getId).toList();
        Map<Long, List<UserDeckMemberSlot>> slotsByMemberId =
                slotRepository.findByDeckMemberIdIn(memberIds).stream()
                        .collect(Collectors.groupingBy(s -> s.getDeckMember().getId()));

        List<Long> allItemIds = slotsByMemberId.values().stream()
                .flatMap(List::stream)
                .map(s -> s.getEquipmentItem().getItemId())
                .distinct()
                .toList();
        Map<Long, List<ItemStat>> itemStatsByItemId = allItemIds.isEmpty() ? Map.of()
                : itemStatRepository.findByItemIdIn(allItemIds).stream()
                        .collect(Collectors.groupingBy(ist -> ist.getItem().getId()));

        List<MyungwangWeaponElementShareCalculator.ShareContext> contexts = new ArrayList<>();
        Map<Long, Integer> sourceEvByWearerId = new HashMap<>();

        for (UserDeckMember wearer : allMembers) {
            List<UserDeckMemberSlot> wearerSlots = slotsByMemberId.getOrDefault(wearer.getId(), List.of());
            myungwangWeaponElementShareCalculator.findShareContext(wearer, wearerSlots, itemStatsByItemId)
                    .ifPresent(ctx -> {
                        contexts.add(ctx);
                        MemberStatComponents components = computeMemberStatComponents(deck, wearer);
                        int sourceEv = MyungwangWeaponElementShareCalculator.computeSourceElementValue(
                                components.baseStatMap(),
                                mergeElementValueMaps(
                                        components.equipStatMap(),
                                        components.setEffectStatMap(),
                                        components.ritualStatMap(),
                                        components.ritualSetEffectStatMap()),
                                components.characteristicStatMap());
                        sourceEvByWearerId.put(wearer.getId(), sourceEv);
                    });
        }

        if (contexts.isEmpty()) {
            return new MyungwangWeaponElementShareCalculator.ComputedShares(Map.of(), Map.of());
        }
        return myungwangWeaponElementShareCalculator.computeForDeckMembers(
                allMembers, contexts, sourceEvByWearerId);
    }

    /**
     * 명왕부·일반/고급 명왕 무기의 ALLY_HEAVENLY_KING 속성값 — 동속성 사천왕에게 합산.
     */
    private int computeHeavenlyKingElementValueForMember(
            List<UserDeckMember> allMembers,
            List<UserDeckMemberSlot> allSlots,
            UserDeckMember target) {

        MercenaryCategory category = target.getMercenary().getCategory();
        if (category != MercenaryCategory.FOUR_HEAVENLY_KINGS
                && category != MercenaryCategory.FOUR_HEAVENLY_KINGS_AWAKENING) {
            return 0;
        }
        Nature targetNature = target.getMercenary().getNature();
        if (targetNature == null) {
            return 0;
        }

        if (allSlots.isEmpty()) {
            return 0;
        }
        List<Long> itemIds = allSlots.stream().map(s -> s.getEquipmentItem().getItemId()).distinct().toList();
        Map<Long, List<ItemStat>> statsByItemId = itemStatRepository.findByItemIdIn(itemIds).stream()
                .filter(ist -> ist.getScope() == BuffTarget.ALLY_HEAVENLY_KING
                        && ist.getStatType() == StatType.ELEMENT_VALUE
                        && ist.getStatUnit() == StatUnit.FLAT)
                .collect(Collectors.groupingBy(ist -> ist.getItem().getId()));

        Map<Long, List<UserDeckMemberSlot>> slotsByMemberId = allSlots.stream()
                .collect(Collectors.groupingBy(s -> s.getDeckMember().getId()));

        int sum = 0;
        for (UserDeckMember wearer : allMembers) {
            if (!isMyeongwangWearer(wearer.getMercenary())) {
                continue;
            }
            Nature wearerNature = wearer.getMercenary().getNature();
            if (wearerNature == null || wearerNature != targetNature) {
                continue;
            }
            for (UserDeckMemberSlot slot : slotsByMemberId.getOrDefault(wearer.getId(), List.of())) {
                for (ItemStat stat : statsByItemId.getOrDefault(slot.getEquipmentItem().getItemId(), List.of())) {
                    sum += stat.getValue();
                }
            }
        }
        return sum;
    }

    private static boolean isMyeongwangWearer(org.example.gersangtrade.domain.catalog.Mercenary mercenary) {
        MercenaryCategory cat = mercenary.getCategory();
        if (cat != MercenaryCategory.MYEONG_KING && cat != MercenaryCategory.MYEONG_KING_AWAKENING) {
            return false;
        }
        return mercenary.getNature() != null && mercenary.getNature() != Nature.EARTH;
    }

    private static Map<StatType, Integer> mergeElementValueMaps(Map<StatType, Integer>... maps) {
        Map<StatType, Integer> merged = new HashMap<>();
        for (Map<StatType, Integer> map : maps) {
            int ev = map.getOrDefault(StatType.ELEMENT_VALUE, 0);
            if (ev != 0) {
                merged.merge(StatType.ELEMENT_VALUE, ev, Integer::sum);
            }
        }
        return merged;
    }

    private record MemberStatComponents(
            List<UserDeckMemberSlot> slots,
            Map<StatType, Integer> baseStatMap,
            Map<StatType, Integer> equipStatMap,
            Map<StatType, Integer> setEffectStatMap,
            Map<StatType, Integer> characteristicStatMap,
            Map<StatType, Integer> partyCharacteristicStatMap,
            Map<StatType, Integer> enemyDebuffStatMap,
            Map<StatType, Integer> ritualStatMap,
            Map<StatType, Integer> ritualSetEffectStatMap,
            Map<StatType, Integer> deckBuffStatMap,
            List<MemberStatResponse.DeckBuffDetail> deckBuffDetails,
            Map<StatType, Integer> levelBonusStatMap,
            Map<StatType, Integer> bonusStatMap,
            StatType resolvedMainStat,
            List<MemberStatResponse.ActiveEquipmentSetEffect> activeEquipmentSetEffects,
            List<MemberStatResponse.ActiveSetEffect> activeSetEffects,
            Map<StatType, Integer> preTransferTotal
    ) {}

    /** 덱 멤버 레벨 저장 (250 또는 260). */
    @Transactional
    public void updateMemberLevel(Long userId, Long deckId, Long memberId, MemberLevelUpdateRequest req) {
        if (req.level() != 250 && req.level() != 260) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "레벨은 250 또는 260만 허용됩니다.");
        }
        UserDeck deck = getDeckOrThrow(deckId);
        validateOwner(deck, userId);
        getMemberOrThrow(memberId, deckId).updateLevel(req.level());
    }

    /** 덱 멤버 레벨·보너스 스탯 빌드 설정 일괄 저장. */
    @Transactional
    public void updateMemberBuild(Long userId, Long deckId, Long memberId, MemberBuildUpdateRequest req) {
        if (req.level() != 250 && req.level() != 260) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "레벨은 250 또는 260만 허용됩니다.");
        }
        UserDeck deck = getDeckOrThrow(deckId);
        validateOwner(deck, userId);
        getMemberOrThrow(memberId, deckId).updateBuild(req.level(), req.bonusTarget(), req.bonusAmount());
    }

    /**
     * 덱 멤버 특성 선택 일괄 저장 (PUT 시맨틱 — 기존 선택 전부 교체).
     * characteristicId는 해당 용병의 특성이어야 한다.
     */
    @Transactional
    public void setMemberCharacteristics(Long userId, Long deckId, Long memberId,
                                         MemberCharacteristicSetRequest req) {
        UserDeck deck = getDeckOrThrow(deckId);
        validateOwner(deck, userId);
        UserDeckMember member = getMemberOrThrow(memberId, deckId);

        Long mercenaryId = member.getMercenary().getId();
        Map<Long, MercenaryCharacteristic> characteristicMap = characteristicRepository
                .findByMercenaryId(mercenaryId).stream()
                .collect(Collectors.toMap(MercenaryCharacteristic::getId, c -> c));

        int usedPoints = 0;
        for (MemberCharacteristicSetRequest.Entry entry : req.characteristics()) {
            MercenaryCharacteristic characteristic = characteristicMap.get(entry.characteristicId());
            if (characteristic == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "해당 용병의 특성이 아닙니다: " + entry.characteristicId());
            }
            usedPoints += CharacteristicPointUtil.selectionCost(characteristic, entry.selectedLevel());
        }

        int maxPoints = maxCharacteristicPoints(member);
        if (usedPoints > maxPoints) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "특성 포인트는 최대 " + maxPoints + "까지 사용할 수 있습니다. 현재=" + usedPoints);
        }

        // 기존 선택 전부 삭제 후 재저장 (delete flush 전 insert 시 unique 제약 충돌 방지)
        List<UserDeckMemberCharacteristic> existing =
                memberCharacteristicRepository.findByDeckMemberIdIn(List.of(memberId));
        memberCharacteristicRepository.deleteAll(existing);
        memberCharacteristicRepository.flush();

        List<UserDeckMemberCharacteristic> newSelections = req.characteristics().stream()
                .map(entry -> {
                    MercenaryCharacteristic characteristic = characteristicRepository
                            .findById(entry.characteristicId())
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                    "특성을 찾을 수 없습니다: " + entry.characteristicId()));
                    return UserDeckMemberCharacteristic.builder()
                            .deckMember(member)
                            .characteristic(characteristic)
                            .selectedLevel(entry.selectedLevel())
                            .build();
                })
                .toList();
        memberCharacteristicRepository.saveAll(newSelections);
    }

    // ── 특성 조회 ───────────────────────────────────────────────────────────

    /**
     * 덱 멤버의 특성 목록과 현재 선택 레벨 조회.
     * 해당 용병의 전체 특성 카탈로그와 유저가 선택한 레벨을 함께 반환한다.
     */
    @Transactional(readOnly = true)
    public MemberCharacteristicResponse getMemberCharacteristics(Long userId, Long deckId, Long memberId) {
        UserDeck deck = getDeckOrThrow(deckId);
        validateOwner(deck, userId);
        UserDeckMember member = getMemberOrThrow(memberId, deckId);

        Long mercenaryId = member.getMercenary().getId();
        List<MercenaryCharacteristic> allCharacteristics = characteristicRepository.findByMercenaryId(mercenaryId);

        List<UserDeckMemberCharacteristic> selected =
                memberCharacteristicRepository.findByDeckMemberIdIn(List.of(memberId));
        Map<Long, Integer> selectedLevelMap = selected.stream()
                .collect(Collectors.toMap(c -> c.getCharacteristic().getId(),
                        UserDeckMemberCharacteristic::getSelectedLevel));

        // 전설장수는 MercenaryCharacteristicLevel 대신 LegendGeneralCharacteristic에서 레벨 데이터 로드
        Map<Long, List<MemberCharacteristicResponse.LevelEntry>> levelsByCharId;
        if (member.getMercenary().getCategory() == MercenaryCategory.LEGENDARY_GENERAL) {
            levelsByCharId = buildLgLevelsByCharId(allCharacteristics, mercenaryId);
        } else {
            List<Long> charIds = allCharacteristics.stream().map(MercenaryCharacteristic::getId).toList();
            levelsByCharId = charIds.isEmpty() ? Map.of()
                    : characteristicLevelRepository.findByCharacteristicIdIn(charIds).stream()
                            .collect(Collectors.groupingBy(
                                    l -> l.getCharacteristic().getId(),
                                    Collectors.mapping(l -> new MemberCharacteristicResponse.LevelEntry(
                                            l.getLabel(), l.getLevel(), l.getAmount(), l.getAmountValue(),
                                            l.getStatType() != null ? l.getStatType().name() : null,
                                            null),
                                            Collectors.toList())));
        }

        List<MemberCharacteristicResponse.CharacteristicEntry> entries = allCharacteristics.stream()
                .map(c -> {
                    List<MemberCharacteristicResponse.LevelEntry> levels =
                            levelsByCharId.getOrDefault(c.getId(), List.of()).stream()
                                    .sorted(Comparator.comparing(MemberCharacteristicResponse.LevelEntry::level))
                                    .toList();
                    return new MemberCharacteristicResponse.CharacteristicEntry(
                            c.getId(), c.getKey(), c.getName(), c.getPoint(),
                            c.getDescription(), c.getRequiredCharacteristicKey(),
                            c.getApplyType().name(),
                            selectedLevelMap.get(c.getId()),
                            levels);
                })
                .toList();

        return new MemberCharacteristicResponse(memberId, member.getLevel(), maxCharacteristicPoints(member), entries);
    }

    /** 전설장수 MercenaryCharacteristic 스텁 ID → LevelEntry 목록 매핑 빌드. */
    private Map<Long, List<MemberCharacteristicResponse.LevelEntry>> buildLgLevelsByCharId(
            List<MercenaryCharacteristic> stubs, Long mercenaryId) {
        LegendGeneral lg = legendGeneralLoadService.loadForCalculation(mercenaryId).orElse(null);
        if (lg == null) return Map.of();

        Map<Integer, List<LegendGeneralCharacteristic>> byIndex = lg.getCharacteristics().stream()
                .collect(Collectors.groupingBy(LegendGeneralCharacteristic::getCharacteristicIndex));

        List<MercenaryCharacteristic> sortedStubs = stubs.stream()
                .sorted(Comparator.comparing(MercenaryCharacteristic::getId))
                .toList();

        Map<Long, List<MemberCharacteristicResponse.LevelEntry>> result = new HashMap<>();
        for (int i = 0; i < sortedStubs.size(); i++) {
            Long stubId = sortedStubs.get(i).getId();
            List<LegendGeneralCharacteristic> lgcRows = byIndex.getOrDefault(i, List.of());
            List<MemberCharacteristicResponse.LevelEntry> levels = lgcRows.stream()
                    .sorted(Comparator.comparing(LegendGeneralCharacteristic::getLevel))
                    .flatMap(row -> row.getEffects().stream()
                            .map(eff -> new MemberCharacteristicResponse.LevelEntry(
                                    null, row.getLevel(), null, eff.getValue(),
                                    eff.getStatType() != null ? eff.getStatType().name() : null,
                                    eff.getElement() != null ? eff.getElement().name() : null)))
                    .toList();
            result.put(stubId, levels);
        }
        return result;
    }

    // ── 장비 슬롯 ───────────────────────────────────────────────────────────

    @Transactional
    public void equipSlot(Long userId, Long deckId, Long memberId, EquipSlot slot, SlotEquipRequest req) {
        UserDeck deck = getDeckOrThrow(deckId);
        validateOwner(deck, userId);
        UserDeckMember member = getMemberOrThrow(memberId, deckId);

        EquipmentItem item = equipmentItemRepository.findWithItemByItemId(req.itemId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "아이템을 찾을 수 없습니다."));
        deckEquipmentValidator.validateSlotCompatibility(slot, item);
        deckEquipmentValidator.validateMercenaryRestriction(member.getMercenary(), item);

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

    // ── 세트 일괄 장착 ───────────────────────────────────────────────────────

    /**
     * 세트에 속한 모든 피스를 덱 멤버 슬롯에 일괄 장착.
     * 반지(pieceCount=2)는 RING_1, RING_2에 모두 장착한다.
     * 이미 착용 중인 슬롯은 교체한다.
     */
    @Transactional
    public void equipSet(Long userId, Long deckId, Long memberId, Long setId) {
        UserDeck deck = getDeckOrThrow(deckId);
        validateOwner(deck, userId);
        UserDeckMember member = getMemberOrThrow(memberId, deckId);

        List<EquipmentSetPiece> pieces = equipmentSetPieceRepository.findWithItemByEquipmentSetId(setId);
        if (!pieces.isEmpty()) {
            for (EquipmentSetPiece piece : pieces) {
                equipSetPiece(member, piece.getEquipmentItem(), piece.getSlot(), piece.getPieceCount());
            }
            return;
        }

        // equipment_set_pieces가 비어 있으면 equipment_items.set_id 기준으로 fallback
        List<EquipmentItem> items = equipmentItemRepository.findBySetIdWithItem(setId);
        if (items.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "세트를 찾을 수 없거나 피스가 없습니다. setId=" + setId);
        }
        for (EquipmentItem item : items) {
            equipSetPiece(member, item, item.getSlot(), item.getSlot() == EquipmentSlot.RING ? 2 : 1);
        }
    }

    /** 세트 피스 1종을 대응 슬롯(반지는 양손)에 장착한다. */
    private void equipSetPiece(UserDeckMember member, EquipmentItem item, EquipmentSlot pieceSlot, int pieceCount) {
        deckEquipmentValidator.validateMercenaryRestriction(member.getMercenary(), item);

        if (pieceSlot == EquipmentSlot.RING || pieceCount >= 2) {
            deckEquipmentValidator.validateSlotCompatibility(EquipSlot.RING_1, item);
            deckEquipmentValidator.validateSlotCompatibility(EquipSlot.RING_2, item);
            equipItemToSlot(member, EquipSlot.RING_1, item);
            equipItemToSlot(member, EquipSlot.RING_2, item);
            return;
        }

        EquipSlot targetSlot = deckEquipmentValidator.resolveSetEquipSlot(pieceSlot, item);
        if (targetSlot == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "세트 피스 슬롯 매핑에 실패했습니다. itemId=" + item.getItemId() + ", slot=" + pieceSlot);
        }
        deckEquipmentValidator.validateSlotCompatibility(targetSlot, item);
        equipItemToSlot(member, targetSlot, item);
    }

    private void equipItemToSlot(UserDeckMember member, EquipSlot slot, EquipmentItem item) {
        slotRepository.findByDeckMemberIdAndSlot(member.getId(), slot)
                .ifPresentOrElse(
                        existing -> existing.changeItem(item),
                        () -> slotRepository.save(UserDeckMemberSlot.of(member, slot, item))
                );
    }

    // ── 내부 유틸 ───────────────────────────────────────────────────────────

    private UserDeck getDeckOrThrow(Long deckId) {
        return deckRepository.findById(deckId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "덱을 찾을 수 없습니다."));
    }

    private Spirit getSpiritOrThrow(Long spiritId) {
        return spiritRepository.findById(spiritId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "정령을 찾을 수 없습니다. id=" + spiritId));
    }

    private DeckBuffSource getDeckBuffSourceOrThrow(Long sourceId, DeckBuffSourceType type) {
        DeckBuffSource source = deckBuffSourceRepository.findById(sourceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "덱 버프를 찾을 수 없습니다. id=" + sourceId));
        if (source.getSourceType() != type) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    type + " 타입의 덱 버프만 선택할 수 있습니다.");
        }
        return source;
    }

    private DeckEffectResponse buildDeckEffectResponse(UserDeck deck) {
        return new DeckEffectResponse(
                selectedSpirits(deck).stream().map(DeckEffectResponse.SpiritEntry::of).toList(),
                DeckEffectResponse.DeckBuffSourceEntry.of(deck.getJinbeopSource()),
                DeckEffectResponse.DeckBuffSourceEntry.of(deck.getCheungjinSource()),
                deck.getGonmyeongLevel(),
                deck.getGahoLevel(),
                DeckEffectResponse.StatEntry.from(calculateDeckBuffStats(deck, false))
        );
    }

    private List<Spirit> selectedSpirits(UserDeck deck) {
        List<Spirit> spirits = new ArrayList<>();
        if (deck.getSpirit1() != null) spirits.add(deck.getSpirit1());
        if (deck.getSpirit2() != null) spirits.add(deck.getSpirit2());
        return spirits;
    }

    private int maxCharacteristicPoints(UserDeckMember member) {
        return member.getMercenary().getCategory() == MercenaryCategory.PROTAGONIST
                ? PROTAGONIST_CHARACTERISTIC_POINTS
                : DEFAULT_CHARACTERISTIC_POINTS;
    }

    private Map<StatType, Integer> calculateDeckBuffStats(UserDeck deck, boolean allyOnly) {
        Map<StatType, Integer> stats = new EnumMap<>(StatType.class);

        accumulateSpiritBuffs(stats, selectedSpirits(deck), allyOnly);

        accumulateDeckBuffSource(stats, deck.getJinbeopSource(), allyOnly);
        accumulateDeckBuffSource(stats, deck.getCheungjinSource(), allyOnly);

        // 공명·가호 DAMAGE_PERCENT는 전체 용병 공통
        if (deck.getGonmyeongLevel() != null) {
            gonmyeongBuffCalculator.getDamagePercent(deck.getGonmyeongLevel())
                    .ifPresent(v -> stats.merge(StatType.DAMAGE_PERCENT, v, Integer::sum));
        }
        if (deck.getGahoLevel() != null) {
            gahoBuffCalculator.getDamagePercent(deck.getGahoLevel())
                    .ifPresent(v -> stats.merge(StatType.DAMAGE_PERCENT, v, Integer::sum));
        }
        return stats;
    }

    /**
     * 멤버 속성(Nature)에 맞는 진법·층진·공명·가호 버프를 합산한다.
     *
     * @param gonmyeongMainStat 공명 MAIN_STAT_FLAT 대상 스탯. 주인공 멤버만 non-null.
     */
    private Map<StatType, Integer> calculateDeckBuffStatsForMember(
            UserDeck deck, Nature nature, boolean allyOnly, StatType gonmyeongMainStat) {
        Map<StatType, Integer> stats = new EnumMap<>(StatType.class);

        accumulateSpiritBuffs(stats, selectedSpirits(deck), allyOnly, nature);
        accumulateDeckBuffSourceForMember(stats, deck.getJinbeopSource(), nature, allyOnly);
        accumulateDeckBuffSourceForMember(stats, deck.getCheungjinSource(), nature, allyOnly);

        // 공명 버프
        if (deck.getGonmyeongLevel() != null) {
            int lvl = deck.getGonmyeongLevel();
            gonmyeongBuffCalculator.getDamagePercent(lvl)
                    .ifPresent(v -> stats.merge(StatType.DAMAGE_PERCENT, v, Integer::sum));
            if (gonmyeongMainStat != null) {
                gonmyeongBuffCalculator.getMainStatFlat(lvl)
                        .ifPresent(v -> stats.merge(gonmyeongMainStat, v, Integer::sum));
            }
        }

        // 가호 버프
        if (deck.getGahoLevel() != null) {
            int lvl = deck.getGahoLevel();
            gahoBuffCalculator.getDamagePercent(lvl)
                    .ifPresent(v -> stats.merge(StatType.DAMAGE_PERCENT, v, Integer::sum));
            StatType mainStat = gahoBuffCalculator.resolveMainStat(nature);
            gahoBuffCalculator.getMainStatFlat(lvl)
                    .ifPresent(v -> stats.merge(mainStat, v, Integer::sum));
            gahoBuffCalculator.getElementValue(lvl, nature)
                    .ifPresent(v -> stats.merge(StatType.ELEMENT_VALUE, v, Integer::sum));
        }

        return stats;
    }

    /**
     * 덱 효과 출처별 기여 내역 — 정령·진법·층진·공명·가호 각 항목을 개별 기록으로 반환.
     *
     * @param gonmyeongMainStat 공명 MAIN_STAT_FLAT 대상 스탯. 주인공 멤버만 non-null.
     */
    private List<MemberStatResponse.DeckBuffDetail> computeDeckBuffDetailsForMember(
            UserDeck deck, Nature nature, boolean allyOnly, StatType gonmyeongMainStat) {
        List<MemberStatResponse.DeckBuffDetail> details = new ArrayList<>();

        List<Spirit> spirits = selectedSpirits(deck);
        boolean hasEarthLegend = spirits.stream()
                .anyMatch(s -> s.getNature() == Nature.EARTH && s.getGrade() == SpiritGrade.LEGEND);

        for (Spirit spirit : spirits) {
            boolean isEarthLegend = spirit.getNature() == Nature.EARTH && spirit.getGrade() == SpiritGrade.LEGEND;
            for (SpiritBuff buff : spirit.getBuffs()) {
                if (allyOnly && buff.getTarget() != BuffTarget.ALLY) continue;
                if (!isDeckBuffElementApplicable(buff.getElement(), nature)) continue;
                float value = buff.getValue();
                if (hasEarthLegend && !isEarthLegend
                        && !SPIRIT_DOUBLE_EXCLUDED_STATS.contains(buff.getStatType())) {
                    value *= 2.0f;
                }
                details.add(new MemberStatResponse.DeckBuffDetail(
                        spirit.getName(), "정령", buff.getStatType(), Math.round(value)));
            }
        }

        addDeckBuffSourceDetails(details, deck.getJinbeopSource(), "진법", nature, allyOnly);
        addDeckBuffSourceDetails(details, deck.getCheungjinSource(), "층진", nature, allyOnly);

        // 공명 detail
        if (deck.getGonmyeongLevel() != null) {
            int lvl = deck.getGonmyeongLevel();
            String label = "공명 " + lvl + "단계";
            gonmyeongBuffCalculator.getDamagePercent(lvl)
                    .ifPresent(v -> details.add(new MemberStatResponse.DeckBuffDetail(label, "공명", StatType.DAMAGE_PERCENT, v)));
            if (gonmyeongMainStat != null) {
                gonmyeongBuffCalculator.getMainStatFlat(lvl)
                        .ifPresent(v -> details.add(new MemberStatResponse.DeckBuffDetail(label, "공명", gonmyeongMainStat, v)));
            }
        }

        // 가호 detail
        if (deck.getGahoLevel() != null) {
            int lvl = deck.getGahoLevel();
            String label = "가호 " + lvl + "단계";
            StatType mainStat = gahoBuffCalculator.resolveMainStat(nature);
            gahoBuffCalculator.getDamagePercent(lvl)
                    .ifPresent(v -> details.add(new MemberStatResponse.DeckBuffDetail(label, "가호", StatType.DAMAGE_PERCENT, v)));
            gahoBuffCalculator.getMainStatFlat(lvl)
                    .ifPresent(v -> details.add(new MemberStatResponse.DeckBuffDetail(label, "가호", mainStat, v)));
            gahoBuffCalculator.getElementValue(lvl, nature)
                    .ifPresent(v -> details.add(new MemberStatResponse.DeckBuffDetail(label, "가호", StatType.ELEMENT_VALUE, v)));
        }

        return details;
    }

    private void addDeckBuffSourceDetails(List<MemberStatResponse.DeckBuffDetail> details,
                                          DeckBuffSource source, String sourceType,
                                          Nature nature, boolean allyOnly) {
        if (source == null) return;
        for (DeckBuff buff : source.getBuffs()) {
            if (allyOnly && buff.getTarget() != BuffTarget.ALLY) continue;
            if (!isDeckBuffElementApplicable(buff.getElement(), nature)) continue;
            details.add(new MemberStatResponse.DeckBuffDetail(
                    source.getName(), sourceType, buff.getStatType(), Math.round(buff.getValue())));
        }
    }

    private void accumulateDeckBuffSourceForMember(Map<StatType, Integer> stats,
                                                   DeckBuffSource source,
                                                   Nature nature,
                                                   boolean allyOnly) {
        if (source == null) return;
        for (DeckBuff buff : source.getBuffs()) {
            if (allyOnly && buff.getTarget() != BuffTarget.ALLY) continue;
            if (!isDeckBuffElementApplicable(buff.getElement(), nature)) continue;
            stats.merge(buff.getStatType(), Math.round(buff.getValue()), Integer::sum);
        }
    }

    /** NONE·ADAPTIVE는 전 멤버 적용, 특정 속성은 Nature 일치 시에만 적용. */
    private boolean isDeckBuffElementApplicable(Element element, Nature nature) {
        return switch (element) {
            case NONE, ADAPTIVE -> true;
            default -> element.name().equals(nature.name());
        };
    }

    /** 주술 outcome에 해당하는 표기 마크(<천추>, <북두칠성> 등)를 반환한다. */
    private String resolveRitualOutcomeMark(Ritual ritual, RitualOutcome outcome) {
        if (outcome == RitualOutcome.GREAT_SUCCESS && ritual.getGreatSuccessMark() != null) {
            return ritual.getGreatSuccessMark();
        }
        return ritual.getSuccessMark();
    }

    private void accumulateDeckBuffSource(Map<StatType, Integer> stats,
                                          DeckBuffSource source,
                                          boolean allyOnly) {
        if (source == null) return;
        for (DeckBuff buff : source.getBuffs()) {
            if (allyOnly && buff.getTarget() != BuffTarget.ALLY) continue;
            stats.merge(buff.getStatType(), Math.round(buff.getValue()), Integer::sum);
        }
    }

    private void accumulateSpiritBuffs(Map<StatType, Integer> stats,
                                       List<Spirit> spirits,
                                       boolean allyOnly) {
        accumulateSpiritBuffs(stats, spirits, allyOnly, null);
    }

    // memberNature가 null이면 element 필터 없이 전체 합산 (덱 효과 표시용)
    private void accumulateSpiritBuffs(Map<StatType, Integer> stats,
                                       List<Spirit> spirits,
                                       boolean allyOnly,
                                       Nature memberNature) {
        boolean hasEarthLegend = spirits.stream()
                .anyMatch(spirit -> spirit.getNature() == Nature.EARTH
                        && spirit.getGrade() == SpiritGrade.LEGEND);

        for (Spirit spirit : spirits) {
            boolean isEarthLegend = spirit.getNature() == Nature.EARTH
                    && spirit.getGrade() == SpiritGrade.LEGEND;

            for (SpiritBuff buff : spirit.getBuffs()) {
                if (allyOnly && buff.getTarget() != BuffTarget.ALLY) continue;
                if (memberNature != null && !isDeckBuffElementApplicable(buff.getElement(), memberNature)) continue;

                float value = buff.getValue();
                if (hasEarthLegend && !isEarthLegend
                        && !SPIRIT_DOUBLE_EXCLUDED_STATS.contains(buff.getStatType())) {
                    value *= 2.0f;
                }
                stats.merge(buff.getStatType(), Math.round(value), Integer::sum);
            }
        }
    }

    /** 착용 세트 피스 수에 따라 SELF scope 장비 세트효과를 합산한다. */
    private void accumulateEquipmentSetEffects(List<UserDeckMemberSlot> slots,
                                                 Nature nature,
                                                 Map<StatType, Integer> setEffectStatMap,
                                                 List<MemberStatResponse.ActiveEquipmentSetEffect> activeEffects) {
        if (slots.isEmpty()) return;

        Map<Long, Integer> pieceCountBySetId = countSetPieces(slots);
        if (pieceCountBySetId.isEmpty()) return;

        Map<Long, String> setNameById = new HashMap<>();
        for (UserDeckMemberSlot slot : slots) {
            EquipmentSet set = slot.getEquipmentItem().getEquipmentSet();
            if (set != null) {
                setNameById.putIfAbsent(set.getId(), set.getName());
            }
        }

        Map<Long, List<EquipmentSetEffect>> effectsBySetId = equipmentSetEffectRepository
                .findBySetIdIn(new ArrayList<>(pieceCountBySetId.keySet())).stream()
                .collect(Collectors.groupingBy(e -> e.getEquipmentSet().getId()));

        for (Map.Entry<Long, Integer> entry : pieceCountBySetId.entrySet()) {
            Long setId = entry.getKey();
            int equippedPieces = entry.getValue();
            String setName = setNameById.getOrDefault(setId, "세트");

            for (EquipmentSetEffect effect : effectsBySetId.getOrDefault(setId, List.of())) {
                if (effect.getRequiredPieces() > equippedPieces) continue;
                if (effect.getScope() != BuffTarget.SELF) continue;
                if (!isDeckBuffElementApplicable(effect.getElement(), nature)) continue;
                if (effect.getStatUnit() != StatUnit.FLAT) continue;

                setEffectStatMap.merge(effect.getStatType(), effect.getStatValue(), Integer::sum);
                activeEffects.add(new MemberStatResponse.ActiveEquipmentSetEffect(
                        setName,
                        equippedPieces,
                        effect.getRequiredPieces(),
                        effect.getStatType(),
                        effect.getStatValue()
                ));
            }
        }
    }

    private Map<Long, Integer> countSetPieces(List<UserDeckMemberSlot> slots) {
        Map<Long, Integer> count = new HashMap<>();
        for (UserDeckMemberSlot slot : slots) {
            EquipmentSet set = slot.getEquipmentItem().getEquipmentSet();
            if (set != null) {
                count.merge(set.getId(), 1, Integer::sum);
            }
        }
        return count;
    }

    private void accumulateLegendGeneralScopedStats(
            LegendGeneral legendGeneral,
            Map<Long, Integer> charIndexByCharId,
            List<UserDeckMemberCharacteristic> memberCharacteristics,
            int mercenaryLevel,
            Map<StatType, Integer> selfMap,
            Map<StatType, Integer> allyMap,
            Map<StatType, Integer> enemyMap) {

        Map<Integer, Integer> pointsMap = new HashMap<>();
        for (UserDeckMemberCharacteristic mc : memberCharacteristics) {
            Integer index = charIndexByCharId.get(mc.getCharacteristic().getId());
            if (index != null) {
                pointsMap.put(index, mc.getSelectedLevel());
            }
        }

        legendGeneralBuffCalculator.calculate(legendGeneral, mercenaryLevel, pointsMap)
                .forEach((key, value) -> {
                    if (key.statType() == null) return;
                    Map<StatType, Integer> target = key.target() == BuffTarget.ENEMY ? enemyMap : allyMap;
                    target.merge(key.statType(), Math.round(value), Integer::sum);
                });
        legendGeneralBuffCalculator.calculateSelfBuffs(legendGeneral, pointsMap)
                .forEach((key, value) -> {
                    if (key.statType() == null) return;
                    selfMap.merge(key.statType(), Math.round(value), Integer::sum);
                });
    }

    /** 용병 삭제 전 하위 데이터 정리 — 장비·주술·특성 등 FK 참조 해제 */
    private void purgeMemberRelatedData(Long memberId) {
        slotRitualRepository.deleteByDeckMemberId(memberId);
        slotRepository.deleteByDeckMemberId(memberId);
        memberCharacteristicRepository.deleteByDeckMemberId(memberId);
        memberEquipRepository.deleteByDeckMemberId(memberId);
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

}
