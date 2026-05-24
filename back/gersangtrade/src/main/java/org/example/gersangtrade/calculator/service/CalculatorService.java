package org.example.gersangtrade.calculator.service;

import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.calculator.dto.request.CalculatorRequest;
import org.example.gersangtrade.calculator.dto.response.CalculatorResponse;
import org.example.gersangtrade.calculator.dto.response.CurrentStatsDto;
import org.example.gersangtrade.calculator.dto.response.ElementValueEntryDto;
import org.example.gersangtrade.calculator.dto.response.ResistPierceEntryDto;
import org.example.gersangtrade.common.GoldFormatter;
import org.example.gersangtrade.catalog.repository.ItemStatRepository;
import org.example.gersangtrade.catalog.repository.MercenaryRepository;
import org.example.gersangtrade.catalog.repository.MercenaryStatRepository;
import org.example.gersangtrade.domain.catalog.ItemStat;
import org.example.gersangtrade.domain.catalog.Mercenary;
import org.example.gersangtrade.domain.catalog.MercenaryStat;
import org.example.gersangtrade.domain.catalog.enums.Element;
import org.example.gersangtrade.domain.catalog.enums.StatType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 가성비 계산기 서비스.
 *
 * <p>데미지 공식:
 * <ul>
 *   <li>저항 통과율(%) = 깎은 뒤 저항 >= 260 → 1.4% 고정; 미만 → 100 - (저항 × 0.16 + 57), 최솟값 0</li>
 *   <li>속성 보정(%) = 무속성 몬스터(element null/NONE)면 0; 아니면 clamp((3 × 용병 속성값 - 몬스터 속성값) / 2, -50, +50)</li>
 *   <li>종합 데미지 배율 = (100 + 속성 보정) × (통과율 / 100)</li>
 *   <li>가성비 점수 = 데미지 상승률(%) / 가격(억 전)</li>
 * </ul>
 *
 * <p>가격 기본값: MaterialPriceHistory 직전 달 평균가. priceOverrides로 덮어쓸 수 있다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CalculatorService {

    // ── 상수 ─────────────────────────────────────────────────────────────────

    /** 저항 상한선. 이 값 이상이면 통과율이 1.4%로 고정된다 */
    private static final int RESIST_CAP = 260;

    /** 저항 상한선 통과율(%) */
    private static final double RESIST_CAP_PASS_RATE = 1.4;

    /** 전(錢) → 억 환산 단위. 가성비 점수 = 데미지 상승률(%) ÷ 가격(억 전) */
    private static final double EOK_UNIT = 100_000_000.0;

    // ── 의존성 ────────────────────────────────────────────────────────────────

    private final ItemStatRepository itemStatRepository;
    private final MercenaryRepository mercenaryRepository;
    private final MercenaryStatRepository mercenaryStatRepository;

    // ── 메인 계산 ─────────────────────────────────────────────────────────────

    /**
     * 유저 입력을 바탕으로 현재 데미지 현황과 저항깎·속성값 가성비 리스트를 계산한다.
     *
     * <p>가격은 priceOverrides로만 입력받는다. 미입력 항목은 price=null로 처리되어 가성비 점수가 표시되지 않는다.
     *
     * @param request 유저 스펙 및 몬스터 정보 입력값
     * @return 현재 데미지 현황 + 저항깎/속성값 리스트
     */
    public CalculatorResponse calculate(CalculatorRequest request) {
        // 가격은 유저 직접 입력(priceOverrides)만 사용 — DB 조회 없음
        Map<Long, Long> priceMap = Map.of();

        // priceOverrides가 null이면 빈 맵으로 처리
        Map<String, Long> overrides =
                request.priceOverrides() != null ? request.priceOverrides() : Map.of();

        // 현재 스펙 기준 데미지 현황 계산
        CurrentStatsDto currentStats = calcCurrentStats(request);

        // 저항깎 / 속성값 가성비 리스트 생성
        List<ResistPierceEntryDto> resistList =
                buildResistPierceList(request, currentStats, priceMap, overrides);
        List<ElementValueEntryDto> elementList =
                buildElementValueList(request, currentStats, priceMap, overrides);

        return new CalculatorResponse(currentStats, resistList, elementList);
    }

    // ── 데미지 공식 ──────────────────────────────────────────────────────────

    /**
     * 저항 통과율 계산.
     * 깎은 뒤 저항 >= 260이면 1.4% 고정; 미만이면 100 - (저항 × 0.16 + 57), 최솟값 0.
     */
    private double calcResistPassRate(int resistAfterDebuff) {
        if (resistAfterDebuff >= RESIST_CAP) {
            return RESIST_CAP_PASS_RATE;
        }
        return Math.max(0.0, 100.0 - (resistAfterDebuff * 0.16 + 57.0));
    }

    /**
     * 속성 보정 계산 — 무속성 몬스터는 0.
     * @see ElementBonusCalculator
     */
    private double calcElementBonus(int myElementValue, int monsterElementValue, Element monsterElement) {
        return ElementBonusCalculator.calcElementBonus(myElementValue, monsterElementValue, monsterElement);
    }

    /**
     * 종합 데미지 배율 계산.
     * = (100 + 속성 보정) × (통과율 / 100)
     */
    private double calcDamageMultiplier(double resistPassRate, double elementBonus) {
        return (100.0 + elementBonus) * (resistPassRate / 100.0);
    }

    /**
     * 데미지 상승률(%) 계산.
     * = (after / before - 1) × 100. before가 0 이하이면 0 반환.
     */
    private double calcIncreaseRate(double before, double after) {
        if (before <= 0.0) return 0.0;
        return (after / before - 1.0) * 100.0;
    }

    /**
     * 가성비 점수 계산.
     * = 데미지 상승률(%) / 가격(억 전).
     * 가격이 null이거나 0이면 null 반환 (계산 제외 처리).
     */
    private Double calcEfficiencyScore(Long price, double increaseRate) {
        if (price == null || price == 0L) return null;
        return increaseRate / (price / EOK_UNIT);
    }

    // ── 현재 데미지 현황 ──────────────────────────────────────────────────────

    /** 유저 현재 스펙 기준 데미지 현황 계산 */
    private CurrentStatsDto calcCurrentStats(CalculatorRequest req) {
        int resistAfterDebuff = req.monsterResistance() - req.currentResistPierce();
        double resistPassRate = calcResistPassRate(resistAfterDebuff);
        double elementBonus = calcElementBonus(req.currentElementValue(), req.monsterElementValue(), req.monsterElement());
        double damageMultiplier = calcDamageMultiplier(resistPassRate, elementBonus);

        // 저깎 0% 기준 배율 (저항깎이 전혀 없을 때 대비 현재 배율)
        double basePassRate = calcResistPassRate(req.monsterResistance());
        double baseDamageMultiplier = calcDamageMultiplier(basePassRate, elementBonus);
        double baseMultiplier = baseDamageMultiplier > 0.0
                ? damageMultiplier / baseDamageMultiplier : 0.0;

        return new CurrentStatsDto(
                resistAfterDebuff, resistPassRate, elementBonus, damageMultiplier, baseMultiplier);
    }

    // ── 가격 로딩 ─────────────────────────────────────────────────────────────

    /**
     * 용병 비용 자동 계산은 미지원.
     * 유저가 priceOverrides에서 "MERC_{mercenaryId}" 키로 직접 입력해야 한다.
     */
    private Map<Long, Long> calcMercenaryCosts(List<Long> mercenaryIds,
                                               Map<String, Long> overrides) {
        return Map.of();
    }

    /**
     * priceOverrides 우선 적용, 없으면 defaultPrice 사용.
     *
     * @param overrides 유저가 수정한 가격 맵. 키: "ITEM_{id}" 또는 "MERC_{id}"
     * @param key 조회할 키
     * @param defaultPrice 기본 가격 (집계 테이블 기준)
     */
    private Long resolvePrice(Map<String, Long> overrides, String key, Long defaultPrice) {
        return overrides.getOrDefault(key, defaultPrice);
    }

    // ── 저항깎 리스트 ─────────────────────────────────────────────────────────

    /**
     * 저항깎 아이템/용병 가성비 리스트 생성.
     * ItemStat(RESIST_PIERCE) 아이템과 MercenaryStat(RESIST_PIERCE) 용병을 포함한다.
     */
    private List<ResistPierceEntryDto> buildResistPierceList(
            CalculatorRequest req, CurrentStatsDto current,
            Map<Long, Long> priceMap, Map<String, Long> overrides) {

        List<ResistPierceEntryDto> list = new ArrayList<>();

        // 저항깎 스탯이 있는 아이템
        for (ItemStat stat : itemStatRepository.findAllByStatTypeWithItem(StatType.RESIST_PIERCE)) {
            Long itemId = stat.getItem().getId();
            Long price = resolvePrice(overrides, "ITEM_" + itemId, priceMap.get(itemId));
            list.add(buildResistEntry(
                    "ITEM", itemId, stat.getItem().getName(), stat.getValue(), price, req, current));
        }

        // 저항깎 스탯(RESIST_PIERCE)이 있는 용병 — MercenaryStat 기반 조회
        List<MercenaryStat> resistStats =
                mercenaryStatRepository.findByStatKey(StatType.RESIST_PIERCE);
        Map<Long, Long> mercCosts = calcMercenaryCosts(
                resistStats.stream().map(s -> s.getMercenary().getId()).distinct().toList(),
                overrides);

        for (MercenaryStat stat : resistStats) {
            Mercenary merc = stat.getMercenary();
            Long price = resolvePrice(overrides, "MERC_" + merc.getId(), mercCosts.get(merc.getId()));
            list.add(buildResistEntry(
                    "MERCENARY", merc.getId(), merc.getName(), stat.getStatValue(),
                    price, req, current));
        }

        return sortAndMarkTopResist(list);
    }

    /** 저항깎 항목 하나 계산 */
    private ResistPierceEntryDto buildResistEntry(
            String sourceType, Long sourceId, String name, int resistValue,
            Long price, CalculatorRequest req, CurrentStatsDto current) {

        int newResistAfterDebuff = req.monsterResistance() - (req.currentResistPierce() + resistValue);
        double newPassRate = calcResistPassRate(newResistAfterDebuff);
        double elementBonus = calcElementBonus(req.currentElementValue(), req.monsterElementValue(), req.monsterElement());
        double newMultiplier = calcDamageMultiplier(newPassRate, elementBonus);
        double increaseRate = calcIncreaseRate(current.damageMultiplier(), newMultiplier);
        Double effScore = calcEfficiencyScore(price, increaseRate);

        return new ResistPierceEntryDto(
                sourceType, sourceId, name, resistValue,
                price, newPassRate, increaseRate, effScore, false,
                GoldFormatter.format(price));
    }

    /**
     * 가성비 점수 내림차순 정렬 후 최고 점수 항목에 recommended=true 마킹.
     * 점수 없는 항목(null)은 목록 맨 뒤로 밀린다.
     */
    private List<ResistPierceEntryDto> sortAndMarkTopResist(List<ResistPierceEntryDto> list) {
        list.sort(Comparator.comparingDouble(
                (ResistPierceEntryDto e) ->
                        e.efficiencyScore() != null ? e.efficiencyScore() : Double.NEGATIVE_INFINITY
        ).reversed());

        // 최고 점수 항목 recommended 마킹 (immutable record이므로 재생성)
        // 점수가 0 이하이면 데미지가 오히려 감소하는 항목이므로 추천하지 않는다
        if (!list.isEmpty()
                && list.get(0).efficiencyScore() != null
                && list.get(0).efficiencyScore() > 0) {
            ResistPierceEntryDto top = list.get(0);
            list.set(0, new ResistPierceEntryDto(
                    top.sourceType(), top.sourceId(), top.name(), top.resistPierceValue(),
                    top.price(), top.newResistPassRate(), top.damageIncreaseRate(),
                    top.efficiencyScore(), true, top.formattedPrice()));
        }
        return list;
    }

    // ── 속성값 리스트 ─────────────────────────────────────────────────────────

    /**
     * 속성값 아이템/용병 가성비 리스트 생성.
     * ItemStat(ELEMENT_VALUE) 아이템과 MercenaryStat(ELEMENT_VALUE) 용병을 포함한다.
     */
    private List<ElementValueEntryDto> buildElementValueList(
            CalculatorRequest req, CurrentStatsDto current,
            Map<Long, Long> priceMap, Map<String, Long> overrides) {

        List<ElementValueEntryDto> list = new ArrayList<>();

        // 속성값 스탯이 있는 아이템
        for (ItemStat stat : itemStatRepository.findAllByStatTypeWithItem(StatType.ELEMENT_VALUE)) {
            Long itemId = stat.getItem().getId();
            Long price = resolvePrice(overrides, "ITEM_" + itemId, priceMap.get(itemId));
            list.add(buildElementEntry(
                    "ITEM", itemId, stat.getItem().getName(), stat.getValue(), price, req, current));
        }

        // 속성값 스탯(ELEMENT_VALUE)이 있는 용병 — MercenaryStat 기반 조회
        List<MercenaryStat> elementStats =
                mercenaryStatRepository.findByStatKey(StatType.ELEMENT_VALUE);
        Map<Long, Long> mercCosts = calcMercenaryCosts(
                elementStats.stream().map(s -> s.getMercenary().getId()).distinct().toList(),
                overrides);

        for (MercenaryStat stat : elementStats) {
            Mercenary merc = stat.getMercenary();
            Long price = resolvePrice(overrides, "MERC_" + merc.getId(), mercCosts.get(merc.getId()));
            list.add(buildElementEntry(
                    "MERCENARY", merc.getId(), merc.getName(), stat.getStatValue(),
                    price, req, current));
        }

        return sortAndMarkTopElement(list);
    }

    /** 속성값 항목 하나 계산 */
    private ElementValueEntryDto buildElementEntry(
            String sourceType, Long sourceId, String name, int elementIncrease,
            Long price, CalculatorRequest req, CurrentStatsDto current) {

        int newElementValue = req.currentElementValue() + elementIncrease;
        double newElementBonus = calcElementBonus(newElementValue, req.monsterElementValue(), req.monsterElement());

        // 저항깎은 현재 스펙 그대로 유지, 속성값만 변경
        int resistAfterDebuff = req.monsterResistance() - req.currentResistPierce();
        double passRate = calcResistPassRate(resistAfterDebuff);
        double newMultiplier = calcDamageMultiplier(passRate, newElementBonus);
        double increaseRate = calcIncreaseRate(current.damageMultiplier(), newMultiplier);
        Double effScore = calcEfficiencyScore(price, increaseRate);

        return new ElementValueEntryDto(
                sourceType, sourceId, name, elementIncrease,
                price, newElementBonus, increaseRate, effScore, false,
                GoldFormatter.format(price));
    }

    /**
     * 가성비 점수 내림차순 정렬 후 최고 점수 항목에 recommended=true 마킹.
     */
    private List<ElementValueEntryDto> sortAndMarkTopElement(List<ElementValueEntryDto> list) {
        list.sort(Comparator.comparingDouble(
                (ElementValueEntryDto e) ->
                        e.efficiencyScore() != null ? e.efficiencyScore() : Double.NEGATIVE_INFINITY
        ).reversed());

        // 점수가 0 이하이면 데미지가 오히려 감소하는 항목이므로 추천하지 않는다
        if (!list.isEmpty()
                && list.get(0).efficiencyScore() != null
                && list.get(0).efficiencyScore() > 0) {
            ElementValueEntryDto top = list.get(0);
            list.set(0, new ElementValueEntryDto(
                    top.sourceType(), top.sourceId(), top.name(), top.elementValueIncrease(),
                    top.price(), top.newElementBonus(), top.damageIncreaseRate(),
                    top.efficiencyScore(), true, top.formattedPrice()));
        }
        return list;
    }
}
