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
import org.example.gersangtrade.catalog.repository.ItemStatRepository;
import org.example.gersangtrade.catalog.repository.MercenaryStatRepository;
import org.example.gersangtrade.catalog.repository.MonsterRepository;
import org.example.gersangtrade.catalog.repository.SkillCoefficientRepository;
import org.example.gersangtrade.deck.repository.UserDeckMemberRepository;
import org.example.gersangtrade.deck.repository.UserDeckMemberSlotRepository;
import org.example.gersangtrade.deck.repository.UserDeckRepository;
import org.example.gersangtrade.domain.catalog.EquipmentSet;
import org.example.gersangtrade.domain.catalog.EquipmentSetEffect;
import org.example.gersangtrade.domain.catalog.ItemStat;
import org.example.gersangtrade.domain.catalog.MercenaryStat;
import org.example.gersangtrade.domain.catalog.Monster;
import org.example.gersangtrade.domain.catalog.SkillCoefficient;
import org.example.gersangtrade.domain.catalog.enums.BuffTarget;
import org.example.gersangtrade.domain.catalog.enums.Element;
import org.example.gersangtrade.domain.catalog.enums.Nature;
import org.example.gersangtrade.domain.catalog.enums.SkillType;
import org.example.gersangtrade.domain.catalog.enums.StatType;
import org.example.gersangtrade.domain.catalog.enums.StatUnit;
import org.example.gersangtrade.domain.deck.UserDeckMember;
import org.example.gersangtrade.domain.deck.UserDeckMemberSlot;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * DPS 계산기 서비스.
 *
 * <p>계산 흐름:
 * <ol>
 *   <li>덱 멤버 목록 및 장착 슬롯 일괄 로드
 *   <li>용병 스탯 + 아이템 스탯 + 세트 효과 + 스킬 계수 일괄 로드 (배치 쿼리)
 *   <li>파티 버프(PARTY scope) 사전 집계 — 모든 멤버의 아이템·세트 효과 합산
 *   <li>멤버별 유효 스탯 산출 = 기본 스탯 + 아이템 SELF 스탯 + 세트 SELF 효과 + 파티 버프
 *   <li>총 저항깎 산출 = 전 멤버 RESIST_PIERCE 합 + 세트 ENEMY 디버프
 *   <li>스킬 계수 결정 → 원데미지 → DPS (INSTANT / PERSISTENT)
 *   <li>저항 통과율 × 속성 보정 → 최종 DPS
 * </ol>
 *
 * <p>현재 버전에서 적용하지 않는 항목:
 * <ul>
 *   <li>사천왕·명왕·전설장수 버프 (별도 버프 계산기 존재, 추후 통합 예정)
 *   <li>레벨별 공격력 공식 (용병마다 상이 — mercenary_stats의 ATTACK_POWER 사용)
 *   <li>StatUnit.LEVEL 단계 효과 (이속 등 — DPS에 직접 영향 없음)
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
    private static final double ELEMENT_BONUS_MIN = -50.0;
    private static final double ELEMENT_BONUS_MAX = 50.0;

    private final UserDeckRepository deckRepository;
    private final UserDeckMemberRepository memberRepository;
    private final UserDeckMemberSlotRepository slotRepository;
    private final MercenaryStatRepository mercenaryStatRepository;
    private final ItemStatRepository itemStatRepository;
    private final EquipmentSetEffectRepository setEffectRepository;
    private final SkillCoefficientRepository skillCoefficientRepository;
    private final MonsterRepository monsterRepository;

    public DpsResponse calculate(DpsRequest req) {
        if (!deckRepository.existsById(req.deckId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "덱을 찾을 수 없습니다.");
        }

        List<UserDeckMember> members = memberRepository.findByDeckIdWithMercenary(req.deckId());
        if (members.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "덱에 용병이 없습니다.");
        }

        Map<Long, MemberDpsInput> inputMap = req.memberInputs() == null ? Map.of() :
                req.memberInputs().stream().collect(Collectors.toMap(MemberDpsInput::memberId, i -> i));

        // ── 배치 로드 ──────────────────────────────────────────────────────────

        List<Long> memberIds = members.stream().map(UserDeckMember::getId).toList();
        List<Long> mercenaryIds = members.stream().map(m -> m.getMercenary().getId()).toList();

        List<UserDeckMemberSlot> allSlots = slotRepository.findByDeckMemberIdIn(memberIds);
        Map<Long, List<UserDeckMemberSlot>> slotsByMemberId = allSlots.stream()
                .collect(Collectors.groupingBy(s -> s.getDeckMember().getId()));

        // 용병 기본 스탯 (mercenary_stats 테이블)
        List<MercenaryStat> allMercStats = mercenaryStatRepository.findByMercenaryIdIn(mercenaryIds);
        Map<Long, Map<StatType, Integer>> baseStatsByMercId = allMercStats.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getMercenary().getId(),
                        Collectors.toMap(MercenaryStat::getStatKey, MercenaryStat::getStatValue, (a, b) -> a)
                ));

        // 아이템 스탯 (item_stats 테이블)
        List<Long> equippedItemIds = allSlots.stream()
                .map(s -> s.getEquipmentItem().getItemId()).distinct().toList();
        Map<Long, List<ItemStat>> itemStatsByItemId = equippedItemIds.isEmpty() ? Map.of() :
                itemStatRepository.findByItemIdIn(equippedItemIds).stream()
                        .collect(Collectors.groupingBy(s -> s.getItem().getId()));

        // 세트 효과 (equipment_set_effects 테이블)
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

        // ── 파티 버프 · 몬스터 디버프 집계 ───────────────────────────────────────
        // 모든 멤버의 PARTY scope 아이템·세트 효과를 하나로 합산한다.
        // ENEMY scope 세트 효과(RESIST_PIERCE)는 몬스터 저항 디버프로 처리한다.
        Map<StatType, Integer> partyFlatBonus = new EnumMap<>(StatType.class);
        Map<StatType, Integer> partyPercentBonus = new EnumMap<>(StatType.class);
        int enemyResistPierceBonus = 0;

        for (UserDeckMember member : members) {
            Nature nature = member.getMercenary().getNature();
            List<UserDeckMemberSlot> memberSlots = slotsByMemberId.getOrDefault(member.getId(), List.of());

            // 아이템 PARTY 버프
            for (UserDeckMemberSlot slot : memberSlots) {
                for (ItemStat stat : itemStatsByItemId.getOrDefault(slot.getEquipmentItem().getItemId(), List.of())) {
                    if (stat.getScope() != BuffTarget.ALLY) continue;
                    if (!isElementApplicable(stat.getElement(), nature)) continue;
                    accumulate(stat.getStatUnit() == StatUnit.PERCENT ? partyPercentBonus : partyFlatBonus,
                            stat.getStatType(), stat.getValue());
                }
            }

            // 세트 PARTY·ENEMY 효과
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
        // SELF 아이템·세트 효과 + 파티 버프를 합산한 최종 스탯 맵을 멤버별로 보관한다.
        Map<Long, Map<StatType, Integer>> effectiveStatsByMemberId = new HashMap<>();

        for (UserDeckMember member : members) {
            Nature nature = member.getMercenary().getNature();
            List<UserDeckMemberSlot> memberSlots = slotsByMemberId.getOrDefault(member.getId(), List.of());
            Map<StatType, Integer> base = baseStatsByMercId.getOrDefault(member.getMercenary().getId(), Map.of());

            // SELF 아이템 스탯 누적
            Map<StatType, Integer> selfFlat = new EnumMap<>(StatType.class);
            Map<StatType, Integer> selfPercent = new EnumMap<>(StatType.class);

            for (UserDeckMemberSlot slot : memberSlots) {
                for (ItemStat stat : itemStatsByItemId.getOrDefault(slot.getEquipmentItem().getItemId(), List.of())) {
                    if (stat.getScope() != BuffTarget.SELF) continue;
                    if (!isElementApplicable(stat.getElement(), nature)) continue;
                    accumulate(stat.getStatUnit() == StatUnit.PERCENT ? selfPercent : selfFlat,
                            stat.getStatType(), stat.getValue());
                }
            }

            // SELF 세트 효과 누적
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

            // 최종 유효 스탯 = (기본 + selfFlat + partyFlat) × (1 + percent/100)
            Map<StatType, Integer> effectiveStats = new EnumMap<>(StatType.class);
            for (StatType type : StatType.values()) {
                int baseVal = base.getOrDefault(type, 0);
                int flat = selfFlat.getOrDefault(type, 0) + partyFlatBonus.getOrDefault(type, 0);
                int pct = selfPercent.getOrDefault(type, 0) + partyPercentBonus.getOrDefault(type, 0);
                int effective = (int) Math.round((baseVal + flat) * (1.0 + pct / 100.0));
                if (effective != 0) effectiveStats.put(type, effective);
            }

            effectiveStatsByMemberId.put(member.getId(), effectiveStats);
        }

        // ── 총 저항깎 · 저항 통과율 ──────────────────────────────────────────────
        Monster monster = monsterRepository.findById(req.monsterId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "몬스터를 찾을 수 없습니다."));

        int totalResistPierce = enemyResistPierceBonus
                + effectiveStatsByMemberId.values().stream()
                        .mapToInt(m -> m.getOrDefault(StatType.RESIST_PIERCE, 0))
                        .sum();

        ResistanceType resistType = req.resistanceType() != null ? req.resistanceType() : ResistanceType.HITTING;
        int monsterResist = resistType == ResistanceType.MAGIC
                ? (monster.getMagicResistance() != null ? monster.getMagicResistance() : 0)
                : (monster.getHittingResistance() != null ? monster.getHittingResistance() : 0);

        int resistAfterDebuff = monsterResist - totalResistPierce;
        double resistPassRate = calcResistPassRate(resistAfterDebuff);
        int monsterElementValue = monster.getElementValue() != null ? monster.getElementValue() : 0;

        // ── 멤버별 DPS 계산 ────────────────────────────────────────────────────

        List<MemberDpsResult> memberResults = new ArrayList<>();
        double totalDps = 0.0;

        for (UserDeckMember member : members) {
            MemberDpsInput input = inputMap.getOrDefault(member.getId(),
                    new MemberDpsInput(member.getId(), 250, BonusStatTarget.MAIN_STAT, 0));

            int level = (input.level() == 260) ? 260 : 250;
            int levelStat = (level == 260) ? LEVEL_STAT_260 : LEVEL_STAT_250;

            Map<StatType, Integer> stats = effectiveStatsByMemberId.getOrDefault(member.getId(), Map.of());

            int baseStr = stats.getOrDefault(StatType.STRENGTH, 0);
            int baseDex = stats.getOrDefault(StatType.DEXTERITY, 0);
            int baseVit = stats.getOrDefault(StatType.VITALITY, 0);
            int baseInt = stats.getOrDefault(StatType.INTELLECT, 0);
            int baseAtk = stats.getOrDefault(StatType.ATTACK_POWER, 0);

            // 스킬 계수 결정 — 장착 아이템 스킬 우선
            List<UserDeckMemberSlot> memberSlots = slotsByMemberId.getOrDefault(member.getId(), List.of());
            List<SkillCoefficient> skillCoefs = resolveSkillCoefs(member, memberSlots, coefsByMercId, coefsByItemId);

            List<SkillDpsResult> skillResults = new ArrayList<>();
            double memberRawDps = 0.0;

            for (SkillCoefficient coef : skillCoefs) {
                StatType mainStat = resolveMainStat(coef);

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

                if (coef.getSkillType() == SkillType.INSTANT && coef.getCastsPerSecond() != null) {
                    skillDps = rawDmg * coef.getHitCount() * coef.getCastsPerSecond();
                    calculated = true;
                } else if (coef.getSkillType() == SkillType.PERSISTENT && coef.getTickIntervalMs() != null) {
                    skillDps = rawDmg / (coef.getTickIntervalMs() / 1000.0);
                    calculated = true;
                }

                String skillName = coef.isMercenarySkill()
                        ? coef.getMercenarySkill().getSkillName()
                        : coef.getItemSkill().getSkillName();

                skillResults.add(new SkillDpsResult(skillName, skillDps, calculated));
                memberRawDps += skillDps;
            }

            int memberElementValue = stats.getOrDefault(StatType.ELEMENT_VALUE, 0);
            double elementBonus = calcElementBonus(memberElementValue, monsterElementValue);
            double memberAdjustedDps = memberRawDps * (100.0 + elementBonus) / 100.0 * (resistPassRate / 100.0);

            totalDps += memberAdjustedDps;
            memberResults.add(new MemberDpsResult(
                    member.getId(),
                    member.getMercenary().getId(),
                    member.getMercenary().getName(),
                    elementBonus,
                    memberRawDps,
                    memberAdjustedDps,
                    skillResults
            ));
        }

        return new DpsResponse(monster.getId(), monster.getName(),
                totalResistPierce, resistAfterDebuff, resistPassRate, totalDps, memberResults);
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

    /**
     * 스킬 계수 결정 — 장착 슬롯 중 아이템 스킬 계수가 존재하면 우선 적용.
     * 없으면 용병 스킬 계수 사용.
     */
    private List<SkillCoefficient> resolveSkillCoefs(
            UserDeckMember member,
            List<UserDeckMemberSlot> slots,
            Map<Long, List<SkillCoefficient>> coefsByMercId,
            Map<Long, List<SkillCoefficient>> coefsByItemId) {

        for (UserDeckMemberSlot slot : slots) {
            List<SkillCoefficient> itemCoefs = coefsByItemId.get(slot.getEquipmentItem().getItemId());
            if (itemCoefs != null && !itemCoefs.isEmpty()) return itemCoefs;
        }
        return coefsByMercId.getOrDefault(member.getMercenary().getId(), List.of());
    }

    /**
     * 주스탯 판별 — STR/DEX/VIT/INT 중 계수가 가장 높은 스탯을 반환.
     * 동점이면 STRENGTH 우선.
     */
    private StatType resolveMainStat(SkillCoefficient coef) {
        float max = Math.max(Math.max(coef.getCoefStr(), coef.getCoefDex()),
                Math.max(coef.getCoefVit(), coef.getCoefInt()));
        if (max == 0f) return StatType.STRENGTH;
        if (coef.getCoefStr() >= max) return StatType.STRENGTH;
        if (coef.getCoefDex() >= max) return StatType.DEXTERITY;
        if (coef.getCoefVit() >= max) return StatType.VITALITY;
        return StatType.INTELLECT;
    }

    private double calcResistPassRate(int resistAfterDebuff) {
        if (resistAfterDebuff >= RESIST_CAP) return RESIST_CAP_PASS_RATE;
        return Math.max(0.0, 100.0 - (resistAfterDebuff * 0.16 + 57.0));
    }

    private double calcElementBonus(int myElementValue, int monsterElementValue) {
        double raw = (3.0 * myElementValue - monsterElementValue) / 2.0;
        return Math.max(ELEMENT_BONUS_MIN, Math.min(ELEMENT_BONUS_MAX, raw));
    }
}
