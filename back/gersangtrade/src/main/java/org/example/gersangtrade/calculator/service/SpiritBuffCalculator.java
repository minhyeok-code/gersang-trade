package org.example.gersangtrade.calculator.service;

import org.example.gersangtrade.domain.catalog.Spirit;
import org.example.gersangtrade.domain.catalog.SpiritBuff;
import org.example.gersangtrade.domain.catalog.enums.Nature;
import org.example.gersangtrade.domain.catalog.enums.SpiritGrade;
import org.example.gersangtrade.domain.catalog.enums.StatType;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 장착된 정령 최대 2개를 받아 최종 합산 버프 Map을 반환한다.
 *
 * <p>땅 전설(어린 토석동) 특수 처리:
 * 타 정령의 버프에 ×2 적용 (ATTACK_SPEED, MOVE_SPEED 제외).
 * 땅 전설 본인 버프는 ×2 대상이 아님.
 *
 * <p>호출 전 검증 전제조건:
 * - equippedSpirits.size() <= 2
 * - 동일 정령(같은 id) 중복 장착 불가
 */
@Service
public class SpiritBuffCalculator {

    /** 땅 전설 ×2 효과 제외 대상 스탯 */
    private static final Set<StatType> SPEED_STATS = Set.of(
            StatType.ATTACK_SPEED,
            StatType.MOVE_SPEED
    );

    /**
     * 장착 정령 목록을 받아 최종 합산 버프를 반환한다.
     *
     * @param equippedSpirits 장착된 정령 목록 (buffs 페치 필요)
     * @return StatType → 합산 수치 Map
     */
    public Map<StatType, Float> calculate(List<Spirit> equippedSpirits) {
        if (equippedSpirits == null || equippedSpirits.isEmpty()) {
            return Map.of();
        }
        if (equippedSpirits.size() > 2) {
            throw new IllegalArgumentException("정령은 최대 2개까지 장착 가능합니다.");
        }

        long distinctCount = equippedSpirits.stream().map(Spirit::getId).distinct().count();
        if (distinctCount != equippedSpirits.size()) {
            throw new IllegalArgumentException("동일한 정령을 중복 장착할 수 없습니다.");
        }

        boolean hasEarthLegend = equippedSpirits.stream()
                .anyMatch(s -> s.getNature() == Nature.EARTH
                        && s.getGrade() == SpiritGrade.LEGEND);

        Map<StatType, Float> result = new HashMap<>();

        for (Spirit spirit : equippedSpirits) {
            boolean isEarthLegend = spirit.getNature() == Nature.EARTH
                    && spirit.getGrade() == SpiritGrade.LEGEND;

            for (SpiritBuff buff : spirit.getBuffs()) {
                float value = buff.getValue();

                // 땅 전설 효과: 다른 정령 버프에 ×2 (공속·이속 제외)
                if (hasEarthLegend && !isEarthLegend && !SPEED_STATS.contains(buff.getStatType())) {
                    value *= 2.0f;
                }

                result.merge(buff.getStatType(), value, Float::sum);
            }
        }

        return result;
    }
}
