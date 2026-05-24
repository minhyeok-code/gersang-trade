package org.example.gersangtrade.deck.service;

import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.catalog.repository.DeckBuffSourceRepository;
import org.example.gersangtrade.catalog.repository.EquipmentItemRepository;
import org.example.gersangtrade.catalog.repository.EquipmentSetEffectRepository;
import org.example.gersangtrade.catalog.repository.EquipmentSetPieceRepository;
import org.example.gersangtrade.catalog.repository.ItemMercenaryRestrictionRepository;
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
import org.example.gersangtrade.domain.catalog.Mercenary;
import org.example.gersangtrade.domain.catalog.MercenaryCharacteristic;
import org.example.gersangtrade.domain.catalog.MercenaryCharacteristicLevel;
import org.example.gersangtrade.domain.catalog.MercenaryStat;
import org.example.gersangtrade.domain.catalog.Ritual;
import org.example.gersangtrade.calculator.service.CharacteristicScopeResolver;
import org.example.gersangtrade.calculator.service.CharacteristicScopeResolver.ApplicationMode;
import org.example.gersangtrade.calculator.service.CharacteristicScopeResolver.ScopedEffect;
import org.example.gersangtrade.calculator.service.AwakenedMyungwangBuffCalculator;
import org.example.gersangtrade.calculator.service.LegendGeneralBuffCalculator;
import org.example.gersangtrade.calculator.service.MemberBuildStatCalculator;
import org.example.gersangtrade.calculator.service.MyungwangStatTransferCalculator;
import org.example.gersangtrade.calculator.service.PlayerCharacterBuffCalculator;
import org.example.gersangtrade.catalog.service.LegendGeneralLoadService;
import org.example.gersangtrade.domain.catalog.LegendGeneral;
import org.example.gersangtrade.domain.catalog.enums.BuffTarget;
import org.example.gersangtrade.domain.catalog.enums.BuffValueType;
import org.example.gersangtrade.domain.catalog.enums.DeckBuffSourceType;
import org.example.gersangtrade.domain.catalog.enums.Element;
import org.example.gersangtrade.domain.catalog.enums.EquipmentKind;
import org.example.gersangtrade.domain.catalog.enums.EquipmentSlot;
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
    private final ItemMercenaryRestrictionRepository itemMercenaryRestrictionRepository;
    private final MercenaryCharacteristicRepository characteristicRepository;
    private final MercenaryCharacteristicLevelRepository characteristicLevelRepository;
    private final RitualStatRepository ritualStatRepository;
    private final RitualSetEffectRepository ritualSetEffectRepository;
    private final SpiritRepository spiritRepository;
    private final DeckBuffSourceRepository deckBuffSourceRepository;
    private final LegendGeneralLoadService legendGeneralLoadService;
    private final LegendGeneralBuffCalculator legendGeneralBuffCalculator;
    private final MyungwangStatTransferCalculator myungwangStatTransferCalculator;
    private final MemberBuildStatCalculator memberBuildStatCalculator;
    private final PlayerCharacterBuffCalculator playerCharacterBuffCalculator;
    private final AwakenedMyungwangBuffCalculator awakenedMyungwangBuffCalculator;

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

        deck.updateEffects(spirit1, spirit2, jinbeop, cheungjin);
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

        validateMyeongwangComposition(deckId, mercenary);

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
        Map<StatType, Integer> transferStatMap =
                transfers.receivedByMemberId().getOrDefault(memberId, Map.of());
        List<MemberStatResponse.MyungwangTransferDetail> transferDetails =
                transfers.detailsByMemberId().getOrDefault(memberId, List.of()).stream()
                        .map(d -> new MemberStatResponse.MyungwangTransferDetail(
                                d.sourceMercenaryName(), d.statType(), d.value()))
                        .toList();
        Map<StatType, Integer> totalMap = new HashMap<>(components.preTransferTotal());
        transferStatMap.forEach((k, v) -> totalMap.merge(k, v, Integer::sum));

        // 전체 멤버 로드 (주인공·각성명왕 버프 계산에 필요)
        List<UserDeckMember> allMembers = memberRepository.findByDeckIdWithMercenary(deckId);

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
                        totalMap.merge(StatType.ELEMENT_VALUE, Math.round(nationBuff.value()), Integer::sum);
                    }
                });

        // 각성 명왕 N명 × 아군 속성값 버프 (+5, EARTH +2 중첩)
        long awakenedMyeongwangCount = allMembers.stream()
                .filter(m -> m.getMercenary().getCategory() == MercenaryCategory.MYEONG_KING_AWAKENING)
                .count();
        if (awakenedMyeongwangCount > 0) {
            var allyBuff = awakenedMyungwangBuffCalculator.getCommonAllyBuff();
            int bonus = (int) (Math.round(allyBuff.defaultValue()) * awakenedMyeongwangCount);
            if (member.getMercenary().getNature() == Nature.EARTH) {
                bonus += (int) (Math.round(allyBuff.earthValue()) * awakenedMyeongwangCount);
            }
            totalMap.merge(StatType.ELEMENT_VALUE, bonus, Integer::sum);
        }

        return MemberStatResponse.of(member, components.baseStatMap(), components.equipStatMap(),
                components.setEffectStatMap(), components.characteristicStatMap(),
                components.partyCharacteristicStatMap(), components.enemyDebuffStatMap(),
                components.ritualStatMap(), components.deckBuffStatMap(),
                components.levelBonusStatMap(), components.bonusStatMap(), components.resolvedMainStat(),
                transferStatMap, transferDetails, components.activeEquipmentSetEffects(),
                components.activeSetEffects(), totalMap, components.slots());
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

        // SELF scope 장비 스탯 합산 (StatType별)
        Map<StatType, Integer> equipStatMap = new HashMap<>();
        if (!slots.isEmpty()) {
            List<Long> itemIds = slots.stream().map(s -> s.getEquipmentItem().getItemId()).toList();
            itemStatRepository.findByItemIdIn(itemIds).stream()
                    .filter(ist -> ist.getScope() == BuffTarget.SELF)
                    .forEach(ist -> equipStatMap.merge(ist.getStatType(), ist.getValue(), Integer::sum));
        }

        Nature memberNature = member.getMercenary().getNature();
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

        // 주술 스탯 합산 (슬롯별 주술 outcome에 따라 RitualStat 조회)
        Map<StatType, Integer> ritualStatMap = new HashMap<>();
        List<MemberStatResponse.ActiveSetEffect> activeSetEffects = new ArrayList<>();

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
                            activeSetEffects.add(new MemberStatResponse.ActiveSetEffect(
                                    eff.getRitual().getDisplayName(),
                                    eff.getEquipmentSet().getName(),
                                    eff.getOutcome().name(),
                                    pieceCountMap.get(k),
                                    eff.getRequiredRitualPieces(),
                                    eff.getStatType(),
                                    eff.getStatValue()
                            ));
                        });
            }
        }

        Map<StatType, Integer> deckBuffStatMap = calculateDeckBuffStatsForMember(
                deck, member.getMercenary().getNature(), true);

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
        deckBuffStatMap.forEach((k, v) -> preTransferTotal.merge(k, v, Integer::sum));
        activeSetEffects.forEach(eff -> preTransferTotal.merge(eff.statType(), eff.statValue(), Integer::sum));
        levelBonusStatMap.forEach((k, v) -> preTransferTotal.merge(k, v, Integer::sum));
        bonusStatMap.forEach((k, v) -> preTransferTotal.merge(k, v, Integer::sum));

        return new MemberStatComponents(slots, baseStatMap, equipStatMap, setEffectStatMap,
                characteristicStatMap, partyCharacteristicStatMap, enemyDebuffStatMap, ritualStatMap,
                deckBuffStatMap, levelBonusStatMap, bonusStatMap, resolvedMainStat,
                activeEquipmentSetEffects, activeSetEffects, preTransferTotal);
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

    private record MemberStatComponents(
            List<UserDeckMemberSlot> slots,
            Map<StatType, Integer> baseStatMap,
            Map<StatType, Integer> equipStatMap,
            Map<StatType, Integer> setEffectStatMap,
            Map<StatType, Integer> characteristicStatMap,
            Map<StatType, Integer> partyCharacteristicStatMap,
            Map<StatType, Integer> enemyDebuffStatMap,
            Map<StatType, Integer> ritualStatMap,
            Map<StatType, Integer> deckBuffStatMap,
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
            int pointCost = characteristic.getPoint() != null ? characteristic.getPoint() : 0;
            usedPoints += pointCost * entry.selectedLevel();
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

        List<Long> charIds = allCharacteristics.stream().map(MercenaryCharacteristic::getId).toList();
        Map<Long, List<MercenaryCharacteristicLevel>> levelsByCharId = charIds.isEmpty()
                ? Map.of()
                : characteristicLevelRepository.findByCharacteristicIdIn(charIds).stream()
                        .collect(Collectors.groupingBy(l -> l.getCharacteristic().getId()));

        List<MemberCharacteristicResponse.CharacteristicEntry> entries = allCharacteristics.stream()
                .map(c -> {
                    List<MemberCharacteristicResponse.LevelEntry> levels =
                            levelsByCharId.getOrDefault(c.getId(), List.of()).stream()
                                    .sorted(Comparator.comparing(MercenaryCharacteristicLevel::getLevel))
                                    .map(l -> new MemberCharacteristicResponse.LevelEntry(
                                            l.getLabel(), l.getLevel(), l.getAmount(), l.getAmountValue(),
                                            l.getStatType() != null ? l.getStatType().name() : null))
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

    // ── 장비 슬롯 ───────────────────────────────────────────────────────────

    @Transactional
    public void equipSlot(Long userId, Long deckId, Long memberId, EquipSlot slot, SlotEquipRequest req) {
        UserDeck deck = getDeckOrThrow(deckId);
        validateOwner(deck, userId);
        UserDeckMember member = getMemberOrThrow(memberId, deckId);

        EquipmentItem item = equipmentItemRepository.findWithItemByItemId(req.itemId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "아이템을 찾을 수 없습니다."));
        validateSlotCompatibility(slot, item);
        validateMercenaryRestriction(member.getMercenary(), item);

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
        validateMercenaryRestriction(member.getMercenary(), item);

        if (pieceSlot == EquipmentSlot.RING || pieceCount >= 2) {
            validateSlotCompatibility(EquipSlot.RING_1, item);
            validateSlotCompatibility(EquipSlot.RING_2, item);
            equipItemToSlot(member, EquipSlot.RING_1, item);
            equipItemToSlot(member, EquipSlot.RING_2, item);
            return;
        }

        EquipSlot targetSlot = resolveSetEquipSlot(pieceSlot, item);
        if (targetSlot == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "세트 피스 슬롯 매핑에 실패했습니다. itemId=" + item.getItemId() + ", slot=" + pieceSlot);
        }
        validateSlotCompatibility(targetSlot, item);
        equipItemToSlot(member, targetSlot, item);
    }

    /** 세트 피스 정의 슬롯을 우선해 EquipSlot을 결정한다. */
    private EquipSlot resolveSetEquipSlot(EquipmentSlot pieceSlot, EquipmentItem item) {
        EquipSlot fromPiece = fallbackEquipSlot(pieceSlot);
        if (fromPiece != null) {
            return fromPiece;
        }
        if (item.getEquipSlot() != null) {
            return item.getEquipSlot();
        }
        return fallbackEquipSlot(item.getSlot());
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
        return stats;
    }

    /** 멤버 속성(Nature)에 맞는 진법·층진 버프만 반영한다. DPS 계산기와 동일 규칙. */
    private Map<StatType, Integer> calculateDeckBuffStatsForMember(UserDeck deck, Nature nature, boolean allyOnly) {
        Map<StatType, Integer> stats = new EnumMap<>(StatType.class);

        accumulateSpiritBuffs(stats, selectedSpirits(deck), allyOnly);
        accumulateDeckBuffSourceForMember(stats, deck.getJinbeopSource(), nature, allyOnly);
        accumulateDeckBuffSourceForMember(stats, deck.getCheungjinSource(), nature, allyOnly);
        return stats;
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
        boolean hasEarthLegend = spirits.stream()
                .anyMatch(spirit -> spirit.getNature() == Nature.EARTH
                        && spirit.getGrade() == SpiritGrade.LEGEND);

        for (Spirit spirit : spirits) {
            boolean isEarthLegend = spirit.getNature() == Nature.EARTH
                    && spirit.getGrade() == SpiritGrade.LEGEND;

            for (SpiritBuff buff : spirit.getBuffs()) {
                if (allyOnly && buff.getTarget() != BuffTarget.ALLY) continue;

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

    private void validateSlotCompatibility(EquipSlot slot, EquipmentItem item) {
        if (slot.name().startsWith("APP_")) {
            if (item.getEquipmentKind() != EquipmentKind.APPEARANCE) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "외변 슬롯에는 외변 아이템만 착용 가능합니다.");
            }
            if (item.getEquipSlot() != slot && fallbackEquipmentSlot(slot) != item.getSlot()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "해당 외변 슬롯에 착용 불가능한 아이템입니다.");
            }
        } else if (slot == EquipSlot.RING_1 || slot == EquipSlot.RING_2) {
            if (item.getSlot() != EquipmentSlot.RING) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "반지 슬롯에는 반지 아이템만 착용 가능합니다.");
            }
        } else {
            if (item.getEquipmentKind() != EquipmentKind.NORMAL) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "일반 슬롯에는 일반 장비만 착용 가능합니다.");
            }
            if (item.getEquipSlot() != slot && fallbackEquipmentSlot(slot) != item.getSlot()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "해당 슬롯에 착용 불가능한 아이템입니다.");
            }
        }
    }

    private EquipSlot fallbackEquipSlot(EquipmentSlot slot) {
        return switch (slot) {
            case HELMET -> EquipSlot.HELMET;
            case ARMOR -> EquipSlot.ARMOR;
            case WEAPON -> EquipSlot.WEAPON;
            case SHOES -> EquipSlot.SHOES;
            case GLOVES -> EquipSlot.GLOVES;
            case BELT -> EquipSlot.BELT;
            case TALISMAN -> EquipSlot.CHARM;
            default -> null;
        };
    }

    private EquipmentSlot fallbackEquipmentSlot(EquipSlot slot) {
        return switch (slot) {
            case HELMET -> EquipmentSlot.HELMET;
            case ARMOR -> EquipmentSlot.ARMOR;
            case WEAPON -> EquipmentSlot.WEAPON;
            case SHOES -> EquipmentSlot.SHOES;
            case GLOVES -> EquipmentSlot.GLOVES;
            case BELT -> EquipmentSlot.BELT;
            case CHARM -> EquipmentSlot.TALISMAN;
            case APP_SPIRIT, APP_EARRING, APP_NECKLACE -> EquipmentSlot.ACCESSORY;
            case APP_BRACELET -> EquipmentSlot.BRACELET;
            case APP_GREAVES -> EquipmentSlot.LEGGING;
            case APP_WAR_GOD -> EquipmentSlot.DIVINE;
            default -> null;
        };
    }

    /**
     * 아이템 착용 용병 제한 검증.
     * item_mercenary_restrictions 행이 없으면 공용. 있으면 하나 이상 조건 일치 시 통과.
     */
    private void validateMercenaryRestriction(Mercenary mercenary, EquipmentItem item) {
        var restrictions = itemMercenaryRestrictionRepository.findByItemId(item.getItemId());
        if (restrictions.isEmpty()) return;
        boolean allowed = restrictions.stream().anyMatch(r -> r.allows(mercenary));
        if (!allowed) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "해당 용병은 이 아이템을 착용할 수 없습니다.");
        }
    }

    /**
     * 명왕 편성 제한 검증.
     * 1. 부동명왕(EARTH) 제외 명왕·각성명왕 합산 최대 2명.
     * 2. 동일 속성(Nature) 계열 명왕·각성명왕은 한 명만 (일반/각성 중 택1).
     */
    private void validateMyeongwangComposition(Long deckId, Mercenary incoming) {
        boolean isMyeongwang = incoming.getCategory() == MercenaryCategory.MYEONG_KING
                || incoming.getCategory() == MercenaryCategory.MYEONG_KING_AWAKENING;
        if (!isMyeongwang) return;

        List<UserDeckMember> currentMembers = memberRepository.findByDeckIdWithMercenary(deckId);

        if (incoming.getNature() != null && incoming.getNature() != Nature.NONE) {
            boolean hasSameNatureMyeongwang = currentMembers.stream()
                    .anyMatch(m -> isMyeongwangMember(m)
                            && m.getMercenary().getNature() == incoming.getNature());
            if (hasSameNatureMyeongwang) {
                String natureLabel = incoming.getNature().getDisplayName();
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        natureLabel + " 계열 명왕(일반/각성)은 한 명만 편성할 수 있습니다.");
            }
        }

        if (incoming.getNature() != Nature.EARTH) {
            long nonEarthCount = currentMembers.stream()
                    .filter(m -> isMyeongwangMember(m)
                            && m.getMercenary().getNature() != Nature.EARTH)
                    .count();
            if (nonEarthCount >= 2) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "부동명왕을 제외한 명왕·각성명왕은 최대 2명까지 편성할 수 있습니다.");
            }
        }
    }

    private static boolean isMyeongwangMember(UserDeckMember member) {
        MercenaryCategory category = member.getMercenary().getCategory();
        return category == MercenaryCategory.MYEONG_KING
                || category == MercenaryCategory.MYEONG_KING_AWAKENING;
    }
}
