package org.example.gersangtrade.calculator.service;

import org.example.gersangtrade.calculator.dto.request.BonusStatTarget;
import org.example.gersangtrade.calculator.dto.request.MemberDpsInput;
import org.example.gersangtrade.calculator.overlay.LoadedMember;
import org.example.gersangtrade.domain.catalog.Mercenary;
import org.example.gersangtrade.domain.catalog.enums.EquipmentSlot;
import org.example.gersangtrade.domain.catalog.enums.MercenaryCategory;
import org.example.gersangtrade.domain.catalog.enums.Nature;
import org.example.gersangtrade.domain.catalog.enums.StatType;
import org.example.gersangtrade.domain.deck.UserDeckMember;
import org.example.gersangtrade.domain.deck.UserDeckMemberSlot;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Function;

/**
 * 부동명왕 무기(명왕월/고급명왕월) 올스텟 → 아군 주스텟 이전 계산기.
 *
 * <p>이전량 = (기본+장비·세트·주술 스탯 합 + 레벨 스탯 + 보너스 스탯) × 이전율%
 * <p>수신: 속성(Nature)이 있는 모든 용병, 부동명왕 본인 제외. 각자 주스텟에 가산.
 */
@Service
public class BudongMyungwangWeaponTransferCalculator {

    private static final List<StatType> FOUR_MAIN = List.of(
            StatType.STRENGTH, StatType.DEXTERITY, StatType.VITALITY, StatType.INTELLECT);

    public record TransferDetail(String sourceLabel, StatType statType, int value) {}

    public record ComputedTransfers(
            Map<Long, Map<StatType, Integer>> receivedByMemberId,
            Map<Long, List<TransferDetail>> detailsByMemberId
    ) {}

    public record WeaponContext(Long budongMemberId, int transferRatePercent) {}

    /** 명왕월 10%, 고급명왕월 15% */
    public static OptionalInt resolveTransferRatePercent(String itemName) {
        if (itemName == null || itemName.isBlank()) {
            return OptionalInt.empty();
        }
        if (itemName.startsWith("고급명왕월")) {
            return OptionalInt.of(15);
        }
        if (itemName.startsWith("명왕월")) {
            return OptionalInt.of(10);
        }
        return OptionalInt.empty();
    }

    public static boolean isBudongMyeongwang(Mercenary mercenary) {
        return mercenary.getCategory() == MercenaryCategory.MYEONG_KING
                && mercenary.getNature() == Nature.EARTH;
    }

    /** overlay 멤버에서 부동명왕 + 이전 무기 착용 여부 */
    public static Optional<WeaponContext> findWeaponContext(LoadedMember member) {
        if (!isBudongMyeongwang(member.mercenary())) {
            return Optional.empty();
        }
        for (UserDeckMemberSlot slot : member.slots()) {
            if (slot.getEquipmentItem().getSlot() != EquipmentSlot.WEAPON) {
                continue;
            }
            OptionalInt rate = resolveTransferRatePercent(slot.getEquipmentItem().getItem().getName());
            if (rate.isPresent()) {
                return Optional.of(new WeaponContext(member.memberId(), rate.getAsInt()));
            }
        }
        return Optional.empty();
    }

    /** 덱 엔티티 멤버에서 부동명왕 + 이전 무기 착용 여부 */
    public Optional<WeaponContext> findWeaponContext(
            UserDeckMember member, List<UserDeckMemberSlot> slots) {
        if (!isBudongMyeongwang(member.getMercenary())) {
            return Optional.empty();
        }
        for (UserDeckMemberSlot slot : slots) {
            if (slot.getEquipmentItem().getSlot() != EquipmentSlot.WEAPON) {
                continue;
            }
            OptionalInt rate = resolveTransferRatePercent(slot.getEquipmentItem().getItem().getName());
            if (rate.isPresent()) {
                return Optional.of(new WeaponContext(member.getId(), rate.getAsInt()));
            }
        }
        return Optional.empty();
    }

    /**
     * 올스텟 합 — 기본+아이템(장비·세트·주술) 4스탯 + 레벨·보너스(각 1회).
     * ALL_STAT은 4스탯 각각에 동일 가산 후 합산한다.
     */
    public static int computeSourceTotal(
            Map<StatType, Integer> baseStats,
            Map<StatType, Integer> itemInducedFlat,
            Map<StatType, Integer> itemInducedPercent,
            int levelStat,
            int bonusAmount) {

        int allStat = valueOf(baseStats, StatType.ALL_STAT) + valueOf(itemInducedFlat, StatType.ALL_STAT);
        int sum = 0;
        for (StatType type : FOUR_MAIN) {
            int flat = valueOf(baseStats, type) + valueOf(itemInducedFlat, type) + allStat;
            int pct = valueOf(itemInducedPercent, type);
            sum += Math.round(flat * (1.0 + pct / 100.0));
        }
        return sum + levelStat + bonusAmount;
    }

    public static int resolveLevelStat(MemberDpsInput input) {
        return input.level() == 260
                ? MemberBuildStatCalculator.LEVEL_STAT_260
                : MemberBuildStatCalculator.LEVEL_STAT_250;
    }

