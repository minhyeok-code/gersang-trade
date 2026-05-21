package org.example.gersangtrade.calculator.service;

import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.catalog.repository.MercenaryCharacteristicLevelRepository;
import org.example.gersangtrade.catalog.repository.MercenaryCharacteristicRepository;
import org.example.gersangtrade.domain.catalog.Mercenary;
import org.example.gersangtrade.domain.catalog.MercenaryCharacteristic;
import org.example.gersangtrade.domain.catalog.MercenaryCharacteristicLevel;
import org.example.gersangtrade.domain.catalog.enums.Nature;
import org.example.gersangtrade.domain.catalog.enums.StatType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * 명왕 스탯 이전 계산 서비스.
 *
 * <p>이전량 = 명왕 해당 스탯 × (기본 이전율% + 특성 레벨 이전율%)
 *
 * <p>이전 대상 우선순위 (탐색 및 실제 적용은 호출자 책임):
 *   1. 같은 속성 사천왕
 *   2. 같은 속성 주인공
 *   3. 같은 속성 전설장수
 *   4. 없으면 이전 X (빈 Map 반환)
 *
 * <p>부동명왕(EARTH)은 스탯 이전 없음 → 빈 Map 반환.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyungwangStatTransferCalculator {

    private final MercenaryCharacteristicRepository characteristicRepository;
    private final MercenaryCharacteristicLevelRepository levelRepository;

    /**
     * 명왕 스탯 이전량을 계산한다.
     *
     * @param myungwang          명왕 용병
     * @param characteristicLevel 이전 특성에 배분된 레벨 (0~10)
     * @param myungwangStats     명왕의 현재 스탯 Map (StatType → 수치)
     * @return 이전량 Map (StatType → 이전 수치). 이전 없으면 빈 Map.
     */
    public Map<StatType, Float> calculate(Mercenary myungwang,
                                          int characteristicLevel,
                                          Map<StatType, Float> myungwangStats) {
        TransferConfig config = getConfig(myungwang.getNature());
        if (config == null) return Map.of();

        float baseRate = config.baseRate();
        float additionalRate = getAdditionalRate(myungwang.getId(), characteristicLevel);
        float totalRate = (baseRate + additionalRate) / 100f;

        float statValue = myungwangStats.getOrDefault(config.statType(), 0f);
        float transferAmount = statValue * totalRate;

        return Map.of(config.statType(), transferAmount);
    }

    // ── 내부 ──────────────────────────────────────────────────────────────────

    private TransferConfig getConfig(Nature nature) {
        return switch (nature) {
            case FIRE    -> new TransferConfig(StatType.STRENGTH,   10f);
            case THUNDER -> new TransferConfig(StatType.DEXTERITY,  10f);
            case WIND    -> new TransferConfig(StatType.VITALITY,    5f);
            case WATER   -> new TransferConfig(StatType.INTELLECT,   5f);
            case EARTH   -> null; // 부동명왕 이전 없음
            default      -> null;
        };
    }

    /**
     * 특성 레벨에 해당하는 추가 이전율(%)을 반환한다.
     * "이전되는" 텍스트가 포함된 label의 해당 레벨 amountValue를 조회한다.
     */
    private float getAdditionalRate(Long mercenaryId, int characteristicLevel) {
        if (characteristicLevel <= 0) return 0f;

        return characteristicRepository.findByMercenaryId(mercenaryId).stream()
                .flatMap(c -> levelRepository.findByCharacteristicId(c.getId()).stream())
                .filter(l -> isTransferLabel(l.getLabel()) && l.getLevel() == characteristicLevel)
                .map(MercenaryCharacteristicLevel::getAmountValue)
                .filter(v -> v != null)
                .findFirst()
                .orElse(0f);
    }

    private boolean isTransferLabel(String label) {
        return label != null && label.contains("이전");
    }

    public record TransferConfig(StatType statType, float baseRate) {}
}
