package org.example.gersangtrade.calculator.service;

import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.calculator.dto.request.BonusStatTarget;
import org.example.gersangtrade.calculator.dto.request.DpsRequest;
import org.example.gersangtrade.calculator.dto.request.MemberDpsInput;
import org.example.gersangtrade.calculator.dto.request.ResistanceType;
import org.example.gersangtrade.calculator.dto.response.DpsResponse;
import org.example.gersangtrade.calculator.dto.response.MemberDpsResult;
import org.example.gersangtrade.calculator.dto.response.SkillDpsResult;
import org.example.gersangtrade.catalog.repository.EquipmentSetEffectRepository;
import org.example.gersangtrade.catalog.repository.EquipmentSetSkillEffectRepository;
import org.example.gersangtrade.catalog.repository.ItemStatRepository;
import org.example.gersangtrade.catalog.repository.MercenaryCharacteristicLevelRepository;
import org.example.gersangtrade.catalog.repository.MercenaryStatRepository;
import org.example.gersangtrade.catalog.repository.MonsterRepository;
import org.example.gersangtrade.catalog.repository.RitualSetEffectRepository;
import org.example.gersangtrade.catalog.repository.RitualStatRepository;
import org.example.gersangtrade.catalog.repository.SkillCoefficientRepository;
import org.example.gersangtrade.deck.repository.UserDeckMemberCharacteristicRepository;
import org.example.gersangtrade.deck.repository.UserDeckMemberRepository;
import org.example.gersangtrade.deck.repository.UserDeckMemberSlotRepository;
import org.example.gersangtrade.deck.repository.UserDeckRepository;
import org.example.gersangtrade.calculator.service.CharacteristicScopeResolver.ApplicationMode;
import org.example.gersangtrade.calculator.service.CharacteristicScopeResolver.ScopedEffect;
import org.example.gersangtrade.calculator.service.LegendGeneralBuffCalculator.BuffKey;
import org.example.gersangtrade.catalog.service.LegendGeneralLoadService;
import org.example.gersangtrade.catalog.repository.MercenaryCharacteristicRepository;
import org.example.gersangtrade.domain.catalog.LegendGeneral;
import org.example.gersangtrade.domain.catalog.MercenaryCharacteristic;
import org.example.gersangtrade.domain.catalog.enums.MercenaryCategory;
import org.example.gersangtrade.domain.catalog.DeckBuff;
import org.example.gersangtrade.domain.catalog.DeckBuffSource;
import org.example.gersangtrade.domain.catalog.EquipmentSet;
import org.example.gersangtrade.domain.catalog.EquipmentSetEffect;
import org.example.gersangtrade.domain.catalog.EquipmentSetSkillEffect;
import org.example.gersangtrade.domain.catalog.ItemStat;
import org.example.gersangtrade.domain.catalog.MercenaryCharacteristicLevel;
import org.example.gersangtrade.domain.catalog.MercenaryStat;
import org.example.gersangtrade.domain.catalog.Monster;
import org.example.gersangtrade.domain.catalog.RitualSetEffect;
import org.example.gersangtrade.domain.catalog.RitualStat;
import org.example.gersangtrade.domain.catalog.SetGrantedSkill;
import org.example.gersangtrade.domain.catalog.SkillCoefficient;
import org.example.gersangtrade.domain.catalog.Spirit;
import org.example.gersangtrade.domain.catalog.SpiritBuff;
import org.example.gersangtrade.domain.catalog.enums.BuffTarget;
import org.example.gersangtrade.domain.catalog.enums.BuffValueType;
import org.example.gersangtrade.domain.catalog.enums.Element;
import org.example.gersangtrade.domain.catalog.enums.Enhancement;
import org.example.gersangtrade.domain.catalog.enums.Nature;
import org.example.gersangtrade.domain.catalog.enums.SkillType;
import org.example.gersangtrade.domain.catalog.enums.SpiritGrade;
import org.example.gersangtrade.domain.catalog.enums.StatSource;
import org.example.gersangtrade.domain.catalog.enums.StatType;
import org.example.gersangtrade.domain.catalog.enums.StatUnit;
import org.example.gersangtrade.domain.catalog.enums.TriggerSource;
import org.example.gersangtrade.domain.deck.UserDeck;
import org.example.gersangtrade.domain.deck.UserDeckMember;
import org.example.gersangtrade.domain.deck.UserDeckMemberCharacteristic;
import org.example.gersangtrade.domain.deck.UserDeckMemberSlot;
import org.example.gersangtrade.domain.listing.enums.RitualOutcome;
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
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * DPS 계산기 서비스.
 *
 * <p>계산 흐름:
 * <ol>
 *   <li>덱 멤버 목록 및 장착 슬롯 일괄 로드
 *   <li>용병 스탯 + 아이템 스탯 + 세트 효과 + 스킬 계수 일괄 로드 (배치 쿼리)
 *   <li>특성 레벨 수치 + 주술 세트효과 + 세트 부여 스킬 계수 일괄 로드
 *   <li>파티 버프(PARTY scope) 사전 집계 — 모든 멤버의 아이템·세트 효과 합산
 *   <li>멤버별 유효 스탯 산출 = 기본 스탯 + 아이템 SELF 스탯 + 세트 SELF 효과
 *                            + 특성 레벨 보너스 + 주술 세트효과 + 파티 버프
 *   <li>총 저항깎 산출 = 전 멤버 RESIST_PIERCE 합 + 세트 ENEMY 디버프
 *   <li>스킬 계수 결정 → 원데미지 → DPS (INSTANT / PERSISTENT)
 *   <li>저항 통과율 × 속성 보정 → 최종 DPS
 * </ol>
 *
 * <p>현재 버전에서 적용하지 않는 항목:
 * <ul>
 *   <li>특성·전설장수 버프 — scope(SELF/ALLY/ENEMY)별 분류 적용 ({@link CharacteristicScopeResolver})
 *   <li>사천왕·명왕·주인공·전설장수 버프 계산기 연동
 *   <li>레벨별 공격력 공식 (용병마다 상이 — mercenary_stats의 ATTACK_POWER 사용)
 *   <li>StatUnit.LEVEL 단계 효과 (이속 등 — DPS에 직접 영향 없음)
 *   <li>StatSource.AFFINITY / TriggerSource.MERCENARY 세트 부여 스킬 (인연 연결 미지원)
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DpsCalculatorService {

    private static final int LEVEL_STAT_250 = 2256;
    private static final int LEVEL_STAT_260 = 2466;
    private static final int RESIST_CAP = 260;
    private static final double RESIST_CAP_PASS_RATE = 1.4;
    private static final Set<StatType> SPIRIT_DOUBLE_EXCLUDED_STATS = Set.of(
            StatType.ATTACK_SPEED,
            StatType.MOVE_SPEED
    );

    private final UserDeckRepository deckRepository;
    private final UserDeckMemberRepository memberRepository;
    private final UserDeckMemberSlotRepository slotRepository;
    private final UserDeckMemberCharacteristicRepository characteristicRepository;
    private final MercenaryStatRepository mercenaryStatRepository;
    private final MercenaryCharacteristicLevelRepository characteristicLevelRepository;
    private final ItemStatRepository itemStatRepository;
    private final EquipmentSetEffectRepository setEffectRepository;
    private final EquipmentSetSkillEffectRepository setSkillEffectRepository;
    private final RitualSetEffectRepository ritualSetEffectRepository;
    private final RitualStatRepository ritualStatRepository;
    private final SkillCoefficientRepository skillCoefficientRepository;
    private final MonsterRepository monsterRepository;
    private final LegendGeneralLoadService legendGeneralLoadService;
    private final MercenaryCharacteristicRepository mercenaryCharacteristicRepository;
    private final LegendGeneralBuffCalculator legendGeneralBuffCalculator;
    private final PlayerCharacterBuffCalculator playerCharacterBuffCalculator;
    private final AwakenedMyungwangBuffCalculator awakenedMyungwangBuffCalculator;
    private final MyungwangStatTransferCalculator myungwangStatTransferCalculator;

    /** (ritualId, outcome, setId) 복합 키 — 주술 세트효과 맵 조회에 사용 */
    private record RitualSetKey(Long ritualId, RitualOutcome outcome, Long setId) {}

    public DpsResponse calculate(DpsRequest req) {
        UserDeck deck = deckRepository.findById(req.deckId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "덱을 찾을 수 없습니다."));

        List<UserDeckMember> members = memberRepository.findByDeckIdWithMercenary(req.deckId());
        if (members.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "덱에 용병이 없습니다.");
        }

        Map<Long, MemberDpsInput> inputMap = req.memberInputs() == null ? Map.of() :
                req.memberInputs().stream().collect(Collectors.toMap(MemberDpsInput::memberId, i -> i));

        // ── 배치 로드 ──────────────────────────────────────────────────────────

        List<Long> memberIds = members.stream().map(UserDeckMember::getId).toList();
        List<Long> mercenaryIds = members.stream().map(m -> m.getMercenary().getId()).toList();

        // 슬롯 (세트·주술 fetch 포함)
        List<UserDeckMemberSlot> allSlots = slotRepository.findByDeckMemberIdIn(memberIds);
        Map<Long, List<UserDeckMemberSlot>> slotsByMemberId = allSlots.stream()
                .collect(Collectors.groupingBy(s -> s.getDeckMember().getId()));

        // 용병 기본 스탯
        List<MercenaryStat> allMercStats = mercenaryStatRepository.findByMercenaryIdIn(mercenaryIds);
        Map<Long, Map<StatType, Integer>> baseStatsByMercId = allMercStats.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getMercenary().getId(),
                        Collectors.toMap(MercenaryStat::getStatKey, MercenaryStat::getStatValue, (a, b) -> a)
                ));

        // 아이템 스탯
        List<Long> equippedItemIds = allSlots.stream()
                .map(s -> s.getEquipmentItem().getItemId()).distinct().toList();
        Map<Long, List<ItemStat>> itemStatsByItemId = equippedItemIds.isEmpty() ? Map.of() :
                itemStatRepository.findByItemIdIn(equippedItemIds).stream()
                        .collect(Collectors.groupingBy(s -> s.getItem().getId()));

        // 세트 효과
        List<Long> equippedSetIds = allSlots.stream()
                .map(s -> s.getEquipmentItem().getEquipmentSet())
                .filter(Objects::nonNull)
                .map(EquipmentSet::getId)
                .distinct().toList();
        Map<Long, List<EquipmentSetEffect>> setEffectsBySetId = equippedSetIds.isEmpty() ? Map.of() :
                setEffectRepository.findBySetIdIn(equippedSetIds).stream()
                        .collect(Collectors.groupingBy(e -> e.getEquipmentSet().getId()));

        // 스킬 계수 (용병/아이템)
        List<SkillCoefficient> mercCoefs = skillCoefficientRepository.findByMercenaryIdIn(mercenaryIds);
        Map<Long, List<SkillCoefficient>> coefsByMercId = mercCoefs.stream()
                .collect(Collectors.groupingBy(sc -> sc.getMercenarySkill().getMercenary().getId()));
        Map<Long, List<SkillCoefficient>> coefsByItemId = equippedItemIds.isEmpty() ? Map.of() :
                skillCoefficientRepository.findByItemIdIn(equippedItemIds).stream()
                        .collect(Collectors.groupingBy(sc -> sc.getItemSkill().getItem().getId()));

        // ── 특성 레벨 수치 배치 로드 ──────────────────────────────────────────
        List<UserDeckMemberCharacteristic> allMemberChars =
                characteristicRepository.findByDeckMemberIdIn(memberIds);
        Map<Long, List<UserDeckMemberCharacteristic>> charsByMemberId = allMemberChars.stream()
                .collect(Collectors.groupingBy(c -> c.getDeckMember().getId()));

        List<Long> characteristicIds = allMemberChars.stream()
                .map(c -> c.getCharacteristic().getId()).distinct().toList();
        Map<Long, List<MercenaryCharacteristicLevel>> charLevelsByCharId = characteristicIds.isEmpty() ? Map.of() :
                characteristicLevelRepository.findByCharacteristicIdIn(characteristicIds).stream()
                        .collect(Collectors.groupingBy(l -> l.getCharacteristic().getId()));

        // SELF_AUTO 각성 특성 배치 로드 (포인트 배분 없이 자동 적용)
        List<MercenaryCharacteristic> selfAutoChars = mercenaryIds.isEmpty() ? List.of() :
                mercenaryCharacteristicRepository.findSelfAutoByMercenaryIdIn(mercenaryIds);
        Map<Long, List<MercenaryCharacteristic>> selfAutoCharsByMercId = selfAutoChars.stream()
                .collect(Collectors.groupingBy(c -> c.getMercenary().getId()));
        List<Long> selfAutoCharIds = selfAutoChars.stream().map(MercenaryCharacteristic::getId).distinct().toList();
        Map<Long, List<MercenaryCharacteristicLevel>> selfAutoLevelsByCharId = selfAutoCharIds.isEmpty() ? Map.of() :
                characteristicLevelRepository.findByCharacteristicIdIn(selfAutoCharIds).stream()
                        .collect(Collectors.groupingBy(l -> l.getCharacteristic().getId()));

        // ── 주술 세트효과 배치 로드 ────────────────────────────────────────────
        List<Long> equippedRitualIds = allSlots.stream()
                .map(UserDeckMemberSlot::getRitual)
                .filter(Objects::nonNull)
                .map(r -> r.getRitual().getId())
                .distinct().toList();
        Map<RitualSetKey, List<RitualSetEffect>> ritualSetEffectMap =
                buildRitualSetEffectMap(equippedRitualIds, equippedSetIds);
        Map<Long, List<RitualStat>> ritualStatsByRitualId = equippedRitualIds.isEmpty() ? Map.of() :
                ritualStatRepository.findByRitualIdIn(equippedRitualIds).stream()
                        .collect(Collectors.groupingBy(rs -> rs.getRitual().getId()));

        // ── 세트 부여 스킬 계수 배치 로드 ─────────────────────────────────────
        Map<Long, List<EquipmentSetSkillEffect>> setSkillEffectsBySetId = equippedSetIds.isEmpty() ? Map.of() :
                setSkillEffectRepository.findByEquipmentSetIdIn(equippedSetIds).stream()
                        .collect(Collectors.groupingBy(e -> e.getEquipmentSet().getId()));

        List<Long> allSetGrantedSkillIds = setSkillEffectsBySetId.values().stream()
                .flatMap(List::stream).map(e -> e.getSetGrantedSkill().getId()).distinct().toList();
        Map<Long, List<SkillCoefficient>> coefsBySetGrantedSkillId = allSetGrantedSkillIds.isEmpty() ? Map.of() :
                skillCoefficientRepository.findBySetGrantedSkillIdIn(allSetGrantedSkillIds).stream()
                        .collect(Collectors.groupingBy(sc -> sc.getSetGrantedSkill().getId()));

        // ── 파티 버프 · 몬스터 디버프 · 특성 버프 집계 ─────────────────────────────
        Map<StatType, Integer> partyFlatBonus = new EnumMap<>(StatType.class);
        Map<StatType, Integer> partyPercentBonus = new EnumMap<>(StatType.class);
        int[] enemyResistPierceHolder = {0};
        int[] enemyMagicResistDebuff = {0};
        int[] enemyHittingResistDebuff = {0};
        int[] enemyElementDebuff = {0};

        Map<Long, Map<StatType, Integer>> memberSelfFlatPref = new HashMap<>();
        Map<Long, Map<StatType, Integer>> memberSelfPercentPref = new HashMap<>();
        Map<Long, Integer> memberDamagePercentSum = new HashMap<>();
        Map<Long, Map<String, Integer>> memberSkillDamageBonus = new HashMap<>();
        List<PartySkillDamageBuff> partySkillDamageBuffs = new ArrayList<>();

        enemyResistPierceHolder[0] += applyDeckEffects(deck, partyFlatBonus, partyPercentBonus);

        Map<Long, Map<Long, Integer>> charIndexByMercId = buildCharacteristicIndexByMercId(mercenaryIds);

        for (UserDeckMember member : members) {
            memberSelfFlatPref.put(member.getId(), new EnumMap<>(StatType.class));
            memberSelfPercentPref.put(member.getId(), new EnumMap<>(StatType.class));
            memberDamagePercentSum.put(member.getId(), 0);
            memberSkillDamageBonus.put(member.getId(), new HashMap<>());

            accumulateMemberCharacteristicBuffs(
                    member,
                    charsByMemberId.getOrDefault(member.getId(), List.of()),
                    charLevelsByCharId,
                    charIndexByMercId.getOrDefault(member.getMercenary().getId(), Map.of()),
                    memberSelfFlatPref.get(member.getId()),
                    memberSelfPercentPref.get(member.getId()),
                    memberDamagePercentSum,
                    memberSkillDamageBonus.get(member.getId()),
                    partyFlatBonus,
                    partyPercentBonus,
                    partySkillDamageBuffs,
                    enemyResistPierceHolder,
                    enemyMagicResistDebuff,
                    enemyHittingResistDebuff,
                    enemyElementDebuff
            );

            // SELF_AUTO 각성 특성 자동 적용 (포인트 배분 불필요, 항상 활성)
            Map<StatType, Integer> membSelfFlat = memberSelfFlatPref.get(member.getId());
            for (MercenaryCharacteristic selfAutoChar : selfAutoCharsByMercId.getOrDefault(member.getMercenary().getId(), List.of())) {
                for (MercenaryCharacteristicLevel lvl : selfAutoLevelsByCharId.getOrDefault(selfAutoChar.getId(), List.of())) {
                    if (lvl.getStatType() == null || lvl.getAmountValue() == null) continue;
                    accumulate(membSelfFlat, lvl.getStatType(), Math.round(lvl.getAmountValue()));
                }
            }
        }

        int enemyResistPierceBonus = enemyResistPierceHolder[0];

        // 장비·세트 ALLY/ENEMY scope
        for (UserDeckMember member : members) {
            Nature nature = member.getMercenary().getNature();
            List<UserDeckMemberSlot> memberSlots = slotsByMemberId.getOrDefault(member.getId(), List.of());

            for (UserDeckMemberSlot slot : memberSlots) {
                for (ItemStat stat : itemStatsByItemId.getOrDefault(slot.getEquipmentItem().getItemId(), List.of())) {
                    if (stat.getScope() != BuffTarget.ALLY) continue;
                    if (!isElementApplicable(stat.getElement(), nature)) continue;
                    accumulate(stat.getStatUnit() == StatUnit.PERCENT ? partyPercentBonus : partyFlatBonus,
                            stat.getStatType(), stat.getValue());
                }
            }

            Map<Long, Integer> setPieceCount = countSetPieces(memberSlots);
            for (Map.Entry<Long, Integer> entry : setPieceCount.entrySet()) {
                for (EquipmentSetEffect effect : setEffectsBySetId.getOrDefault(entry.getKey(), List.of())) {
                    if (effect.getRequiredPieces() > entry.getValue()) continue;
                    if (!isElementApplicable(effect.getElement(), nature)) continue;
                    if (effect.getScope() == BuffTarget.ALLY) {
                        accumulate(effect.getStatUnit() == StatUnit.PERCENT ? partyPercentBonus : partyFlatBonus,
                                effect.getStatType(), effect.getStatValue());
                    } else if (effect.getScope() == BuffTarget.ENEMY && effect.getStatType() == StatType.RESIST_PIERCE) {
                        enemyResistPierceBonus += effect.getStatValue();
                    }
                }
            }
        }

        // ── 멤버별 유효 스탯 산출 ─────────────────────────────────────────────
        Map<Long, Map<StatType, Integer>> effectiveStatsByMemberId = new HashMap<>();

        for (UserDeckMember member : members) {
            Nature nature = member.getMercenary().getNature();
            List<UserDeckMemberSlot> memberSlots = slotsByMemberId.getOrDefault(member.getId(), List.of());
            Map<StatType, Integer> base = baseStatsByMercId.getOrDefault(member.getMercenary().getId(), Map.of());

            Map<StatType, Integer> selfFlat = new EnumMap<>(memberSelfFlatPref.getOrDefault(member.getId(), Map.of()));
            Map<StatType, Integer> selfPercent = new EnumMap<>(memberSelfPercentPref.getOrDefault(member.getId(), Map.of()));

            // SELF 아이템 스탯
            for (UserDeckMemberSlot slot : memberSlots) {
                for (ItemStat stat : itemStatsByItemId.getOrDefault(slot.getEquipmentItem().getItemId(), List.of())) {
                    if (stat.getScope() != BuffTarget.SELF) continue;
                    if (!isElementApplicable(stat.getElement(), nature)) continue;
                    accumulate(stat.getStatUnit() == StatUnit.PERCENT ? selfPercent : selfFlat,
                            stat.getStatType(), stat.getValue());
                }
            }

            // SELF 주술 스탯 — 슬롯별 outcome(SUCCESS/GREAT_SUCCESS)에 따라 RitualStat 적용
            for (UserDeckMemberSlot slot : memberSlots) {
                if (slot.getRitual() == null) continue;
                Long ritualId = slot.getRitual().getRitual().getId();
                RitualOutcome outcome = slot.getRitual().getOutcome();
                for (RitualStat stat : ritualStatsByRitualId.getOrDefault(ritualId, List.of())) {
                    if (stat.getOutcome() != outcome) continue;
                    if (!isElementApplicable(stat.getElement(), nature)) continue;
                    accumulate(stat.getStatUnit() == StatUnit.PERCENT ? selfPercent : selfFlat,
                            stat.getStatType(), stat.getStatValue());
                }
            }

            // SELF 세트 효과
            Map<Long, Integer> setPieceCount = countSetPieces(memberSlots);
            for (Map.Entry<Long, Integer> entry : setPieceCount.entrySet()) {
                for (EquipmentSetEffect effect : setEffectsBySetId.getOrDefault(entry.getKey(), List.of())) {
                    if (effect.getRequiredPieces() > entry.getValue()) continue;
                    if (effect.getScope() != BuffTarget.SELF) continue;
                    if (!isElementApplicable(effect.getElement(), nature)) continue;
                    accumulate(effect.getStatUnit() == StatUnit.PERCENT ? selfPercent : selfFlat,
                            effect.getStatType(), effect.getStatValue());
                }
            }

            // ② 주술 세트효과
            Map<RitualSetKey, Integer> ritualPieceCount = countRitualSetPieces(memberSlots);
            for (Map.Entry<RitualSetKey, Integer> entry : ritualPieceCount.entrySet()) {
                for (RitualSetEffect effect : ritualSetEffectMap.getOrDefault(entry.getKey(), List.of())) {
                    if (effect.getRequiredRitualPieces() > entry.getValue()) continue;
                    if (!isElementApplicable(effect.getElement(), nature)) continue;
                    accumulate(effect.getStatUnit() == StatUnit.PERCENT ? selfPercent : selfFlat,
                            effect.getStatType(), effect.getStatValue());
                }
            }

            // ③ 진법/층진 속성 지정 버프 (NONE/ADAPTIVE는 partyBonus에 이미 포함)
            applyDeckBuffSourcePerMember(deck.getJinbeopSource(), nature, selfFlat, selfPercent);
            applyDeckBuffSourcePerMember(deck.getCheungjinSource(), nature, selfFlat, selfPercent);

            // 최종 유효 스탯 = (기본 + selfFlat + partyFlat) × (1 + percent/100)
            Map<StatType, Integer> effectiveStats = new EnumMap<>(StatType.class);
            for (StatType type : StatType.values()) {
                int baseVal = base.getOrDefault(type, 0);
                int flat = selfFlat.getOrDefault(type, 0) + partyFlatBonus.getOrDefault(type, 0);
                int pct = selfPercent.getOrDefault(type, 0) + partyPercentBonus.getOrDefault(type, 0);
                int effective = (int) Math.round((baseVal + flat) * (1.0 + pct / 100.0));
                if (effective != 0) effectiveStats.put(type, effective);
            }

            // 주인공 국가 속성 버프
            applyProtagonistNationBuff(members, member, effectiveStats);

            // 각성 명왕 공통 아군 속성값 버프
            applyAwakenedMyeongwangAllyBuff(members, member, effectiveStats);

            effectiveStatsByMemberId.put(member.getId(), effectiveStats);
        }

        // ── 명왕 스탯 이전 패스 ─────────────────────────────────────────────────
        MyungwangStatTransferCalculator.ComputedTransfers myungwangTransfers =
                myungwangStatTransferCalculator.computeReceivedTransfers(
                        members, charsByMemberId, charLevelsByCharId, effectiveStatsByMemberId);
        myungwangTransfers.receivedByMemberId().forEach((targetId, stats) -> {
            Map<StatType, Integer> targetStats = effectiveStatsByMemberId.get(targetId);
            if (targetStats == null) return;
            stats.forEach((statType, amount) -> targetStats.merge(statType, amount, Integer::sum));
        });

        // ── 총 저항깎 · 총 속성깎 · 저항 통과율 ─────────────────────────────────
        Monster monster = monsterRepository.findById(req.monsterId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "몬스터를 찾을 수 없습니다."));

        int memberResistPierce = effectiveStatsByMemberId.values().stream()
                .mapToInt(m -> m.getOrDefault(StatType.RESIST_PIERCE, 0))
                .sum();
        int totalResistPierce = enemyResistPierceBonus + memberResistPierce
                + Math.abs(enemyMagicResistDebuff[0]) + Math.abs(enemyHittingResistDebuff[0]);

        int memberElementPierce = effectiveStatsByMemberId.values().stream()
                .mapToInt(m -> m.getOrDefault(StatType.ELEMENT_PIERCE, 0))
                .sum();
        int totalElementPierce = memberElementPierce + Math.abs(enemyElementDebuff[0]);

        ResistanceType resistType = req.resistanceType() != null ? req.resistanceType() : ResistanceType.HITTING;
        int monsterResist = resistType == ResistanceType.MAGIC
                ? (monster.getMagicResistance() != null ? monster.getMagicResistance() : 0)
                : (monster.getHittingResistance() != null ? monster.getHittingResistance() : 0);

        if (resistType == ResistanceType.MAGIC) {
            monsterResist += enemyMagicResistDebuff[0];
        } else {
            monsterResist += enemyHittingResistDebuff[0];
        }

        int resistAfterDebuff = monsterResist - memberResistPierce - enemyResistPierceBonus;
        double resistPassRate = calcResistPassRate(resistAfterDebuff);

        Element monsterElement = monster.getElement();
        int effectiveMonsterElement = ElementBonusCalculator.hasElementAttribute(monsterElement)
                ? ElementBonusCalculator.effectiveMonsterElementValue(
                        monster.getElementValue(), memberElementPierce, Math.abs(enemyElementDebuff[0]))
                : 0;

        // ── 1패스: 멤버별 DPS 계산 (damageShare 제외) ─────────────────────────

        record MemberIntermediate(
                Long memberId, Long mercenaryId, String mercenaryName,
                int elementValue, double elementBonus,
                long rawDps, long elementAdjustedDps, long adjustedDps,
                List<SkillDpsResult> skillResults) {}

        List<MemberIntermediate> intermediates = new ArrayList<>();

        for (UserDeckMember member : members) {
            MemberDpsInput input = inputMap.getOrDefault(member.getId(),
                    new MemberDpsInput(
                            member.getId(),
                            member.getLevel(),
                            member.getBonusTarget(),
                            member.getBonusAmount()));

            int level = (input.level() == 260) ? 260 : 250;
            int levelStat = (level == 260) ? LEVEL_STAT_260 : LEVEL_STAT_250;

            Map<StatType, Integer> stats = effectiveStatsByMemberId.getOrDefault(member.getId(), Map.of());

            int baseStr = stats.getOrDefault(StatType.STRENGTH, 0);
            int baseDex = stats.getOrDefault(StatType.DEXTERITY, 0);
            int baseVit = stats.getOrDefault(StatType.VITALITY, 0);
            int baseInt = stats.getOrDefault(StatType.INTELLECT, 0);
            int baseAtk = stats.getOrDefault(StatType.ATTACK_POWER, 0);

            List<UserDeckMemberSlot> memberSlots = slotsByMemberId.getOrDefault(member.getId(), List.of());
            List<SkillCoefficient> skillCoefs = resolveSkillCoefs(
                    member, memberSlots, coefsByMercId, coefsByItemId,
                    setSkillEffectsBySetId, coefsBySetGrantedSkillId);

            List<SkillDpsResult> skillResults = new ArrayList<>();
            double memberRawDps = 0.0;

            for (SkillCoefficient coef : skillCoefs) {
                StatType mainStat = MemberBuildStatCalculator.resolveMainStat(coef);

                int str = baseStr, dex = baseDex, vit = baseVit, intel = baseInt;

                switch (mainStat) {
                    case STRENGTH  -> str   += levelStat;
                    case DEXTERITY -> dex   += levelStat;
                    case VITALITY  -> vit   += levelStat;
                    case INTELLECT -> intel += levelStat;
                    default -> {}
                }

                if (input.bonusTarget() == BonusStatTarget.VITALITY) {
                    vit += input.bonusAmount();
                } else {
                    switch (mainStat) {
                        case STRENGTH  -> str   += input.bonusAmount();
                        case DEXTERITY -> dex   += input.bonusAmount();
                        case VITALITY  -> vit   += input.bonusAmount();
                        case INTELLECT -> intel += input.bonusAmount();
                        default -> str += input.bonusAmount();
                    }
                }

                double rawDmg = coef.getCoefStr() * str
                        + coef.getCoefDex() * dex
                        + coef.getCoefVit() * vit
                        + coef.getCoefInt() * intel
                        + coef.getCoefAtk() * baseAtk
                        + coef.getCoefLvl() * level;

                double skillDps = 0.0;
                boolean calculated = false;

                if (coef.getSkillType() == SkillType.INSTANT && coef.getCastsPerSecond() != null
                        && coef.getCastsPerSecond() > 0) {
                    skillDps = rawDmg * damageMultiplier(member, resolveSkillName(coef),
                            memberDamagePercentSum, memberSkillDamageBonus, partySkillDamageBuffs)
                            * coef.getHitCount() * coef.getCastsPerSecond();
                    calculated = true;
                } else if (coef.getSkillType() == SkillType.PERSISTENT && coef.getTickIntervalMs() != null
                        && coef.getTickIntervalMs() > 0) {
                    skillDps = rawDmg * damageMultiplier(member, resolveSkillName(coef),
                            memberDamagePercentSum, memberSkillDamageBonus, partySkillDamageBuffs)
                            / (coef.getTickIntervalMs() / 1000.0);
                    calculated = true;
                }

                skillResults.add(new SkillDpsResult(resolveSkillName(coef), skillDps, calculated));
                memberRawDps += skillDps;
            }

            int memberElementValue = stats.getOrDefault(StatType.ELEMENT_VALUE, 0);
            double elementBonus = ElementBonusCalculator.calcElementBonus(
                    memberElementValue, effectiveMonsterElement, monsterElement);
            long memberRawDpsRounded         = Math.round(memberRawDps);
            long memberElementAdjustedDps    = Math.round(memberRawDps * (100.0 + elementBonus) / 100.0);
            long memberAdjustedDps           = Math.round(memberElementAdjustedDps * (resistPassRate / 100.0));

            intermediates.add(new MemberIntermediate(
                    member.getId(), member.getMercenary().getId(), member.getMercenary().getName(),
                    memberElementValue, elementBonus,
                    memberRawDpsRounded, memberElementAdjustedDps, memberAdjustedDps,
                    skillResults));
        }

        // ── 2패스: 집계 후 damageShare 계산 ────────────────────────────────────

        long rawTotalDps    = intermediates.stream().mapToLong(MemberIntermediate::rawDps).sum();
        long adjustTotalDps = intermediates.stream().mapToLong(MemberIntermediate::elementAdjustedDps).sum();
        long totalDps       = intermediates.stream().mapToLong(MemberIntermediate::adjustedDps).sum();

        List<MemberDpsResult> memberResults = intermediates.stream()
                .map(im -> new MemberDpsResult(
                        im.memberId(), im.mercenaryId(), im.mercenaryName(),
                        im.elementValue(), im.elementBonus(),
                        im.rawDps(), im.elementAdjustedDps(), im.adjustedDps(),
                        adjustTotalDps > 0 ? (double) im.elementAdjustedDps() / adjustTotalDps * 100.0 : 0.0,
                        im.skillResults()))
                .toList();

        return new DpsResponse(monster.getId(), monster.getName(),
                totalResistPierce, resistAfterDebuff, resistPassRate,
                totalElementPierce, effectiveMonsterElement,
                rawTotalDps, adjustTotalDps, totalDps, memberResults);
    }

    // ── 내부 유틸 ──────────────────────────────────────────────────────────────

    /** 슬롯 목록에서 세트별 착용 피스 수를 집계한다. */
    private Map<Long, Integer> countSetPieces(List<UserDeckMemberSlot> slots) {
        Map<Long, Integer> count = new HashMap<>();
        for (UserDeckMemberSlot slot : slots) {
            EquipmentSet set = slot.getEquipmentItem().getEquipmentSet();
            if (set != null) count.merge(set.getId(), 1, Integer::sum);
        }
        return count;
    }

    /**
     * 슬롯 목록에서 (ritualId, outcome, setId) 기준 주술 피스 수를 집계한다.
     * 주술 없는 슬롯 또는 세트에 속하지 않는 슬롯은 건너뛴다.
     */
    private Map<RitualSetKey, Integer> countRitualSetPieces(List<UserDeckMemberSlot> slots) {
        Map<RitualSetKey, Integer> count = new HashMap<>();
        for (UserDeckMemberSlot slot : slots) {
            if (slot.getRitual() == null) continue;
            EquipmentSet set = slot.getEquipmentItem().getEquipmentSet();
            if (set == null) continue;
            RitualSetKey key = new RitualSetKey(
                    slot.getRitual().getRitual().getId(),
                    slot.getRitual().getOutcome(),
                    set.getId());
            count.merge(key, 1, Integer::sum);
        }
        return count;
    }

    /**
     * 주술 세트효과 맵을 빌드한다.
     * ritualId IN + setId IN 배치 조회 후 RitualSetKey 기준으로 그룹핑한다.
     */
    private Map<RitualSetKey, List<RitualSetEffect>> buildRitualSetEffectMap(
            List<Long> ritualIds, List<Long> setIds) {
        if (ritualIds.isEmpty() || setIds.isEmpty()) return Map.of();
        return ritualSetEffectRepository.findByRitualIdInAndEquipmentSetIdIn(ritualIds, setIds)
                .stream()
                .collect(Collectors.groupingBy(e -> new RitualSetKey(
                        e.getRitual().getId(), e.getOutcome(), e.getEquipmentSet().getId())));
    }

    /**
     * 슬롯 목록에서 달성된 세트 부여 스킬 ID 목록을 반환한다.
     * 세트 피스 중 effect.enhancement 이상인 피스 수가 requiredPieces 이상이면 달성.
     */
    private List<Long> resolveAchievedSetGrantedSkills(
            List<UserDeckMemberSlot> slots,
            Map<Long, List<EquipmentSetSkillEffect>> setSkillEffectsBySetId) {

        // setId → (enhancement → pieceCount)
        Map<Long, Map<Enhancement, Integer>> piecesByEnhancement = new HashMap<>();
        for (UserDeckMemberSlot slot : slots) {
            EquipmentSet set = slot.getEquipmentItem().getEquipmentSet();
            if (set == null) continue;
            Enhancement enh = slot.getEquipmentItem().getEnhancement();
            if (enh == null) enh = Enhancement.NONE;
            piecesByEnhancement
                    .computeIfAbsent(set.getId(), k -> new EnumMap<>(Enhancement.class))
                    .merge(enh, 1, Integer::sum);
        }

        List<Long> achieved = new ArrayList<>();
        for (Map.Entry<Long, Map<Enhancement, Integer>> setEntry : piecesByEnhancement.entrySet()) {
            for (EquipmentSetSkillEffect effect : setSkillEffectsBySetId.getOrDefault(setEntry.getKey(), List.of())) {
                int effectEnhValue = effect.getEnhancement() != null ? effect.getEnhancement().getValue() : 0;
                int qualifyingPieces = setEntry.getValue().entrySet().stream()
                        .filter(e -> e.getKey().getValue() >= effectEnhValue)
                        .mapToInt(Map.Entry::getValue)
                        .sum();
                if (qualifyingPieces >= effect.getRequiredPieces()) {
                    achieved.add(effect.getSetGrantedSkill().getId());
                }
            }
        }
        return achieved;
    }

    /**
     * 스킬 계수 결정.
     * 기본: 아이템 스킬 우선 → 없으면 용병 스킬.
     * 추가: 세트 부여 스킬 (StatSource.AFFINITY / TriggerSource.MERCENARY 제외).
     */
    private List<SkillCoefficient> resolveSkillCoefs(
            UserDeckMember member,
            List<UserDeckMemberSlot> slots,
            Map<Long, List<SkillCoefficient>> coefsByMercId,
            Map<Long, List<SkillCoefficient>> coefsByItemId,
            Map<Long, List<EquipmentSetSkillEffect>> setSkillEffectsBySetId,
            Map<Long, List<SkillCoefficient>> coefsBySetGrantedSkillId) {

        List<SkillCoefficient> result = new ArrayList<>();

        // 아이템 스킬 우선 (첫 번째 발견 슬롯)
        for (UserDeckMemberSlot slot : slots) {
            List<SkillCoefficient> itemCoefs = coefsByItemId.get(slot.getEquipmentItem().getItemId());
            if (itemCoefs != null && !itemCoefs.isEmpty()) {
                result.addAll(itemCoefs);
                break;
            }
        }

        // 아이템 스킬 없으면 용병 스킬
        if (result.isEmpty()) {
            result.addAll(coefsByMercId.getOrDefault(member.getMercenary().getId(), List.of()));
        }

        // 세트 부여 스킬 추가 (인연 연결 미지원 항목 제외)
        for (Long sgId : resolveAchievedSetGrantedSkills(slots, setSkillEffectsBySetId)) {
            coefsBySetGrantedSkillId.getOrDefault(sgId, List.of()).stream()
                    .filter(sc -> isSetGrantedSkillCalculable(sc.getSetGrantedSkill()))
                    .forEach(result::add);
        }

        return result;
    }

    /** StatSource.AFFINITY 또는 TriggerSource.MERCENARY는 MVP에서 계산 제외한다. */
    private boolean isSetGrantedSkillCalculable(SetGrantedSkill skill) {
        return skill.getStatSource() != StatSource.AFFINITY
                && skill.getTriggerSource() != TriggerSource.MERCENARY;
    }

    /**
     * 덱 단위 효과를 파티 버프와 적 디버프로 반영한다.
     * 반환값은 적 저항깎 증가분이다.
     */
    private int applyDeckEffects(UserDeck deck,
                                 Map<StatType, Integer> partyFlatBonus,
                                 Map<StatType, Integer> partyPercentBonus) {
        int enemyResistPierce = 0;

        List<Spirit> spirits = selectedSpirits(deck);
        boolean hasEarthLegend = spirits.stream()
                .anyMatch(spirit -> spirit.getNature() == Nature.EARTH
                        && spirit.getGrade() == SpiritGrade.LEGEND);

        for (Spirit spirit : spirits) {
            boolean isEarthLegend = spirit.getNature() == Nature.EARTH
                    && spirit.getGrade() == SpiritGrade.LEGEND;
            for (SpiritBuff buff : spirit.getBuffs()) {
                float value = buff.getValue();
                if (hasEarthLegend && !isEarthLegend
                        && !SPIRIT_DOUBLE_EXCLUDED_STATS.contains(buff.getStatType())) {
                    value *= 2.0f;
                }
                if (buff.getTarget() == BuffTarget.ALLY) {
                    accumulate(partyFlatBonus, buff.getStatType(), Math.round(value));
                } else if (buff.getTarget() == BuffTarget.ENEMY
                        && buff.getStatType() == StatType.RESIST_PIERCE) {
                    enemyResistPierce += Math.round(value);
                }
            }
        }

        enemyResistPierce += applyDeckBuffSource(deck.getJinbeopSource(), partyFlatBonus, partyPercentBonus);
        enemyResistPierce += applyDeckBuffSource(deck.getCheungjinSource(), partyFlatBonus, partyPercentBonus);
        return enemyResistPierce;
    }

    private List<Spirit> selectedSpirits(UserDeck deck) {
        List<Spirit> spirits = new ArrayList<>();
        if (deck.getSpirit1() != null) spirits.add(deck.getSpirit1());
        if (deck.getSpirit2() != null) spirits.add(deck.getSpirit2());
        return spirits;
    }

    /**
     * 속성 무관(NONE/ADAPTIVE) ALLY 버프와 ENEMY 디버프만 전역 맵에 누적한다.
     * 속성 지정(FIRE/WATER 등) ALLY 버프는 멤버별로 적용해야 하므로 여기서 제외한다.
     */
    private int applyDeckBuffSource(DeckBuffSource source,
                                    Map<StatType, Integer> partyFlatBonus,
                                    Map<StatType, Integer> partyPercentBonus) {
        if (source == null) return 0;

        int enemyResistPierce = 0;
        for (DeckBuff buff : source.getBuffs()) {
            int value = Math.round(buff.getValue());
            if (buff.getTarget() == BuffTarget.ALLY
                    && (buff.getElement() == Element.NONE || buff.getElement() == Element.ADAPTIVE)) {
                accumulate(buff.getValueType() == BuffValueType.PERCENT_ADD ? partyPercentBonus : partyFlatBonus,
                        buff.getStatType(), value);
            } else if (buff.getTarget() == BuffTarget.ENEMY
                    && buff.getStatType() == StatType.RESIST_PIERCE) {
                enemyResistPierce += Math.abs(value);
            }
        }
        return enemyResistPierce;
    }

    /**
     * 속성 지정 ALLY 버프를 멤버 nature에 맞춰 selfFlat/selfPercent에 적용한다.
     * NONE/ADAPTIVE는 이미 전역 파티 버프에 포함되어 있으므로 건너뛴다.
     */
    private void applyDeckBuffSourcePerMember(DeckBuffSource source, Nature nature,
                                              Map<StatType, Integer> selfFlat,
                                              Map<StatType, Integer> selfPercent) {
        if (source == null) return;
        for (DeckBuff buff : source.getBuffs()) {
            if (buff.getTarget() != BuffTarget.ALLY) continue;
            if (buff.getElement() == Element.NONE || buff.getElement() == Element.ADAPTIVE) continue;
            if (!isElementApplicable(buff.getElement(), nature)) continue;
            int value = Math.round(buff.getValue());
            accumulate(buff.getValueType() == BuffValueType.PERCENT_ADD ? selfPercent : selfFlat,
                    buff.getStatType(), value);
        }
    }

    /** 스킬 계수의 표시 이름을 반환한다. */
    private String resolveSkillName(SkillCoefficient coef) {
        if (coef.isMercenarySkill()) return coef.getMercenarySkill().getSkillName();
        if (coef.isItemSkill()) return coef.getItemSkill().getSkillName();
        return coef.getSetGrantedSkill().getSkillName();
    }

    /**
     * 속성 적용 가능 여부 판단.
     * NONE·ADAPTIVE: 항상 적용. 특정 속성: 용병 속성(Nature)과 이름 일치 시에만 적용.
     */
    private boolean isElementApplicable(Element element, Nature nature) {
        return switch (element) {
            case NONE, ADAPTIVE -> true;
            default -> element.name().equals(nature.name());
        };
    }

    /** 스탯 맵에 값을 누적한다. */
    private void accumulate(Map<StatType, Integer> map, StatType type, int value) {
        map.merge(type, value, Integer::sum);
    }

    /** ALLY scope 데미지% 버프 — element 조건으로 DPS 계산 시 필터링 */
    private record PartySkillDamageBuff(Element element, int percent) {}

    /** 용병별 MercenaryCharacteristic.id → characteristicIndex 매핑 (전설장수 연동용) */
    private Map<Long, Map<Long, Integer>> buildCharacteristicIndexByMercId(List<Long> mercenaryIds) {
        Map<Long, Map<Long, Integer>> result = new HashMap<>();
        for (Long mercenaryId : mercenaryIds) {
            List<MercenaryCharacteristic> characteristics =
                    mercenaryCharacteristicRepository.findByMercenaryId(mercenaryId).stream()
                            .sorted(Comparator.comparing(MercenaryCharacteristic::getId))
                            .toList();
            Map<Long, Integer> indexByCharId = new HashMap<>();
            for (int i = 0; i < characteristics.size(); i++) {
                indexByCharId.put(characteristics.get(i).getId(), i);
            }
            result.put(mercenaryId, indexByCharId);
        }
        return result;
    }

    /**
     * 멤버 특성·전설장수 효과를 scope에 따라 SELF/ALLY/ENEMY 버킷에 분배한다.
     */
    private void accumulateMemberCharacteristicBuffs(
            UserDeckMember member,
            List<UserDeckMemberCharacteristic> memberChars,
            Map<Long, List<MercenaryCharacteristicLevel>> charLevelsByCharId,
            Map<Long, Integer> charIndexByCharId,
            Map<StatType, Integer> selfFlat,
            Map<StatType, Integer> selfPercent,
            Map<Long, Integer> memberDamagePercentSum,
            Map<String, Integer> skillDamageBonus,
            Map<StatType, Integer> partyFlatBonus,
            Map<StatType, Integer> partyPercentBonus,
            List<PartySkillDamageBuff> partySkillDamageBuffs,
            int[] enemyResistPierceHolder,
            int[] enemyMagicResistDebuff,
            int[] enemyHittingResistDebuff,
            int[] enemyElementDebuff) {

        MercenaryCategory category = member.getMercenary().getCategory();

        // MercenaryCharacteristicLevel 기반 (사천왕·명왕·주인공 등)
        for (UserDeckMemberCharacteristic mc : memberChars) {
            MercenaryCharacteristic characteristic = mc.getCharacteristic();
            int selectedLevel = mc.getSelectedLevel();
            if (selectedLevel <= 0) continue;

            for (MercenaryCharacteristicLevel lvl : charLevelsByCharId.getOrDefault(characteristic.getId(), List.of())) {
                if (!lvl.getLevel().equals(selectedLevel)) continue;

                ScopedEffect scoped = CharacteristicScopeResolver.resolve(lvl);
                if (scoped == null || scoped.mode() == ApplicationMode.SKIP) continue;

                int value = Math.round(lvl.getAmountValue());
                applyScopedEffect(
                        scoped, lvl.getStatType(), value, member.getMercenary().getNature(),
                        selfFlat, selfPercent, member.getId(), memberDamagePercentSum, skillDamageBonus,
                        partyFlatBonus, partyPercentBonus, partySkillDamageBuffs,
                        enemyResistPierceHolder, enemyMagicResistDebuff,
                        enemyHittingResistDebuff, enemyElementDebuff, Element.NONE);
            }
        }

        // 전설장수 — CharacteristicEffect에 명시된 target 사용
        if (category == MercenaryCategory.LEGENDARY_GENERAL) {
            legendGeneralLoadService.loadForCalculation(member.getMercenary().getId())
                    .ifPresent(lg -> applyLegendGeneralBuffs(
                            lg, member, charIndexByCharId, memberChars,
                            selfFlat, selfPercent, memberDamagePercentSum,
                            partyFlatBonus, partyPercentBonus, partySkillDamageBuffs,
                            enemyResistPierceHolder, enemyMagicResistDebuff,
                            enemyHittingResistDebuff, enemyElementDebuff));
        }
    }

    private void applyLegendGeneralBuffs(
            LegendGeneral legendGeneral,
            UserDeckMember member,
            Map<Long, Integer> charIndexByCharId,
            List<UserDeckMemberCharacteristic> memberChars,
            Map<StatType, Integer> selfFlat,
            Map<StatType, Integer> selfPercent,
            Map<Long, Integer> memberDamagePercentSum,
            Map<StatType, Integer> partyFlatBonus,
            Map<StatType, Integer> partyPercentBonus,
            List<PartySkillDamageBuff> partySkillDamageBuffs,
            int[] enemyResistPierceHolder,
            int[] enemyMagicResistDebuff,
            int[] enemyHittingResistDebuff,
            int[] enemyElementDebuff) {

        Map<Integer, Integer> pointsMap = new HashMap<>();
        for (UserDeckMemberCharacteristic mc : memberChars) {
            Integer index = charIndexByCharId.get(mc.getCharacteristic().getId());
            if (index != null) {
                pointsMap.put(index, mc.getSelectedLevel());
            }
        }

        Map<BuffKey, Float> deckBuffs = legendGeneralBuffCalculator.calculate(
                legendGeneral, member.getLevel(), pointsMap);
        for (Map.Entry<BuffKey, Float> entry : deckBuffs.entrySet()) {
            BuffKey key = entry.getKey();
            int value = Math.round(entry.getValue());
            ScopedEffect scoped = new ScopedEffect(key.target(),
                    isSkillDamageStat(key.statType()) ? ApplicationMode.SKILL_DAMAGE : ApplicationMode.STAT,
                    isPercentStatType(key.statType()));
            applyScopedEffect(scoped, key.statType(), value, member.getMercenary().getNature(),
                    selfFlat, selfPercent, member.getId(), memberDamagePercentSum, Map.of(),
                    partyFlatBonus, partyPercentBonus, partySkillDamageBuffs,
                    enemyResistPierceHolder, enemyMagicResistDebuff,
                    enemyHittingResistDebuff, enemyElementDebuff, key.element());
        }

        Map<BuffKey, Float> selfBuffs = legendGeneralBuffCalculator.calculateSelfBuffs(legendGeneral, pointsMap);
        for (Map.Entry<BuffKey, Float> entry : selfBuffs.entrySet()) {
            BuffKey key = entry.getKey();
            int value = Math.round(entry.getValue());
            ScopedEffect scoped = new ScopedEffect(BuffTarget.SELF,
                    isSkillDamageStat(key.statType()) ? ApplicationMode.SKILL_DAMAGE : ApplicationMode.STAT,
                    isPercentStatType(key.statType()));
            applyScopedEffect(scoped, key.statType(), value, member.getMercenary().getNature(),
                    selfFlat, selfPercent, member.getId(), memberDamagePercentSum, Map.of(),
                    partyFlatBonus, partyPercentBonus, partySkillDamageBuffs,
                    enemyResistPierceHolder, enemyMagicResistDebuff,
                    enemyHittingResistDebuff, enemyElementDebuff, key.element());
        }
    }

    private void applyScopedEffect(
            ScopedEffect scoped,
            StatType statType,
            int value,
            Nature providerNature,
            Map<StatType, Integer> selfFlat,
            Map<StatType, Integer> selfPercent,
            Long memberId,
            Map<Long, Integer> memberDamagePercentSum,
            Map<String, Integer> skillDamageBonus,
            Map<StatType, Integer> partyFlatBonus,
            Map<StatType, Integer> partyPercentBonus,
            List<PartySkillDamageBuff> partySkillDamageBuffs,
            int[] enemyResistPierceHolder,
            int[] enemyMagicResistDebuff,
            int[] enemyHittingResistDebuff,
            int[] enemyElementDebuff,
            Element element) {

        if (scoped.mode() == ApplicationMode.SKILL_DAMAGE) {
            if (scoped.target() == BuffTarget.SELF) {
                if (scoped.targetSkillName() != null) {
                    skillDamageBonus.merge(scoped.targetSkillName(), value, Integer::sum);
                } else {
                    memberDamagePercentSum.merge(memberId, value, Integer::sum);
                }
            } else if (scoped.target() == BuffTarget.ALLY) {
                partySkillDamageBuffs.add(new PartySkillDamageBuff(element, value));
            }
            return;
        }

        if (scoped.mode() == ApplicationMode.SKIP || statType == null) return;

        switch (scoped.target()) {
            case SELF -> {
                if (scoped.percent()) {
                    accumulate(selfPercent, statType, value);
                } else {
                    accumulate(selfFlat, statType, value);
                }
            }
            case ALLY -> {
                if (scoped.percent()) {
                    accumulate(partyPercentBonus, statType, value);
                } else {
                    accumulate(partyFlatBonus, statType, value);
                }
            }
            case ENEMY -> applyEnemyDebuff(statType, value, enemyResistPierceHolder,
                    enemyMagicResistDebuff, enemyHittingResistDebuff, enemyElementDebuff);
        }
    }

    private void applyEnemyDebuff(StatType statType, int value,
                                int[] enemyResistPierceHolder,
                                int[] enemyMagicResistDebuff,
                                int[] enemyHittingResistDebuff,
                                int[] enemyElementDebuff) {
        switch (statType) {
            case RESIST_PIERCE -> enemyResistPierceHolder[0] += Math.abs(value);
            case MAGIC_RESISTANCE -> enemyMagicResistDebuff[0] += value;
            case HITTING_RESISTANCE -> enemyHittingResistDebuff[0] += value;
            case ELEMENT_VALUE -> enemyElementDebuff[0] += value;
            default -> { /* DPS 계산 대상 외 ENEMY 디버프는 무시 */ }
        }
    }

    private boolean isSkillDamageStat(StatType statType) {
        return statType == StatType.SKILL_DAMAGE_PERCENT
                || statType == StatType.DAMAGE_PERCENT
                || statType == StatType.DAMAGE_PERCENT_GROUND
                || statType == StatType.DAMAGE_PERCENT_AIR;
    }

    private boolean isPercentStatType(StatType statType) {
        return statType == StatType.SKILL_DAMAGE_PERCENT
                || statType == StatType.DAMAGE_PERCENT
                || statType == StatType.DAMAGE_PERCENT_GROUND
                || statType == StatType.DAMAGE_PERCENT_AIR
                || statType == StatType.MAGIC_RESISTANCE
                || statType == StatType.HITTING_RESISTANCE
                || statType == StatType.ATTACK_SPEED
                || statType == StatType.MOVE_SPEED
                || statType == StatType.CRITICAL_CHANCE;
    }

    /** 스킬 DPS에 적용할 damage_percent 합산 배율 */
    private double damageMultiplier(
            UserDeckMember member,
            String skillName,
            Map<Long, Integer> memberDamagePercentSum,
            Map<Long, Map<String, Integer>> memberSkillDamageBonus,
            List<PartySkillDamageBuff> partySkillDamageBuffs) {

        Nature nature = member.getMercenary().getNature();
        int partyPct = partySkillDamageBuffs.stream()
                .filter(buff -> isElementApplicable(buff.element(), nature))
                .mapToInt(PartySkillDamageBuff::percent)
                .sum();

        int selfPct = memberDamagePercentSum.getOrDefault(member.getId(), 0);
        int skillPct = memberSkillDamageBonus.getOrDefault(member.getId(), Map.of())
                .getOrDefault(skillName, 0);

        return (100.0 + partyPct + selfPct + skillPct) / 100.0;
    }

    /** 덱 내 주인공 국가 속성 버프 — 해당 속성 용병 ELEMENT_VALUE 가산 */
    private void applyProtagonistNationBuff(List<UserDeckMember> members,
                                            UserDeckMember target,
                                            Map<StatType, Integer> effectiveStats) {
        members.stream()
                .filter(m -> m.getMercenary().getCategory() == MercenaryCategory.PROTAGONIST)
                .findFirst()
                .ifPresent(protagonist -> {
                    var nationBuff = playerCharacterBuffCalculator.getNationBuff(
                            protagonist.getMercenary().getNation());
                    if (nationBuff.value() <= 0 || nationBuff.element() == Element.NONE) return;
                    if (!nationBuff.element().name().equals(target.getMercenary().getNature().name())) return;
                    effectiveStats.merge(StatType.ELEMENT_VALUE,
                            Math.round(nationBuff.value()), Integer::sum);
                });
    }

    /** 각성 명왕 N명 × (전원 +5, EARTH +2 추가) — N명이면 N배 중첩 */
    private void applyAwakenedMyeongwangAllyBuff(List<UserDeckMember> members,
                                                  UserDeckMember target,
                                                  Map<StatType, Integer> effectiveStats) {
        long awakenedCount = members.stream()
                .filter(m -> m.getMercenary().getCategory() == MercenaryCategory.MYEONG_KING_AWAKENING)
                .count();
        if (awakenedCount == 0) return;

        var allyBuff = awakenedMyungwangBuffCalculator.getCommonAllyBuff();
        int bonus = (int) (Math.round(allyBuff.defaultValue()) * awakenedCount);
        if (target.getMercenary().getNature() == Nature.EARTH) {
            bonus += (int) (Math.round(allyBuff.earthValue()) * awakenedCount);
        }
        effectiveStats.merge(StatType.ELEMENT_VALUE, bonus, Integer::sum);
    }

    private double calcResistPassRate(int resistAfterDebuff) {
        if (resistAfterDebuff >= RESIST_CAP) return RESIST_CAP_PASS_RATE;
        return Math.max(0.0, 100.0 - (resistAfterDebuff * 0.16 + 57.0));
    }

}
