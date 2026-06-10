package org.example.gersangtrade.catalog.service;

import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.catalog.dto.response.MercenaryCharacteristicCatalogResponse;
import org.example.gersangtrade.catalog.dto.response.MercenaryCharacteristicSetupResponse;
import org.example.gersangtrade.catalog.dto.response.MercenaryResponse;
import org.example.gersangtrade.catalog.repository.MercenaryCharacteristicLevelRepository;
import org.example.gersangtrade.catalog.repository.MercenaryCharacteristicRepository;
import org.example.gersangtrade.catalog.repository.MercenaryRepository;
import org.example.gersangtrade.catalog.repository.MercenaryStatRepository;
import org.example.gersangtrade.config.CacheConfig;
import org.example.gersangtrade.domain.catalog.LegendGeneral;
import org.example.gersangtrade.domain.catalog.LegendGeneralCharacteristic;
import org.example.gersangtrade.domain.catalog.Mercenary;
import org.example.gersangtrade.domain.catalog.MercenaryCharacteristic;
import org.example.gersangtrade.domain.catalog.MercenaryCharacteristicLevel;
import org.example.gersangtrade.domain.catalog.MercenaryStat;
import org.example.gersangtrade.domain.catalog.enums.MercenaryCategory;
import org.example.gersangtrade.domain.catalog.enums.StatType;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 공개 용병 목록 조회 서비스 — DPS 계산기 용병 선택용.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MercenaryService {

    private static final int PROTAGONIST_CHARACTERISTIC_POINTS = 16;
    private static final int DEFAULT_CHARACTERISTIC_POINTS = 17;

    private final MercenaryRepository mercenaryRepository;
    private final MercenaryStatRepository mercenaryStatRepository;
    private final MercenaryCharacteristicRepository characteristicRepository;
    private final MercenaryCharacteristicLevelRepository characteristicLevelRepository;
    private final LegendGeneralLoadService legendGeneralLoadService;

    /**
     * 용병 목록 조회.
     * comingSoon=true인 용병은 제외한다.
     *
     * @param element 속성 필터 (Nature enum name, null이면 전체)
     * @param q       이름 검색어 (null이면 전체)
     * @param limit   최대 반환 수 (null이면 전체)
     */
    @Cacheable(value = CacheConfig.MERCENARIES, key = "#element + ':' + #q + ':' + #limit")
    public List<MercenaryResponse> listMercenaries(String element, String q, Integer limit) {
        List<Mercenary> all = mercenaryRepository.findAll();

        // comingSoon 제외 + 필터 적용
        List<Mercenary> filtered = all.stream()
                .filter(m -> !m.isComingSoon())
                .filter(m -> element == null || element.isBlank()
                        || (m.getNature() != null && m.getNature().name().equalsIgnoreCase(element)))
                .filter(m -> q == null || q.isBlank()
                        || m.getName().contains(q))
                .collect(Collectors.toList());

        if (limit != null && limit > 0 && filtered.size() > limit) {
            filtered = filtered.subList(0, limit);
        }

        // 저항깎·속성값 스탯 배치 조회 (N+1 방지)
        List<Long> ids = filtered.stream().map(Mercenary::getId).toList();
        List<MercenaryStat> allStats = mercenaryStatRepository.findByMercenaryIdIn(ids);
        Map<Long, Integer> resistPierceMap = allStats.stream()
                .filter(s -> s.getStatKey() == StatType.RESIST_PIERCE)
                .collect(Collectors.toMap(
                        s -> s.getMercenary().getId(),
                        MercenaryStat::getStatValue,
                        (a, b) -> a
                ));
        Map<Long, Integer> elementValueMap = allStats.stream()
                .filter(s -> s.getStatKey() == StatType.ELEMENT_VALUE)
                .collect(Collectors.toMap(
                        s -> s.getMercenary().getId(),
                        MercenaryStat::getStatValue,
                        (a, b) -> a
                ));

        return filtered.stream()
                .map(m -> MercenaryResponse.of(m, resistPierceMap.get(m.getId()),
                        elementValueMap.get(m.getId())))
                .toList();
    }

    /**
     * 용병 특성 카탈로그 — 스냅샷 뷰어에서 characteristicId → 이름·레벨 라벨 조회용.
     */
    @Cacheable(value = CacheConfig.MERCENARY_CHARACTERISTICS, key = "#mercenaryId")
    public MercenaryCharacteristicCatalogResponse getCharacteristicCatalog(Long mercenaryId) {
        Mercenary mercenary = mercenaryRepository.findById(mercenaryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "용병을 찾을 수 없습니다."));

        List<MercenaryCharacteristic> allCharacteristics = characteristicRepository.findByMercenaryId(mercenaryId);
        Map<Long, List<MercenaryCharacteristicCatalogResponse.LevelEntry>> levelsByCharId;
        if (mercenary.getCategory() == MercenaryCategory.LEGENDARY_GENERAL) {
            levelsByCharId = buildLgLevelsByCharId(allCharacteristics, mercenaryId);
        } else {
            List<Long> charIds = allCharacteristics.stream().map(MercenaryCharacteristic::getId).toList();
            levelsByCharId = charIds.isEmpty() ? Map.of()
                    : characteristicLevelRepository.findByCharacteristicIdIn(charIds).stream()
                            .collect(Collectors.groupingBy(
                                    l -> l.getCharacteristic().getId(),
                                    Collectors.mapping(
                                            l -> new MercenaryCharacteristicCatalogResponse.LevelEntry(
                                                    l.getLevel(), l.getLabel()),
                                            Collectors.toList())));
        }

        List<MercenaryCharacteristicCatalogResponse.CharacteristicEntry> entries = allCharacteristics.stream()
                .map(c -> {
                    List<MercenaryCharacteristicCatalogResponse.LevelEntry> levels =
                            levelsByCharId.getOrDefault(c.getId(), List.of()).stream()
                                    .sorted(Comparator.comparing(MercenaryCharacteristicCatalogResponse.LevelEntry::level))
                                    .toList();
                    return new MercenaryCharacteristicCatalogResponse.CharacteristicEntry(
                            c.getId(), c.getName(), levels);
                })
                .toList();

        return new MercenaryCharacteristicCatalogResponse(mercenaryId, entries);
    }

    /**
     * 용병 특성 배분 UI용 전체 정의 조회 — 가성비 시뮬레이션 후보 용병 설정에 사용한다.
     */
    @Cacheable(value = CacheConfig.MERCENARY_CHARACTERISTICS, key = "'setup:' + #mercenaryId")
    public MercenaryCharacteristicSetupResponse getCharacteristicSetup(Long mercenaryId) {
        Mercenary mercenary = mercenaryRepository.findById(mercenaryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "용병을 찾을 수 없습니다."));

        List<MercenaryCharacteristic> allCharacteristics = characteristicRepository.findByMercenaryId(mercenaryId);

        Map<Long, List<MercenaryCharacteristicSetupResponse.LevelEntry>> levelsByCharId;
        if (mercenary.getCategory() == MercenaryCategory.LEGENDARY_GENERAL) {
            levelsByCharId = buildSetupLgLevelsByCharId(allCharacteristics, mercenaryId);
        } else {
            List<Long> charIds = allCharacteristics.stream().map(MercenaryCharacteristic::getId).toList();
            levelsByCharId = charIds.isEmpty() ? Map.of()
                    : characteristicLevelRepository.findByCharacteristicIdIn(charIds).stream()
                            .collect(Collectors.groupingBy(
                                    l -> l.getCharacteristic().getId(),
                                    Collectors.mapping(l -> new MercenaryCharacteristicSetupResponse.LevelEntry(
                                            l.getLabel(), l.getLevel(), l.getAmount(), l.getAmountValue(),
                                            l.getStatType() != null ? l.getStatType().name() : null,
                                            null),
                                            Collectors.toList())));
        }

        List<MercenaryCharacteristicSetupResponse.CharacteristicEntry> entries = allCharacteristics.stream()
                .map(c -> {
                    List<MercenaryCharacteristicSetupResponse.LevelEntry> levels =
                            levelsByCharId.getOrDefault(c.getId(), List.of()).stream()
                                    .sorted(Comparator.comparing(MercenaryCharacteristicSetupResponse.LevelEntry::level))
                                    .toList();
                    return new MercenaryCharacteristicSetupResponse.CharacteristicEntry(
                            c.getId(), c.getKey(), c.getName(), c.getPoint(),
                            c.getDescription(), c.getRequiredCharacteristicKey(),
                            c.getApplyType().name(), levels);
                })
                .toList();

        int maxPoints = mercenary.getCategory() == MercenaryCategory.PROTAGONIST
                ? PROTAGONIST_CHARACTERISTIC_POINTS
                : DEFAULT_CHARACTERISTIC_POINTS;

        return new MercenaryCharacteristicSetupResponse(mercenaryId, maxPoints, entries);
    }

    /** 전설장수 MercenaryCharacteristic 스텁 ID → LevelEntry 목록 매핑 */
    private Map<Long, List<MercenaryCharacteristicCatalogResponse.LevelEntry>> buildLgLevelsByCharId(
            List<MercenaryCharacteristic> stubs, Long mercenaryId) {
        LegendGeneral lg = legendGeneralLoadService.loadForCalculation(mercenaryId).orElse(null);
        if (lg == null) return Map.of();

        Map<Integer, List<LegendGeneralCharacteristic>> byIndex = lg.getCharacteristics().stream()
                .collect(Collectors.groupingBy(LegendGeneralCharacteristic::getCharacteristicIndex));

        List<MercenaryCharacteristic> sortedStubs = stubs.stream()
                .sorted(Comparator.comparing(MercenaryCharacteristic::getId))
                .toList();

        Map<Long, List<MercenaryCharacteristicCatalogResponse.LevelEntry>> result = new HashMap<>();
        for (int i = 0; i < sortedStubs.size(); i++) {
            Long stubId = sortedStubs.get(i).getId();
            List<LegendGeneralCharacteristic> lgcRows = byIndex.getOrDefault(i, List.of());
            List<MercenaryCharacteristicCatalogResponse.LevelEntry> levels = lgcRows.stream()
                    .sorted(Comparator.comparing(LegendGeneralCharacteristic::getLevel))
                    .map(row -> new MercenaryCharacteristicCatalogResponse.LevelEntry(
                            row.getLevel(), row.getName()))
                    .toList();
            result.put(stubId, levels);
        }
        return result;
    }

    private Map<Long, List<MercenaryCharacteristicSetupResponse.LevelEntry>> buildSetupLgLevelsByCharId(
            List<MercenaryCharacteristic> stubs, Long mercenaryId) {
        LegendGeneral lg = legendGeneralLoadService.loadForCalculation(mercenaryId).orElse(null);
        if (lg == null) return Map.of();

        Map<Integer, List<LegendGeneralCharacteristic>> byIndex = lg.getCharacteristics().stream()
                .collect(Collectors.groupingBy(LegendGeneralCharacteristic::getCharacteristicIndex));

        List<MercenaryCharacteristic> sortedStubs = stubs.stream()
                .sorted(Comparator.comparing(MercenaryCharacteristic::getId))
                .toList();

        Map<Long, List<MercenaryCharacteristicSetupResponse.LevelEntry>> result = new HashMap<>();
        for (int i = 0; i < sortedStubs.size(); i++) {
            Long stubId = sortedStubs.get(i).getId();
            List<LegendGeneralCharacteristic> lgcRows = byIndex.getOrDefault(i, List.of());
            List<MercenaryCharacteristicSetupResponse.LevelEntry> levels = lgcRows.stream()
                    .sorted(Comparator.comparing(LegendGeneralCharacteristic::getLevel))
                    .flatMap(row -> row.getEffects().stream()
                            .map(eff -> new MercenaryCharacteristicSetupResponse.LevelEntry(
                                    null, row.getLevel(), null, eff.getValue(),
                                    eff.getStatType() != null ? eff.getStatType().name() : null,
                                    eff.getElement() != null ? eff.getElement().name() : null)))
                    .toList();
            result.put(stubId, levels);
        }
        return result;
    }
}
