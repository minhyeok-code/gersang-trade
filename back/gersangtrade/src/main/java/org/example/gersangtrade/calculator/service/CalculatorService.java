package org.example.gersangtrade.calculator.service;

import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.calculator.dto.request.CalculatorRequest;
import org.example.gersangtrade.calculator.dto.response.CalculatorResponse;
import org.example.gersangtrade.calculator.dto.response.CurrentStatsDto;
import org.example.gersangtrade.calculator.dto.response.ElementValueEntryDto;
import org.example.gersangtrade.calculator.dto.response.ResistPierceEntryDto;
import org.example.gersangtrade.catalog.repository.ItemStatRepository;
import org.example.gersangtrade.catalog.repository.MercenaryMaterialRepository;
import org.example.gersangtrade.catalog.repository.MercenaryRepository;
import org.example.gersangtrade.crawler.repository.MaterialPriceHistoryRepository;
import org.example.gersangtrade.domain.catalog.ItemStat;
import org.example.gersangtrade.domain.catalog.Mercenary;
import org.example.gersangtrade.domain.catalog.MercenaryMaterial;
import org.example.gersangtrade.domain.catalog.enums.StatType;
import org.example.gersangtrade.domain.crawler.MaterialPriceHistory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 가성비 계산기 서비스.
 *
 * <p>데미지 공식:
 * <ul>
 *   <li>저항 통과율(%) = 깎은 뒤 저항 >= 260 → 1.4% 고정; 미만 → 100 - (저항 × 0.16 + 57), 최솟값 0</li>
 *   <li>속성 보정(%) = clamp((3 × 용병 속성값 - 몬스터 속성값) / 2, -50, +50)</li>
 *   <li>종합 데미지 배율 = (100 + 속성 보정) × (통과율 / 100)</li>
 *   <li>가성비 점수 = 데미지 상승률(%) / 가격(만 골드)</li>
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

    /** 속성 보정 최솟값(%) */
    private static final double ELEMENT_BONUS_MIN = -50.0;

    /** 속성 보정 최댓값(%) */
    private static final double ELEMENT_BONUS_MAX = 50.0;

    /** 골드 → 만 골드 환산 단위 */
    private static final double MAN_GOLD_UNIT = 10_000.0;

    /** 직전 달 연월 포맷 */
    private static final DateTimeFormatter YEAR_MONTH_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM");

    // ── 의존성 ────────────────────────────────────────────────────────────────

    private final ItemStatRepository itemStatRepository;
    private final MercenaryRepository mercenaryRepository;
    private final MercenaryMaterialRepository mercenaryMaterialRepository;
    private final MaterialPriceHistoryRepository materialPriceHistoryRepository;

    // ── 메인 계산 ─────────────────────────────────────────────────────────────

    /**
     * 유저 입력을 바탕으로 현재 데미지 현황과 저항깎·속성값 가성비 리스트를 계산한다.
     *
     * <p>가격 조회는 직전 달 MaterialPriceHistory를 기준으로 하며,
     * priceOverrides가 있으면 해당 항목 가격을 덮어쓴다.
     *
     * @param request 유저 스펙 및 몬스터 정보 입력값
     * @return 현재 데미지 현황 + 저항깎/속성값 리스트
     */
    public CalculatorResponse calculate(CalculatorRequest request) {
        // 직전 달 연월 계산 (가격 기본값 조회 기준)
        String prevYearMonth = LocalDate.now().minusMonths(1).format(YEAR_MONTH_FORMATTER);

        // 서버별 전체 아이템 가격 맵 로드 (itemId → avgPrice)
        Map<Long, Long> priceMap = loadPriceMap(request.serverId(), prevYearMonth);

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
     * 속성 보정 계산.
     * = clamp((3 × 용병 속성값 - 몬스터 속성값) / 2, -50, +50)
     */
    private double calcElementBonus(int myElementValue, int monsterElementValue) {
        double raw = (3.0 * myElementValue - monsterElementValue) / 2.0;
        return Math.max(ELEMENT_BONUS_MIN, Math.min(ELEMENT_BONUS_MAX, raw));
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
     * = 데미지 상승률(%) / 가격(만 골드).
     * 가격이 null이거나 0이면 null 반환 (계산 제외 처리).
     */
    private Double calcEfficiencyScore(Long price, double increaseRate) {
        if (price == null || price == 0L) return null;
        return increaseRate / (price / MAN_GOLD_UNIT);
    }

    // ── 현재 데미지 현황 ──────────────────────────────────────────────────────

    /** 유저 현재 스펙 기준 데미지 현황 계산 */
    private CurrentStatsDto calcCurrentStats(CalculatorRequest req) {
        int resistAfterDebuff = req.monsterResistance() - req.currentResistPierce();
        double resistPassRate = calcResistPassRate(resistAfterDebuff);
        double elementBonus = calcElementBonus(req.currentElementValue(), req.monsterElementValue());
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
     * 서버·연월 기준 아이템 가격 맵 로드.
     * key: itemId, value: avgPrice (골드 단위)
     */
    private Map<Long, Long> loadPriceMap(Integer serverId, String yearMonth) {
        List<MaterialPriceHistory> histories =
                materialPriceHistoryRepository.findAllByServerIdAndYearMonth(serverId, yearMonth);
        return histories.stream()
                .collect(Collectors.toMap(
                        mph -> mph.getItem().getId(),
                        MaterialPriceHistory::getAvgPrice,
                        (a, b) -> a));  // 중복 시 첫 번째 값 유지
    }

    /**
     * 용병 ID 목록에 해당하는 고용 총비용 계산.
     * 재료 가격 정보가 없는 항목은 제외하고 합산하므로 실제 비용보다 낮을 수 있다.
     * key: mercenaryId, value: 알려진 재료 비용 합산 (골드 단위)
     */
    private Map<Long, Long> calcMercenaryCosts(List<Long> mercenaryIds, Map<Long, Long> priceMap) {
        if (mercenaryIds.isEmpty()) return Map.of();

        List<MercenaryMaterial> materials =
                mercenaryMaterialRepository.findAllWithItemAndMercenaryByMercenaryIds(mercenaryIds);

        Map<Long, Long> costMap = new HashMap<>();
        for (MercenaryMaterial mm : materials) {
            Long itemPrice = priceMap.get(mm.getItem().getId());
            if (itemPrice != null) {
                long materialCost = itemPrice * mm.getQuantity();
                costMap.merge(mm.getMercenary().getId(), materialCost, Long::sum);
            }
        }
        return costMap;
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
     * ItemStat(RESIST_PIERCE) 아이템과 resistPierce가 있는 용병을 포함한다.
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

        // 저항깎 수치가 있는 용병
        List<Mercenary> mercs = mercenaryRepository.findByResistPierceIsNotNull();
        Map<Long, Long> mercCosts = calcMercenaryCosts(
                mercs.stream().map(Mercenary::getId).toList(), priceMap);

        for (Mercenary merc : mercs) {
            Long price = resolvePrice(overrides, "MERC_" + merc.getId(), mercCosts.get(merc.getId()));
            list.add(buildResistEntry(
                    "MERCENARY", merc.getId(), merc.getName(), merc.getResistPierce(),
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
        double elementBonus = calcElementBonus(req.currentElementValue(), req.monsterElementValue());
        double newMultiplier = calcDamageMultiplier(newPassRate, elementBonus);
        double increaseRate = calcIncreaseRate(current.damageMultiplier(), newMultiplier);
        Double effScore = calcEfficiencyScore(price, increaseRate);

        return new ResistPierceEntryDto(
                sourceType, sourceId, name, resistValue,
                price, newPassRate, increaseRate, effScore, false);
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
                    top.efficiencyScore(), true));
        }
        return list;
    }

    // ── 속성값 리스트 ─────────────────────────────────────────────────────────

    /**
     * 속성값 아이템/용병 가성비 리스트 생성.
     * ItemStat(ELEMENT_VALUE) 아이템과 elementValue가 있는 용병을 포함한다.
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

        // 속성값이 있는 용병
        List<Mercenary> mercs = mercenaryRepository.findByElementValueIsNotNull();
        Map<Long, Long> mercCosts = calcMercenaryCosts(
                mercs.stream().map(Mercenary::getId).toList(), priceMap);

        for (Mercenary merc : mercs) {
            Long price = resolvePrice(overrides, "MERC_" + merc.getId(), mercCosts.get(merc.getId()));
            list.add(buildElementEntry(
                    "MERCENARY", merc.getId(), merc.getName(), merc.getElementValue(),
                    price, req, current));
        }

        return sortAndMarkTopElement(list);
    }

    /** 속성값 항목 하나 계산 */
    private ElementValueEntryDto buildElementEntry(
            String sourceType, Long sourceId, String name, int elementIncrease,
            Long price, CalculatorRequest req, CurrentStatsDto current) {

        int newElementValue = req.currentElementValue() + elementIncrease;
        double newElementBonus = calcElementBonus(newElementValue, req.monsterElementValue());

        // 저항깎은 현재 스펙 그대로 유지, 속성값만 변경
        int resistAfterDebuff = req.monsterResistance() - req.currentResistPierce();
        double passRate = calcResistPassRate(resistAfterDebuff);
        double newMultiplier = calcDamageMultiplier(passRate, newElementBonus);
        double increaseRate = calcIncreaseRate(current.damageMultiplier(), newMultiplier);
        Double effScore = calcEfficiencyScore(price, increaseRate);

        return new ElementValueEntryDto(
                sourceType, sourceId, name, elementIncrease,
                price, newElementBonus, increaseRate, effScore, false);
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
                    top.efficiencyScore(), true));
        }
        return list;
    }
}