    public static int resolveBonusAmount(MemberDpsInput input) {
        return Math.max(0, input.bonusAmount());
    }

    /** pref 맵을 total에서 차감 — 특성·SELF_AUTO 등 아이템 외 스탯 제거용 */
    public static Map<StatType, Integer> subtractStatMaps(
            Map<StatType, Integer> total, Map<StatType, Integer> toSubtract) {
        Map<StatType, Integer> result = new EnumMap<>(total);
        toSubtract.forEach((k, v) -> result.merge(k, -v, Integer::sum));
        return result;
    }

    public ComputedTransfers compute(
            List<LoadedMember> members,
            WeaponContext weapon,
            int sourceTotal,
            Map<Long, MemberDpsInput> inputMap,
            Function<LoadedMember, StatType> mainStatResolver) {

        int transferAmount = Math.round(sourceTotal * weapon.transferRatePercent() / 100.0f);
        if (transferAmount <= 0) {
            return emptyTransfers();
        }

        Map<Long, Map<StatType, Integer>> received = new HashMap<>();
        Map<Long, List<TransferDetail>> details = new HashMap<>();
        String sourceLabel = "부동명왕 무기";

        for (LoadedMember target : members) {
            if (!isEligibleRecipient(target, weapon.budongMemberId())) {
                continue;
            }
            StatType mainStat = mainStatResolver.apply(target);
            received.computeIfAbsent(target.memberId(), k -> new EnumMap<>(StatType.class))
                    .merge(mainStat, transferAmount, Integer::sum);
            details.computeIfAbsent(target.memberId(), k -> new ArrayList<>())
                    .add(new TransferDetail(sourceLabel, mainStat, transferAmount));
        }
        return new ComputedTransfers(received, details);
    }

    public ComputedTransfers computeForDeckMembers(
            List<UserDeckMember> members,
            WeaponContext weapon,
            int sourceTotal,
            Map<Long, StatType> mainStatByMemberId) {

        int transferAmount = Math.round(sourceTotal * weapon.transferRatePercent() / 100.0f);
        if (transferAmount <= 0) {
            return emptyTransfers();
        }

        Map<Long, Map<StatType, Integer>> received = new HashMap<>();
        Map<Long, List<TransferDetail>> details = new HashMap<>();
        String sourceLabel = "부동명왕 무기";

        for (UserDeckMember target : members) {
            if (!isEligibleDeckRecipient(target, weapon.budongMemberId())) {
                continue;
            }
            StatType mainStat = mainStatByMemberId.getOrDefault(target.getId(), StatType.VITALITY);
            received.computeIfAbsent(target.getId(), k -> new EnumMap<>(StatType.class))
                    .merge(mainStat, transferAmount, Integer::sum);
            details.computeIfAbsent(target.getId(), k -> new ArrayList<>())
                    .add(new TransferDetail(sourceLabel, mainStat, transferAmount));
        }
        return new ComputedTransfers(received, details);
    }

    /** 덱 UI용 — MemberStatComponents에서 올스텟 합 산출 */
    public static int computeSourceTotalFromDeckComponents(
            Map<StatType, Integer> baseStatMap,
            Map<StatType, Integer> equipStatMap,
            Map<StatType, Integer> setEffectStatMap,
            Map<StatType, Integer> ritualStatMap,
            Map<StatType, Integer> ritualSetEffectStatMap,
            Map<StatType, Integer> levelBonusStatMap,
            Map<StatType, Integer> bonusStatMap) {

        Map<StatType, Integer> itemFlat = new EnumMap<>(StatType.class);
        mergeInto(itemFlat, equipStatMap);
        mergeInto(itemFlat, setEffectStatMap);
        mergeInto(itemFlat, ritualStatMap);
        mergeInto(itemFlat, ritualSetEffectStatMap);

        int level = levelBonusStatMap.values().stream().mapToInt(Integer::intValue).sum();
        int bonus = bonusStatMap.values().stream().mapToInt(Integer::intValue).sum();
        return computeSourceTotal(baseStatMap, itemFlat, Map.of(), level, bonus);
    }

    private static boolean isEligibleRecipient(LoadedMember target, Long budongMemberId) {
        if (target.memberId().equals(budongMemberId)) {
            return false;
        }
        return target.mercenary().getNature() != null;
    }

    private static boolean isEligibleDeckRecipient(UserDeckMember target, Long budongMemberId) {
        if (target.getId().equals(budongMemberId)) {
            return false;
        }
        return target.getMercenary().getNature() != null;
    }

    private static int valueOf(Map<StatType, Integer> map, StatType type) {
        return map.getOrDefault(type, 0);
    }

    private static void mergeInto(Map<StatType, Integer> target, Map<StatType, Integer> source) {
        if (source == null) {
            return;
        }
        source.forEach((k, v) -> target.merge(k, v, Integer::sum));
    }

    private static ComputedTransfers emptyTransfers() {
        return new ComputedTransfers(Map.of(), Map.of());
    }
}
