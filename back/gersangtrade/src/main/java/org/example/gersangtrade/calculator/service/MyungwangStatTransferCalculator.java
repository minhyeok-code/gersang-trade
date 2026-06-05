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
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 명왕 스탯 이전 계산 서비스.
 *
 * <p>이전량 = 명왕 해당 스탯 × (기본 이전율% + 특성 레벨 이전율%)
 *
 * <p>이전 규칙:
 *   - 속성 제한 없음 — 임의의 명왕이 임의의 사천왕에게 이전 가능
 *   - 사천왕 1명에 명왕 1명만 배정 (1:1 제약)
 *   - 동속성 명왕·사천왕 쌍 우선 배정
 *   - 동속성 없으면 사천왕의 스탯 중 이전 대상 스탯이 더 높은 명왕 우선
 *   - 사천왕 배정 후 남은 명왕 → 주인공 → 전설장수 순으로 fallback
 *   - 부동명왕(EARTH)은 스탯 이전 없음
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
     *
     * <p>배정 순서:
     * 1. 동속성 명왕·사천왕 쌍 우선 배정 (1:1)
     * 2. 미배정 사천왕 — 남은 명왕 중 해당 사천왕의 이전 스탯이 더 높은 명왕 배정
     * 3. 미배정 명왕 — 주인공·전설장수 fallback (속성 무관, 선착순)
     */
    public ComputedTransfers computeReceivedTransfers(
            List<UserDeckMember> members,
            Map<Long, List<UserDeckMemberCharacteristic>> charsByMemberId,
            Map<Long, List<MercenaryCharacteristicLevel>> charLevelsByCharId,
            Map<Long, Map<StatType, Integer>> preTransferStatsByMemberId) {

        Map<Long, Map<StatType, Integer>> received = new HashMap<>();
        Map<Long, List<TransferDetail>> details = new HashMap<>();

        // ── 명왕 이전 정보 수집 ──────────────────────────────────────────────────
        record MyungwangInfo(UserDeckMember member, TransferConfig config, int transferAmount) {}

        List<MyungwangInfo> myungwangs = new ArrayList<>();
        for (UserDeckMember m : members) {
            MercenaryCategory cat = m.getMercenary().getCategory();
            if (cat != MercenaryCategory.MYEONG_KING && cat != MercenaryCategory.MYEONG_KING_AWAKENING) continue;

            TransferConfig config = getConfig(m.getMercenary());
            if (config == null) continue;

            int level = resolveTransferLevel(m, charsByMemberId, charLevelsByCharId);
            float additionalRate = getAdditionalRate(m.getMercenary().getId(), level);
            float totalRate = (config.baseRate() + additionalRate) / 100f;
            int statVal = preTransferStatsByMemberId.getOrDefault(m.getId(), Map.of())
                    .getOrDefault(config.statType(), 0);
            int amount = Math.round(statVal * totalRate);

            myungwangs.add(new MyungwangInfo(m, config, amount));
        }

        if (myungwangs.isEmpty()) return new ComputedTransfers(received, details);

        // ── 사천왕 목록 ──────────────────────────────────────────────────────────
        List<UserDeckMember> heavenlyKings = members.stream()
                .filter(m -> m.getMercenary().getCategory() == MercenaryCategory.FOUR_HEAVENLY_KINGS
                        || m.getMercenary().getCategory() == MercenaryCategory.FOUR_HEAVENLY_KINGS_AWAKENING)
                .collect(Collectors.toList());

        Set<Long> usedMyungwangIds = new HashSet<>();
        Map<Long, MyungwangInfo> assignment = new HashMap<>(); // 사천왕 memberId → 배정 명왕

        // ── 1단계: 동속성 우선 배정 ────────────────────────────────────────────
        for (UserDeckMember king : heavenlyKings) {
            Nature kingNature = king.getMercenary().getNature();
            for (MyungwangInfo info : myungwangs) {
                if (usedMyungwangIds.contains(info.member().getId())) continue;
                if (info.member().getMercenary().getNature() == kingNature) {
                    assignment.put(king.getId(), info);
                    usedMyungwangIds.add(info.member().getId());
                    break;
                }
            }
        }

        // ── 2단계: 미배정 사천왕 — 천왕의 이전 대상 스탯이 더 높은 명왕 우선 ──
        List<MyungwangInfo> remaining = myungwangs.stream()
                .filter(info -> !usedMyungwangIds.contains(info.member().getId()))
                .collect(Collectors.toList());

        for (UserDeckMember king : heavenlyKings) {
            if (assignment.containsKey(king.getId()) || remaining.isEmpty()) continue;

            Map<StatType, Integer> kingStats = preTransferStatsByMemberId
                    .getOrDefault(king.getId(), Map.of());

            MyungwangInfo best = remaining.stream()
                    .max(Comparator.comparingInt(
                            info -> kingStats.getOrDefault(info.config().statType(), 0)))
                    .orElse(null);

            if (best != null) {
                assignment.put(king.getId(), best);
                usedMyungwangIds.add(best.member().getId());
                remaining.remove(best);
            }
        }

        // ── 배정 결과 적용 ───────────────────────────────────────────────────────
        for (Map.Entry<Long, MyungwangInfo> entry : assignment.entrySet()) {
            applyTransfer(entry.getKey(), entry.getValue().member().getMercenary().getName(),
                    entry.getValue().config().statType(), entry.getValue().transferAmount(),
                    received, details);
        }

        // ── 3단계: 사천왕에 배정되지 않은 명왕 → 주인공·전설장수 fallback ───────
        List<MyungwangInfo> unassigned = myungwangs.stream()
                .filter(info -> !usedMyungwangIds.contains(info.member().getId()))
                .toList();

        Set<Long> usedFallbackIds = new HashSet<>();
        for (MyungwangInfo info : unassigned) {
            UserDeckMember target = findFallbackTarget(members, usedFallbackIds);
            if (target == null) continue;
            usedFallbackIds.add(target.getId());
            applyTransfer(target.getId(), info.member().getMercenary().getName(),
                    info.config().statType(), info.transferAmount(), received, details);
        }

        return new ComputedTransfers(received, details);
    }

    private void applyTransfer(Long targetId, String sourceName, StatType statType, int amount,
                                Map<Long, Map<StatType, Integer>> received,
                                Map<Long, List<TransferDetail>> details) {
        received.computeIfAbsent(targetId, id -> new EnumMap<>(StatType.class))
                .merge(statType, amount, Integer::sum);
        details.computeIfAbsent(targetId, id -> new ArrayList<>())
                .add(new TransferDetail(sourceName, statType, amount));
    }

    /** 주인공 → 전설장수 순 fallback. 속성 무관, 아직 수신하지 않은 대상만. */
    private static UserDeckMember findFallbackTarget(List<UserDeckMember> members,
                                                      Set<Long> usedIds) {
        for (UserDeckMember m : members) {
            if (isMyungwangCategory(m.getMercenary().getCategory())) continue;
            if (usedIds.contains(m.getId())) continue;
            if (m.getMercenary().getCategory() == MercenaryCategory.PROTAGONIST) return m;
        }
        for (UserDeckMember m : members) {
            if (isMyungwangCategory(m.getMercenary().getCategory())) continue;
            if (usedIds.contains(m.getId())) continue;
            if (m.getMercenary().getCategory() == MercenaryCategory.LEGENDARY_GENERAL) return m;
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
