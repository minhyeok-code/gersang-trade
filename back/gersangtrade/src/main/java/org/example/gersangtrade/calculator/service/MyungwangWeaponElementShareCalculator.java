package org.example.gersangtrade.calculator.service;

import org.example.gersangtrade.calculator.overlay.LoadedMember;
import org.example.gersangtrade.crawler.service.MyungwangWeaponPolicy;
import org.example.gersangtrade.domain.catalog.ItemStat;
import org.example.gersangtrade.domain.catalog.Mercenary;
import org.example.gersangtrade.domain.catalog.enums.BuffTarget;
import org.example.gersangtrade.domain.catalog.enums.EquipmentSlot;
import org.example.gersangtrade.domain.catalog.enums.MercenaryCategory;
import org.example.gersangtrade.domain.catalog.enums.Nature;
import org.example.gersangtrade.domain.catalog.enums.StatType;
import org.example.gersangtrade.domain.catalog.enums.StatUnit;
import org.example.gersangtrade.domain.deck.UserDeckMember;
import org.example.gersangtrade.domain.deck.UserDeckMemberSlot;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * 각성 명왕 무기(동속아군 속성+n%) 속성값 공유 계산기.
 *
 * <p>공유량 = (기본 속성값 + 장비·세트·주술 SELF 속성값) × 무기 이전율%
 * <p>가호·진법·정령·타 용병 특성·명왕부·파티 버프·스탯 이전 수신분은 소스에서 제외.
 * <p>수신: 동일 속성(Nature) 아군 전원, 착용자 본인 제외.
 */
@Service
public class MyungwangWeaponElementShareCalculator {

    public record ShareDetail(String sourceLabel, int value) {}

    public record ComputedShares(
            Map<Long, Integer> receivedElementValueByMemberId,
            Map<Long, List<ShareDetail>> detailsByMemberId
    ) {}

    public record ShareContext(Long wearerMemberId, Nature wearerNature, int ratePercent, String weaponName) {}

    /** overlay 멤버 — 각성 명왕 + 각성 무기 착용 시 이전율(%) */
    public static Optional<ShareContext> findShareContext(
            LoadedMember member, Map<Long, List<ItemStat>> itemStatsByItemId) {
        if (!isEligibleWearer(member.mercenary())) {
            return Optional.empty();
        }
        for (UserDeckMemberSlot slot : member.slots()) {
            if (slot.getEquipmentItem().getSlot() != EquipmentSlot.WEAPON) {
                continue;
            }
            String itemName = slot.getEquipmentItem().getItem().getName();
            if (!MyungwangWeaponPolicy.isAwakenedWeapon(itemName)) {
                continue;
            }
            List<ItemStat> stats = itemStatsByItemId.getOrDefault(
                    slot.getEquipmentItem().getItemId(), List.of());
            OptionalInt rate = resolveShareRatePercent(stats);
            if (rate.isPresent()) {
                return Optional.of(new ShareContext(
                        member.memberId(),
                        member.mercenary().getNature(),
                        rate.getAsInt(),
                        itemName));
            }
        }
        return Optional.empty();
    }

    /** 덱 멤버 — 무기 슬롯 item_stats에서 이전율 조회 */
    public Optional<ShareContext> findShareContext(
            UserDeckMember member,
            List<UserDeckMemberSlot> slots,
            Map<Long, List<ItemStat>> itemStatsByItemId) {

        if (!isEligibleWearer(member.getMercenary())) {
            return Optional.empty();
        }
        for (UserDeckMemberSlot slot : slots) {
            if (slot.getEquipmentItem().getSlot() != EquipmentSlot.WEAPON) {
                continue;
            }
            String itemName = slot.getEquipmentItem().getItem().getName();
            if (!MyungwangWeaponPolicy.isAwakenedWeapon(itemName)) {
                continue;
            }
            List<ItemStat> stats = itemStatsByItemId.getOrDefault(
                    slot.getEquipmentItem().getItemId(), List.of());
            OptionalInt rate = resolveShareRatePercent(stats);
            if (rate.isPresent()) {
                return Optional.of(new ShareContext(
                        member.getId(),
                        member.getMercenary().getNature(),
                        rate.getAsInt(),
                        itemName));
            }
        }
        return Optional.empty();
    }

