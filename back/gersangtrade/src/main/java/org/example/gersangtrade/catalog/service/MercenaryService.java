package org.example.gersangtrade.catalog.service;

import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.catalog.dto.response.MercenaryResponse;
import org.example.gersangtrade.catalog.repository.MercenaryRepository;
import org.example.gersangtrade.catalog.repository.MercenaryStatRepository;
import org.example.gersangtrade.domain.catalog.Mercenary;
import org.example.gersangtrade.domain.catalog.MercenaryStat;
import org.example.gersangtrade.domain.catalog.enums.StatType;
import org.example.gersangtrade.config.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private final MercenaryRepository mercenaryRepository;
    private final MercenaryStatRepository mercenaryStatRepository;

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

        // 저항깎 스탯 배치 조회 (N+1 방지)
        List<Long> ids = filtered.stream().map(Mercenary::getId).toList();
        Map<Long, Integer> resistPierceMap = mercenaryStatRepository.findByMercenaryIdIn(ids)
                .stream()
                .filter(s -> s.getStatKey() == StatType.RESIST_PIERCE)
                .collect(Collectors.toMap(
                        s -> s.getMercenary().getId(),
                        MercenaryStat::getStatValue,
                        (a, b) -> a
                ));

        return filtered.stream()
                .map(m -> MercenaryResponse.of(m, resistPierceMap.get(m.getId())))
                .toList();
    }
}
