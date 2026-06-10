package org.example.gersangtrade.watchlist.service;

import org.example.gersangtrade.catalog.repository.EquipmentItemRepository;
import org.example.gersangtrade.domain.catalog.EquipmentItem;
import org.example.gersangtrade.domain.trade.TradeConfirmed;
import org.example.gersangtrade.domain.user.UserWatchTarget;
import org.example.gersangtrade.domain.user.enums.WatchTargetType;
import org.example.gersangtrade.listing.service.SetTitleGenerator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 관심 매물과 trade_confirmed.stat_key_snapshot 매칭.
 * SET 관심: SET:{setId}: 형식의 statKey만 매칭 — 단품 ITEM 키는 제외.
 * ITEM(단품) 관심: 동일 itemId의 ITEM statKey만 — 세트 번들 거래는 제외.
 */
public final class CompletedTradeMatcher {

    private static final int RESULT_CAP = 5;

    private CompletedTradeMatcher() {}

    /** 배치 조회용 SET statKey prefix (SET:{setId}:) */
    public static Set<String> collectSetPrefixes(List<UserWatchTarget> targets) {

        Set<String> prefixes = new LinkedHashSet<>();
        for (UserWatchTarget target : targets) {
            if (target.getTargetType() == WatchTargetType.SET && target.getEquipmentSet() != null) {
                prefixes.add("SET:" + target.getEquipmentSet().getId() + ":");
            }
        }
        return prefixes;
    }

    /** exact IN 조회에 추가할 보조 ITEM 키 (ITEM 타입 전용) */
    public static List<String> additionalExactKeys(
            UserWatchTarget target,
            EquipmentItemRepository equipmentItemRepository) {

        // SET 타입은 SET statKey만 매칭 — 단품 ITEM 키를 통한 오분류 방지
        if (target.getTargetType() != WatchTargetType.ITEM || target.getItem() == null) {
            return List.of();
        }

        Long itemId = target.getItem().getId();
        List<String> keys = new ArrayList<>();
        keys.add(WatchKeyBuilder.itemKey(itemId));
        if (target.getRitualMark() != null && !target.getRitualMark().isBlank()) {
            keys.add(WatchKeyBuilder.itemKey(itemId, target.getRitualMark()));
        }
        return keys;
    }

    public static List<TradeConfirmed> resolveForTarget(
            UserWatchTarget target,
            List<String> extraExactKeys,
            Map<String, List<TradeConfirmed>> byExactKey,
            Collection<TradeConfirmed> setPrefixTrades,
            Map<Long, List<EquipmentItem>> piecesBySetId) {

        Map<Long, TradeConfirmed> merged = new LinkedHashMap<>();

        byExactKey.getOrDefault(target.getWatchKey(), List.of())
                .forEach(trade -> merged.putIfAbsent(trade.getId(), trade));

        for (String key : extraExactKeys) {
            byExactKey.getOrDefault(key, List.of()).forEach(trade -> {
                if (matches(target, trade, piecesBySetId)) {
                    merged.putIfAbsent(trade.getId(), trade);
                }
            });
        }

        if (target.getTargetType() == WatchTargetType.SET) {
            for (TradeConfirmed trade : setPrefixTrades) {
                if (matches(target, trade, piecesBySetId)) {
                    merged.putIfAbsent(trade.getId(), trade);
                }
            }
        }

        return merged.values().stream()
                .sorted(Comparator.comparing(TradeConfirmed::getConfirmedAt).reversed())
                .limit(RESULT_CAP)
                .toList();
    }

    static boolean matches(
            UserWatchTarget target,
            TradeConfirmed trade,
            Map<Long, List<EquipmentItem>> piecesBySetId) {

        String statKey = trade.getStatKeySnapshot();
        if (statKey == null || statKey.isBlank()) {
            return false;
        }
        if (Objects.equals(target.getWatchKey(), statKey)) {
            return true;
        }

        return target.getTargetType() == WatchTargetType.SET
                ? matchesSetTarget(target, statKey, piecesBySetId)
                : matchesItemTarget(target, statKey, piecesBySetId);
    }

    private static boolean matchesSetTarget(
            UserWatchTarget target,
            String statKey,
            Map<Long, List<EquipmentItem>> piecesBySetId) {

        // SET statKey만 매칭 (ITEM 키를 통한 세트 오매핑 방지)
        return StatKeyParser.parseSet(statKey)
                .filter(parsed -> target.getEquipmentSet() != null
                        && parsed.setId().equals(target.getEquipmentSet().getId()))
                .filter(parsed -> SetWatchMatcher.matchesSell(
                        target.getComposition(),
                        target.getRitualCount() != null ? target.getRitualCount() : 0,
                        target.getRitualMark(),
                        new SetTitleGenerator.WatchInfo(
                                parsed.composition(), parsed.ritualCount(), parsed.mark())))
                .isPresent();
    }

    private static boolean matchesItemTarget(
            UserWatchTarget target,
            String statKey,
            Map<Long, List<EquipmentItem>> piecesBySetId) {

        if (target.getItem() == null) {
            return false;
        }
        Long watchItemId = target.getItem().getId();
        String watchRitualMark = target.getRitualMark();

        // null == null: 주술없는 관심 → 주술없는 거래만. mark == mark: 특정 주술끼리만 일치
        return StatKeyParser.parseItem(statKey)
                .filter(itemKey -> watchItemId.equals(itemKey.itemId()))
                .map(itemKey -> Objects.equals(watchRitualMark, itemKey.ritualMark()))
                .orElse(false);
    }

    public static Map<Long, List<EquipmentItem>> loadPiecesBySetId(
            Collection<Long> setIds,
            EquipmentItemRepository equipmentItemRepository) {

        return setIds.stream()
                .distinct()
                .collect(Collectors.toMap(
                        setId -> setId,
                        equipmentItemRepository::findBySetIdWithItem,
                        (a, b) -> a,
                        LinkedHashMap::new));
    }
}