    /**
     * 소스 속성값 — 기본 + 아이템(장비·세트·주술)만.
     * selfFlat에서 특성·SELF_AUTO 분을 제외한 ELEMENT_VALUE 합산.
     */
    public static int computeSourceElementValue(
            Map<StatType, Integer> baseStats,
            Map<StatType, Integer> selfFlatAfterItems,
            Map<StatType, Integer> characteristicFlat) {

        int baseEv = valueOf(baseStats, StatType.ELEMENT_VALUE);
        int selfEv = valueOf(selfFlatAfterItems, StatType.ELEMENT_VALUE);
        int charEv = valueOf(characteristicFlat, StatType.ELEMENT_VALUE);
        return Math.max(0, baseEv + selfEv - charEv);
    }

    public ComputedShares compute(
            List<LoadedMember> members,
            List<ShareContext> contexts,
            Map<Long, Integer> sourceEvByWearerId) {

        Map<Long, Integer> received = new HashMap<>();
        Map<Long, List<ShareDetail>> details = new HashMap<>();

        for (ShareContext ctx : contexts) {
            int sourceEv = sourceEvByWearerId.getOrDefault(ctx.wearerMemberId(), 0);
            int share = Math.round(sourceEv * ctx.ratePercent() / 100.0f);
            if (share <= 0 || ctx.wearerNature() == null) {
                continue;
            }
            String label = ctx.weaponName() + " (" + ctx.ratePercent() + "%)";

            for (LoadedMember target : members) {
                if (!isEligibleRecipient(target, ctx)) {
                    continue;
                }
                received.merge(target.memberId(), share, Integer::sum);
                details.computeIfAbsent(target.memberId(), k -> new ArrayList<>())
                        .add(new ShareDetail(label, share));
            }
        }
        return new ComputedShares(received, details);
    }

    public ComputedShares computeForDeckMembers(
            List<UserDeckMember> members,
            List<ShareContext> contexts,
            Map<Long, Integer> sourceEvByWearerId) {

        Map<Long, Integer> received = new HashMap<>();
        Map<Long, List<ShareDetail>> details = new HashMap<>();

        for (ShareContext ctx : contexts) {
            int sourceEv = sourceEvByWearerId.getOrDefault(ctx.wearerMemberId(), 0);
            int share = Math.round(sourceEv * ctx.ratePercent() / 100.0f);
            if (share <= 0 || ctx.wearerNature() == null) {
                continue;
            }
            String label = ctx.weaponName() + " (" + ctx.ratePercent() + "%)";

            for (UserDeckMember target : members) {
                if (!isEligibleDeckRecipient(target, ctx)) {
                    continue;
                }
                received.merge(target.getId(), share, Integer::sum);
                details.computeIfAbsent(target.getId(), k -> new ArrayList<>())
                        .add(new ShareDetail(label, share));
            }
        }
        return new ComputedShares(received, details);
    }

    private static boolean isEligibleWearer(Mercenary mercenary) {
        if (mercenary.getCategory() != MercenaryCategory.MYEONG_KING_AWAKENING) {
            return false;
        }
        // 부동명왕 각성은 속성 공유 없음
        return mercenary.getNature() != null && mercenary.getNature() != Nature.EARTH;
    }

    private static OptionalInt resolveShareRatePercent(List<ItemStat> weaponStats) {
        for (ItemStat stat : weaponStats) {
            if (stat.getStatType() != StatType.ELEMENT_VALUE) {
                continue;
            }
            if (stat.getScope() != BuffTarget.ALLY_SAME_ELEMENT) {
                continue;
            }
            if (stat.getStatUnit() != StatUnit.PERCENT) {
                continue;
            }
            return OptionalInt.of(stat.getValue());
        }
        return OptionalInt.empty();
    }

    private static boolean isEligibleRecipient(LoadedMember target, ShareContext ctx) {
        if (target.memberId().equals(ctx.wearerMemberId())) {
            return false;
        }
        Nature nature = target.mercenary().getNature();
        return nature != null && nature == ctx.wearerNature();
    }

    private static boolean isEligibleDeckRecipient(UserDeckMember target, ShareContext ctx) {
        if (target.getId().equals(ctx.wearerMemberId())) {
            return false;
        }
        Nature nature = target.getMercenary().getNature();
        return nature != null && nature == ctx.wearerNature();
    }

    private static int valueOf(Map<StatType, Integer> map, StatType type) {
        return map.getOrDefault(type, 0);
    }
}
