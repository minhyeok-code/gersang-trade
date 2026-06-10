package org.example.gersangtrade.home.service;

import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.catalog.repository.EquipmentItemRepository;
import org.example.gersangtrade.catalog.repository.ServerRepository;
import org.example.gersangtrade.config.CacheConfig;
import org.example.gersangtrade.domain.catalog.EquipmentItem;
import org.example.gersangtrade.domain.catalog.Server;
import org.example.gersangtrade.domain.listing.BundleEquipmentDetail;
import org.example.gersangtrade.domain.listing.BundleEquipmentRitual;
import org.example.gersangtrade.domain.listing.BundleLine;
import org.example.gersangtrade.domain.listing.ListingBundle;
import org.example.gersangtrade.domain.listing.TradeListing;
import org.example.gersangtrade.domain.listing.enums.BundleType;
import org.example.gersangtrade.domain.listing.enums.ListingStatus;
import org.example.gersangtrade.domain.trade.TradeConfirmed;
import org.example.gersangtrade.domain.user.UserWatchTarget;
import org.example.gersangtrade.domain.user.enums.WatchTargetType;
import org.example.gersangtrade.domain.wanted.WantedItem;
import org.example.gersangtrade.domain.wanted.WantedListing;
import org.example.gersangtrade.domain.wanted.enums.WantedStatus;
import org.example.gersangtrade.home.dto.BuySummary;
import org.example.gersangtrade.home.dto.CompletedSnapshot;
import org.example.gersangtrade.home.dto.CompletedSummary;
import org.example.gersangtrade.home.dto.ListingSnapshot;
import org.example.gersangtrade.home.dto.PriceWatchResponse;
import org.example.gersangtrade.home.dto.PriceWatchTargetResponse;
import org.example.gersangtrade.home.dto.SellSummary;
import org.example.gersangtrade.home.dto.WantedSnapshot;
import org.example.gersangtrade.home.exception.PriceWatchException;
import org.example.gersangtrade.listing.repository.BundleEquipmentDetailRepository;
import org.example.gersangtrade.listing.repository.BundleEquipmentRitualRepository;
import org.example.gersangtrade.listing.repository.BundleLineRepository;
import org.example.gersangtrade.listing.repository.ListingBundleRepository;
import org.example.gersangtrade.listing.repository.ListingQueryRepository;
import org.example.gersangtrade.listing.service.SetTitleGenerator;
import org.example.gersangtrade.trade.repository.TradeConfirmedRepository;
import org.example.gersangtrade.user.repository.UserWatchTargetRepository;
import org.example.gersangtrade.wanted.repository.WantedItemRepository;
import org.example.gersangtrade.wanted.repository.WantedListingQueryRepository;
import org.example.gersangtrade.watchlist.service.SetWatchMatcher;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PriceWatchService {

    private static final int CANDIDATE_FETCH = 20;
    private static final int RESULT_CAP = 5;

    private final UserWatchTargetRepository watchTargetRepository;
    private final ServerRepository serverRepository;
    private final ListingQueryRepository listingQueryRepository;
    private final WantedListingQueryRepository wantedListingQueryRepository;
    private final TradeConfirmedRepository tradeConfirmedRepository;
    private final ListingBundleRepository listingBundleRepository;
    private final BundleLineRepository bundleLineRepository;
    private final BundleEquipmentDetailRepository bundleEquipmentDetailRepository;
    private final BundleEquipmentRitualRepository bundleEquipmentRitualRepository;
    private final EquipmentItemRepository equipmentItemRepository;
    private final WantedItemRepository wantedItemRepository;

    @Cacheable(value = CacheConfig.PRICE_WATCH, key = "#userId + ':' + #serverId")
    public PriceWatchResponse getPriceWatch(Long userId, Integer serverId) {
        Server server = serverRepository.findById(serverId)
                .orElseThrow(() -> new PriceWatchException(
                        HttpStatus.NOT_FOUND, "SERVER_NOT_FOUND", "존재하지 않는 서버입니다."));

        List<UserWatchTarget> targets =
                watchTargetRepository.findByUserIdOrderBySortOrderAscCreatedAtAsc(userId);

        if (targets.isEmpty()) {
            return new PriceWatchResponse(serverId, server.getName(), List.of());
        }

        return new PriceWatchResponse(
                serverId,
                server.getName(),
                buildTargetResponses(targets, server.getName())
        );
    }

    private List<PriceWatchTargetResponse> buildTargetResponses(
            List<UserWatchTarget> targets, String serverName) {

        // 거래완료: watchKey IN 배치 1쿼리 → 앱에서 key별 top RESULT_CAP 슬라이스
        List<String> watchKeys = targets.stream().map(UserWatchTarget::getWatchKey).toList();
        Map<String, List<TradeConfirmed>> completedByKey = groupTopN(
                tradeConfirmedRepository.findRecentByStatKeysAndServer(watchKeys, serverName, watchKeys.size() * RESULT_CAP),
                TradeConfirmed::getStatKeySnapshot
        );

        // ITEM 타입 배치
        List<UserWatchTarget> itemTargets = targets.stream()
                .filter(t -> t.getTargetType() == WatchTargetType.ITEM).toList();
        Map<Long, List<TradeListing>> sellByItemId = Map.of();
        Map<Long, List<WantedListing>> buyByItemId = Map.of();
        if (!itemTargets.isEmpty()) {
            List<Long> itemIds = itemTargets.stream()
                    .map(t -> t.getItem().getId()).distinct().toList();
            sellByItemId = listingQueryRepository.searchLatestPerItemIds(
                    serverName, ListingStatus.ACTIVE, itemIds, CANDIDATE_FETCH);
            buyByItemId = wantedListingQueryRepository.searchLatestPerItemIds(
                    serverName, WantedStatus.OPEN, itemIds, CANDIDATE_FETCH);
        }

        List<PriceWatchTargetResponse> result = new ArrayList<>();
        for (UserWatchTarget target : targets) {
            result.add(target.getTargetType() == WatchTargetType.ITEM
                    ? buildItemResponse(target, serverName, sellByItemId, buyByItemId, completedByKey)
                    : buildSetResponse(target, serverName, completedByKey));
        }
        return result;
    }

    // ── ITEM ─────────────────────────────────────────────────────────────────

    private PriceWatchTargetResponse buildItemResponse(
            UserWatchTarget target,
            String serverName,
            Map<Long, List<TradeListing>> sellByItemId,
            Map<Long, List<WantedListing>> buyByItemId,
            Map<String, List<TradeConfirmed>> completedByKey) {

        Long itemId = target.getItem().getId();
        String ritualMark = target.getRitualMark();

        List<TradeListing> sellFiltered = sellByItemId.getOrDefault(itemId, List.of())
                .stream()
                .limit(RESULT_CAP)
                .toList();

        // ritual 있으면 ListingBundle 로드 없이 watchKey 정확 일치 우선 — 단품 ITEM은 BundleLine 단위 ritual 마크가 있어야 함
        // 단순화: ITEM watch의 ritualMark 2차 필터는 MVP에서 생략 (단품 DB 필터로 충분히 근사)
        List<WantedListing> buyFiltered = buyByItemId.getOrDefault(itemId, List.of())
                .stream()
                .limit(RESULT_CAP)
                .toList();

        List<TradeConfirmed> completed = completedByKey.getOrDefault(target.getWatchKey(), List.of());

        // 주술 필터링이 실제로 적용되지 않으므로, ritualMark가 있으면 정확도 경고
        String dataQuality = ritualMark != null ? "LIMITED" : "OK";

        return new PriceWatchTargetResponse(
                target.getId(),
                target.getTargetType(),
                target.getWatchKey(),
                buildDisplayLabel(target),
                toSellSummary(sellFiltered),
                toBuySummary(buyFiltered),
                toCompletedSummary(completed, dataQuality)
        );
    }

    // ── SET ──────────────────────────────────────────────────────────────────

    private PriceWatchTargetResponse buildSetResponse(
            UserWatchTarget target,
            String serverName,
            Map<String, List<TradeConfirmed>> completedByKey) {

        Long setId = target.getEquipmentSet().getId();

        SellSummary sell = buildSetSellSummary(target, serverName, setId);
        BuySummary buy = buildSetBuySummary(target, serverName, setId);
        List<TradeConfirmed> completed = completedByKey.getOrDefault(target.getWatchKey(), List.of());

        return new PriceWatchTargetResponse(
                target.getId(),
                target.getTargetType(),
                target.getWatchKey(),
                buildDisplayLabel(target),
                sell,
                buy,
                toCompletedSummary(completed, "LIMITED")
        );
    }

    private SellSummary buildSetSellSummary(
            UserWatchTarget target, String serverName, Long setId) {

        List<TradeListing> candidates = listingQueryRepository.searchLatestBySetId(
                serverName, ListingStatus.ACTIVE, setId, CANDIDATE_FETCH);

        if (candidates.isEmpty()) {
            return toSellSummary(List.of());
        }

        // 번들·라인·상세·주술 배치 로드
        List<Long> listingIds = candidates.stream().map(TradeListing::getId).toList();
        List<ListingBundle> bundles = listingBundleRepository
                .findByListingIdInAndBundleTypeAndEquipmentSet_Id(listingIds, BundleType.EQUIPMENT_SET, setId);

        List<Long> bundleIds = bundles.stream().map(ListingBundle::getId).toList();
        List<BundleLine> lines = bundleLineRepository.findByBundleIdIn(bundleIds);
        List<Long> lineIds = lines.stream().map(BundleLine::getId).toList();
        List<BundleEquipmentDetail> details =
                bundleEquipmentDetailRepository.findWithEquipmentSetByBundleLineIdIn(lineIds);
        List<BundleEquipmentRitual> rituals =
                bundleEquipmentRitualRepository.findWithRitualByBundleLineIdIn(lineIds);

        Map<Long, List<BundleLine>> linesByBundleId = lines.stream()
                .collect(Collectors.groupingBy(l -> l.getBundle().getId()));
        Map<Long, BundleEquipmentDetail> detailByLineId = details.stream()
                .collect(Collectors.toMap(BundleEquipmentDetail::getBundleLineId, d -> d));
        Map<Long, List<BundleEquipmentRitual>> ritualsByLineId = rituals.stream()
                .collect(Collectors.groupingBy(r -> r.getBundleLine().getId()));
        // 번들별 listingId 역매핑
        Map<Long, Long> listingIdByBundleId = bundles.stream()
                .collect(Collectors.toMap(ListingBundle::getId, b -> b.getListing().getId()));

        // 2차 필터: composition·ritualCount·ritualMark 매칭
        Set<Long> matchedListingIds = bundles.stream()
                .filter(bundle -> {
                    List<BundleLine> bundleLines = linesByBundleId.getOrDefault(bundle.getId(), List.of());
                    SetTitleGenerator.WatchInfo info = buildWatchInfo(bundleLines, detailByLineId, ritualsByLineId);
                    return SetWatchMatcher.matchesSell(
                            target.getComposition(),
                            target.getRitualCount() != null ? target.getRitualCount() : 0,
                            target.getRitualMark(),
                            info);
                })
                .map(b -> listingIdByBundleId.get(b.getId()))
                .collect(Collectors.toSet());

        List<TradeListing> matched = candidates.stream()
                .filter(l -> matchedListingIds.contains(l.getId()))
                .limit(RESULT_CAP)
                .toList();

        return toSellSummary(matched);
    }

    private BuySummary buildSetBuySummary(
            UserWatchTarget target, String serverName, Long setId) {

        List<EquipmentItem> pieces = equipmentItemRepository.findBySetIdWithItem(setId);
        if (pieces.isEmpty()) {
            return toBuySummary(List.of());
        }

        List<Long> setPieceItemIds = pieces.stream().map(EquipmentItem::getItemId).toList();

        List<WantedListing> candidates = wantedListingQueryRepository.searchLatestBySetPieceIds(
                serverName, WantedStatus.OPEN, setPieceItemIds, CANDIDATE_FETCH);

        if (candidates.isEmpty()) {
            return toBuySummary(List.of());
        }

        // WantedItem 배치 로드 (N+1 방지)
        List<Long> listingIds = candidates.stream().map(WantedListing::getId).toList();
        List<WantedItem> allItems = wantedItemRepository.findByWantedListingIdIn(listingIds);
        Map<Long, List<Long>> itemIdsByListingId = allItems.stream()
                .collect(Collectors.groupingBy(
                        wi -> wi.getWantedListing().getId(),
                        Collectors.mapping(wi -> wi.getItem().getId(), Collectors.toList())
                ));

        Set<Long> pieceIdSet = Set.copyOf(setPieceItemIds);
        List<WantedListing> matched = candidates.stream()
                .filter(l -> SetWatchMatcher.matchesBuy(
                        pieceIdSet, itemIdsByListingId.getOrDefault(l.getId(), List.of())))
                .limit(RESULT_CAP)
                .toList();

        return toBuySummary(matched);
    }

    // ── 유틸 ─────────────────────────────────────────────────────────────────

    private SetTitleGenerator.WatchInfo buildWatchInfo(
            List<BundleLine> bundleLines,
            Map<Long, BundleEquipmentDetail> detailByLineId,
            Map<Long, List<BundleEquipmentRitual>> ritualsByLineId) {

        List<SetTitleGenerator.PieceTitleInput> inputs = new ArrayList<>();
        for (BundleLine line : bundleLines) {
            BundleEquipmentDetail detail = detailByLineId.get(line.getId());
            if (detail == null) continue;
            String displayMark = null;
            if (detail.isHasRitual()) {
                List<BundleEquipmentRitual> lineRituals = ritualsByLineId.getOrDefault(line.getId(), List.of());
                if (!lineRituals.isEmpty()) {
                    BundleEquipmentRitual first = lineRituals.get(0);
                    displayMark = SetTitleGenerator.buildTitleMark(first.getRitual(), first.getOutcome());
                }
            }
            inputs.add(new SetTitleGenerator.PieceTitleInput(detail.getEquipmentItem().getSlot(), displayMark));
        }
        return SetTitleGenerator.resolveWatchInfo(inputs);
    }

    private String buildDisplayLabel(UserWatchTarget w) {
        if (w.getTargetType() == WatchTargetType.ITEM) {
            String name = w.getItem() != null ? w.getItem().getName() : "알 수 없는 아이템";
            return w.getRitualMark() != null ? name + " " + w.getRitualMark() : name;
        }
        if (w.getEquipmentSet() == null || w.getComposition() == null) return w.getWatchKey();
        return SetTitleGenerator.generateByKind(
                w.getEquipmentSet().getName(),
                w.getComposition(),
                w.getRitualCount() != null ? w.getRitualCount() : 0,
                w.getRitualMark());
    }

    private SellSummary toSellSummary(List<TradeListing> listings) {
        Long avg = toAvg(listings.stream().map(TradeListing::getPrice).toList());
        return new SellSummary(avg, listings.size(),
                listings.stream().map(ListingSnapshot::from).toList());
    }

    private BuySummary toBuySummary(List<WantedListing> listings) {
        Long avg = toAvg(listings.stream().map(WantedListing::getOfferedPrice).toList());
        return new BuySummary(avg, listings.size(),
                listings.stream().map(WantedSnapshot::from).toList());
    }

    private CompletedSummary toCompletedSummary(List<TradeConfirmed> trades, String dataQuality) {
        return new CompletedSummary(
                trades.size(),
                dataQuality,
                trades.stream().map(CompletedSnapshot::from).toList()
        );
    }

    /** 가격 목록의 산술 평균. 빈 목록이면 null (NaN 방지). */
    private Long toAvg(List<Long> prices) {
        if (prices.isEmpty()) return null;
        return (long) prices.stream().mapToLong(Long::longValue).average().orElse(0);
    }

    /** items를 keyMapper 기준으로 그루핑하여 각 key별 최대 RESULT_CAP건 반환. */
    private <T> Map<String, List<T>> groupTopN(List<T> items, java.util.function.Function<T, String> keyMapper) {
        Map<String, List<T>> result = new LinkedHashMap<>();
        for (T item : items) {
            String key = keyMapper.apply(item);
            List<T> bucket = result.computeIfAbsent(key, k -> new ArrayList<>());
            if (bucket.size() < RESULT_CAP) {
                bucket.add(item);
            }
        }
        return result;
    }
}
