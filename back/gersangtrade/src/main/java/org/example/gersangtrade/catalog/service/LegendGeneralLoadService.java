package org.example.gersangtrade.catalog.service;

import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.catalog.repository.LegendGeneralCharacteristicRepository;
import org.example.gersangtrade.catalog.repository.LegendGeneralRepository;
import org.example.gersangtrade.domain.catalog.LegendGeneral;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 전설장수 상세 로드 — Hibernate MultipleBagFetchException 방지를 위해
 * characteristics / effects / passives를 분리 쿼리로 초기화한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LegendGeneralLoadService {

    private final LegendGeneralRepository legendGeneralRepository;
    private final LegendGeneralCharacteristicRepository characteristicRepository;

    public Optional<LegendGeneral> loadForCalculation(Long mercenaryId) {
        Optional<LegendGeneral> optional = legendGeneralRepository.findByMercenaryIdWithDetails(mercenaryId);
        optional.ifPresent(lg -> {
            characteristicRepository.findWithEffectsByLegendGeneralId(lg.getId());
            lg.getPassives().size();
        });
        return optional;
    }
}
