package org.example.gersangtrade.calculator.service;

import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.catalog.repository.MercenaryCharacteristicLevelRepository;
import org.example.gersangtrade.catalog.repository.MercenaryCharacteristicRepository;
import org.example.gersangtrade.domain.catalog.Mercenary;
import org.example.gersangtrade.domain.catalog.MercenaryCharacteristicLevel;
import org.example.gersangtrade.domain.catalog.enums.Nature;
import org.example.gersangtrade.domain.catalog.enums.StatType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * 각성 명왕 버프 계산 서비스.
 *
 * <p>공통 고정 버프 (엔티티 저장 없음, 하드코딩):
 *   - 자신 속성값 +15 (SELF_AUTO, 각성 특성으로 별도 저장)
 *   - 모든 아군 속성값 +5 (EARTH 속성 아군은 +2)
 *
 * <p>스탯 이전 대상: 해당 스탯이 가장 높은 아군 (속성 무관).
 * 이전 대상 우선순위: 사천왕 → 주인공 → 전설장수.
 * 기본 이전율: 전원 10%.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AwakenedMyungwangBuffCalculator {

    private final MercenaryCharacteristicRepository characteristicRepository;
    private final MercenaryCharacteristicLevelRepository levelRepository;

    /**
     * 각성 명왕 공통 아군 속성값 버프를 반환한다.
     * ADAPTIVE: 모든 아군 +5, EARTH 속성 아군 +2.
     */
    public AllyElementBuff getCommonAllyBuff() {
        return new AllyElementBuff(5f, 2f);
    }

    /**
     * 스탯 이전량을 계산한다.
     *
     * @param myungwang           각성 명왕 용병
     * @param characteristicLevel 이전 특성에 배분된 레벨 (0~5)
     * @param myungwangStats      명왕의 현재 스탯 Map (StatType → 수치)
     * @return 이전량 Map (StatType → 이전 수치). 이전 없으면 빈 Map.
     */
    public Map<StatType, Float> calculateTransfer(Mercenary myungwang,
                                                   int characteristicLevel,
                                                   Map<StatType, Float> myungwangStats) {
        TransferConfig config = getConfig(myungwang.getNature());
        if (config == null) return Map.of();

        float additionalRate = getAdditionalRate(myungwang.getId(), characteristicLevel);
        float totalRate = (10f + additionalRate) / 100f;

        float statValue = myungwangStats.getOrDefault(config.statType(), 0f);
        float transferAmount = statValue * totalRate;

        return Map.of(config.statType(), transferAmount);
    }

    // ── 내부 ──────────────────────────────────────────────────────────────────

    private TransferConfig getConfig(Nature nature) {
        return switch (nature) {
            case FIRE    -> new TransferConfig(StatType.STRENGTH);
            case THUNDER -> new TransferConfig(StatType.DEXTERITY);
            case WIND    -> new TransferConfig(StatType.VITALITY);
            case WATER   -> new TransferConfig(StatType.INTELLECT);
            default      -> null;
        };
    }

    /**
     * 이전율 label의 해당 레벨 값을 반환한다.
     * label에 "이전율" 텍스트가 포함된 행의 level == characteristicLevel 인 amountValue.
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
        return label != null && label.contains("이전율");
    }

    /** 공통 아군 속성값 버프 — 일반 속성 +5, EARTH 속성 +2 */
    public record AllyElementBuff(float defaultValue, float earthValue) {}

    public record TransferConfig(StatType statType) {}
}
