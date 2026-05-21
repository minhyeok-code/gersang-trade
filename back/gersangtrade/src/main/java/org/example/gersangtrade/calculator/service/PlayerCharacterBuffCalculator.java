package org.example.gersangtrade.calculator.service;

import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.catalog.repository.MercenaryCharacteristicLevelRepository;
import org.example.gersangtrade.domain.catalog.MercenaryCharacteristic;
import org.example.gersangtrade.domain.catalog.MercenaryCharacteristicLevel;
import org.example.gersangtrade.domain.catalog.enums.Element;
import org.example.gersangtrade.domain.catalog.enums.Nation;
import org.example.gersangtrade.domain.catalog.enums.StatType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 주인공 2차전직 버프를 계산한다.
 *
 * <p>버프 종류:
 *   1. 국가 속성 버프 — 해당 속성 용병에게 ELEMENT_VALUE +n (자동 적용)
 *   2. 특성 버프 — statType이 매핑된 특성만 계산, 항상 적용 가정
 *
 * <p>jobType != SECOND이면 계산 스킵 (호출자 책임).
 * <p>상위 특성(point=2): 레벨 = floor(배분 포인트 / 2)
 * <p>하위 특성(point=1): 레벨 = 배분 포인트
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlayerCharacterBuffCalculator {

    private final MercenaryCharacteristicLevelRepository levelRepository;

    /**
     * 국가 속성 버프를 반환한다.
     * 해당 Element 속성을 가진 아군 용병의 ELEMENT_VALUE에 value를 가산한다.
     */
    public NationBuff getNationBuff(Nation nation) {
        return switch (nation) {
            case JOSEON -> new NationBuff(Element.FIRE,    5f);
            case JAPAN  -> new NationBuff(Element.WATER,   5f);
            case CHINA  -> new NationBuff(Element.WIND,    5f);
            case TAIWAN -> new NationBuff(Element.THUNDER, 5f);
            case INDIA  -> new NationBuff(Element.EARTH,   3f);
            default     -> new NationBuff(Element.NONE,    0f);
        };
    }

    /**
     * 특성 버프를 계산한다.
     *
     * @param characteristics  주인공의 특성 목록
     * @param pointsAllocated  특성 ID → 배분 포인트 Map
     * @return StatType → 합산 버프값 Map (statType=null인 특성은 스킵)
     */
    public Map<StatType, Float> calculateCharacteristicBuffs(
            List<MercenaryCharacteristic> characteristics,
            Map<Long, Integer> pointsAllocated) {

        Map<StatType, Float> result = new HashMap<>();

        for (MercenaryCharacteristic characteristic : characteristics) {
            int points = pointsAllocated.getOrDefault(characteristic.getId(), 0);
            if (points == 0) continue;

            Integer point = characteristic.getPoint();
            if (point == null) continue;

            int level = point == 2 ? points / 2 : points;
            if (level == 0) continue;

            List<MercenaryCharacteristicLevel> levels =
                    levelRepository.findByCharacteristicId(characteristic.getId());

            for (MercenaryCharacteristicLevel l : levels) {
                if (l.getStatType() == null) continue;
                if (l.getLevel() != level) continue;
                if (l.getAmountValue() == null) continue;
                result.merge(l.getStatType(), l.getAmountValue(), Float::sum);
            }
        }

        return result;
    }

    /** 국가 속성 버프 — Element 속성 용병에게 value만큼 ELEMENT_VALUE 가산 */
    public record NationBuff(Element element, float value) {}
}
