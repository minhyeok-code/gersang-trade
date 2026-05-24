package org.example.gersangtrade.calculator.service;

import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.catalog.repository.MercenaryCharacteristicLevelRepository;
import org.example.gersangtrade.catalog.repository.MercenaryCharacteristicRepository;
import org.example.gersangtrade.domain.catalog.Mercenary;
import org.example.gersangtrade.domain.catalog.MercenaryCharacteristic;
import org.example.gersangtrade.domain.catalog.MercenaryCharacteristicLevel;
import org.example.gersangtrade.domain.catalog.enums.MercenaryCategory;
import org.example.gersangtrade.domain.catalog.enums.Nature;
import org.example.gersangtrade.domain.catalog.enums.StatType;
import org.example.gersangtrade.domain.deck.UserDeckMember;
import org.example.gersangtrade.domain.deck.UserDeckMemberCharacteristic;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 명왕 스탯 이전 계산 서비스.
 *
 * <p>이전량 = 명왕 해당 스탯 × (기본 이전율% + 특성 레벨 이전율%)
 *
 * <p>이전 대상 우선순위 — 명왕당 동속성 대상 1명만 수신:
 *   1. 같은 속성 사천왕
 *   2. 같은 속성 주인공
 *   3. 같은 속성 전설장수
 *   4. 없으면 이전 X
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
        TransferConfig config = getConfig(myungwang);
        if (config == null) return Map.of();

        float baseRate = config.baseRate();
        float additionalRate = getAdditionalRate(myungwang.getId(), characteristicLevel);
        float totalRate = (baseRate + additionalRate) / 100f;

        float statValue = myungwangStats.getOrDefault(config.statType(), 0f);
        float transferAmount = statValue * totalRate;

        return Map.of(config.statType(), transferAmount);
    }

    public record TransferDetail(String sourceMercenaryName, StatType statType, int value) {}

    public record ComputedTransfers(
            Map<Long, Map<StatType, Integer>> receivedByMemberId,
            Map<Long, List<TransferDetail>> detailsByMemberId
    ) {}

    /**
     * 덱 내 모든 명왕의 스탯 이전을 계산한다.
     * 각 명왕의 이전량은 우선순위에 따라 찾은 수신 멤버 1명에게만 합산된다.
     */
    public ComputedTransfers computeReceivedTransfers(
            List<UserDeckMember> members,
            Map<Long, List<UserDeckMemberCharacteristic>> charsByMemberId,
            Map<Long, List<MercenaryCharacteristicLevel>> charLevelsByCharId,
            Map<Long, Map<StatType, Integer>> preTransferStatsByMemberId) {

        Map<Long, Map<StatType, Integer>> received = new HashMap<>();
        Map<Long, List<TransferDetail>> details = new HashMap<>();

        for (UserDeckMember member : members) {
            MercenaryCategory category = member.getMercenary().getCategory();
            if (category != MercenaryCategory.MYEONG_KING
                    && category != MercenaryCategory.MYEONG_KING_AWAKENING) {
                continue;
            }

            int transferLevel = resolveTransferLevel(member, charsByMemberId, charLevelsByCharId);
            Map<StatType, Integer> myungwangStats =
                    preTransferStatsByMemberId.getOrDefault(member.getId(), Map.of());
            Map<StatType, Float> myungwangStatsFloat = new EnumMap<>(StatType.class);
            myungwangStats.forEach((k, v) -> myungwangStatsFloat.put(k, v.floatValue()));

            Map<StatType, Float> transferAmount =
                    calculate(member.getMercenary(), transferLevel, myungwangStatsFloat);
            if (transferAmount.isEmpty()) continue;

            UserDeckMember target = findTransferTarget(members, member.getMercenary().getNature());
            if (target == null) continue;

            String sourceName = member.getMercenary().getName();
            transferAmount.forEach((statType, amount) -> {
                int rounded = Math.round(amount);
                received.computeIfAbsent(target.getId(), id -> new EnumMap<>(StatType.class))
                        .merge(statType, rounded, Integer::sum);
                details.computeIfAbsent(target.getId(), id -> new ArrayList<>())
                        .add(new TransferDetail(sourceName, statType, rounded));
            });
        }
        return new ComputedTransfers(received, details);
    }

    /** 이전 대상 탐색 — 사천왕(동속성) → 주인공(동속성) → 전설장수(동속성). 명왕 본인은 제외. */
    public static UserDeckMember findTransferTarget(List<UserDeckMember> members, Nature nature) {
        for (UserDeckMember m : members) {
            if (isMyungwangCategory(m.getMercenary().getCategory())) continue;
            MercenaryCategory cat = m.getMercenary().getCategory();
            if ((cat == MercenaryCategory.FOUR_HEAVENLY_KINGS
                    || cat == MercenaryCategory.FOUR_HEAVENLY_KINGS_AWAKENING)
                    && m.getMercenary().getNature() == nature) {
                return m;
            }
        }
        for (UserDeckMember m : members) {
            if (isMyungwangCategory(m.getMercenary().getCategory())) continue;
            if (m.getMercenary().getCategory() == MercenaryCategory.PROTAGONIST
                    && m.getMercenary().getNature() == nature) {
                return m;
            }
        }
        for (UserDeckMember m : members) {
            if (isMyungwangCategory(m.getMercenary().getCategory())) continue;
            if (m.getMercenary().getCategory() == MercenaryCategory.LEGENDARY_GENERAL
                    && m.getMercenary().getNature() == nature) {
                return m;
            }
        }
        return null;
    }

    private static boolean isMyungwangCategory(MercenaryCategory category) {
        return category == MercenaryCategory.MYEONG_KING
                || category == MercenaryCategory.MYEONG_KING_AWAKENING;
    }

    // ── 내부 ──────────────────────────────────────────────────────────────────

    private int resolveTransferLevel(
            UserDeckMember member,
            Map<Long, List<UserDeckMemberCharacteristic>> charsByMemberId,
            Map<Long, List<MercenaryCharacteristicLevel>> charLevelsByCharId) {
        for (UserDeckMemberCharacteristic mc : charsByMemberId.getOrDefault(member.getId(), List.of())) {
            boolean isTransferChar = charLevelsByCharId
                    .getOrDefault(mc.getCharacteristic().getId(), List.of())
                    .stream()
                    .anyMatch(l -> CharacteristicScopeResolver.isStatTransferRateLabel(l.getLabel()));
            if (isTransferChar) {
                return mc.getSelectedLevel();
            }
        }
        return 0;
    }

    private TransferConfig getConfig(Mercenary myungwang) {
        Nature nature = myungwang.getNature();
        boolean awakened = myungwang.getCategory() == MercenaryCategory.MYEONG_KING_AWAKENING;
        return switch (nature) {
            case FIRE    -> new TransferConfig(StatType.STRENGTH,   10f);
            case THUNDER -> new TransferConfig(StatType.DEXTERITY,  10f);
            case WIND    -> new TransferConfig(StatType.VITALITY,   awakened ? 10f : 5f);
            case WATER   -> new TransferConfig(StatType.INTELLECT,  awakened ? 10f : 5f);
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
        return CharacteristicScopeResolver.isStatTransferRateLabel(label);
    }

    public record TransferConfig(StatType statType, float baseRate) {}
}
