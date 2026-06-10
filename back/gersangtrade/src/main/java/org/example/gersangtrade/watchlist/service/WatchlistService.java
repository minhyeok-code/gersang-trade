package org.example.gersangtrade.watchlist.service;

import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.catalog.repository.EquipmentItemRepository;
import org.example.gersangtrade.catalog.repository.EquipmentSetRepository;
import org.example.gersangtrade.catalog.repository.ItemRepository;
import org.example.gersangtrade.catalog.repository.ServerRepository;
import org.example.gersangtrade.config.CacheConfig;
import org.example.gersangtrade.domain.catalog.EquipmentItem;
import org.example.gersangtrade.domain.catalog.EquipmentSet;
import org.example.gersangtrade.domain.catalog.Item;
import org.example.gersangtrade.domain.catalog.enums.ItemType;
import org.example.gersangtrade.domain.user.User;
import org.example.gersangtrade.domain.user.UserWatchTarget;
import org.example.gersangtrade.domain.user.enums.SetComposition;
import org.example.gersangtrade.domain.user.enums.WatchTargetType;
import org.example.gersangtrade.domain.user.UserRepository;
import org.example.gersangtrade.listing.service.SetTitleGenerator;
import org.example.gersangtrade.user.repository.UserWatchTargetRepository;
import org.example.gersangtrade.watchlist.dto.request.WatchTargetAddRequest;
import org.example.gersangtrade.watchlist.dto.response.WatchTargetResponse;
import org.example.gersangtrade.watchlist.exception.WatchlistException;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WatchlistService {

    private static final int WATCH_LIMIT = 5;

    private final UserWatchTargetRepository watchTargetRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final EquipmentItemRepository equipmentItemRepository;
    private final EquipmentSetRepository equipmentSetRepository;
    private final ServerRepository serverRepository;
    private final CacheManager cacheManager;

    public List<WatchTargetResponse> getList(Long userId) {
        return watchTargetRepository.findByUserIdOrderBySortOrderAscCreatedAtAsc(userId)
                .stream()
                .map(w -> WatchTargetResponse.from(w, buildDisplayLabel(w)))
                .toList();
    }

    @Transactional
    public WatchTargetResponse add(Long userId, WatchTargetAddRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));

        long count = watchTargetRepository.countByUserId(userId);
        if (count >= WATCH_LIMIT) {
            throw WatchlistException.limitExceeded((int) count, WATCH_LIMIT);
        }

        UserWatchTarget target = buildTarget(user, req);

        if (watchTargetRepository.existsByUserIdAndWatchKey(userId, target.getWatchKey())) {
            throw WatchlistException.duplicate();
        }

        UserWatchTarget saved = watchTargetRepository.save(target);
        evictPriceWatchByUser(userId);
        return WatchTargetResponse.from(saved, buildDisplayLabel(saved));
    }

    @Transactional
    public void remove(Long entryId, Long userId) {
        UserWatchTarget target = watchTargetRepository.findById(entryId)
                .orElseThrow(WatchlistException::notFound);
        if (!target.getUser().getId().equals(userId)) {
            throw WatchlistException.forbidden();
        }
        watchTargetRepository.delete(target);
        evictPriceWatchByUser(userId);
    }

    /** watchlist 변경 시 해당 userId의 priceWatch 캐시 전체 evict. allEntries=true 금지 대신 서버별 key 순회. */
    private void evictPriceWatchByUser(Long userId) {
        Cache cache = cacheManager.getCache(CacheConfig.PRICE_WATCH);
        if (cache == null) return;
        serverRepository.findAll().forEach(s -> cache.evictIfPresent(userId + ":" + s.getServerId()));
    }

    private UserWatchTarget buildTarget(User user, WatchTargetAddRequest req) {
        if (req.targetType() == WatchTargetType.ITEM) {
            return buildItemTarget(user, req);
        } else {
            return buildSetTarget(user, req);
        }
    }

    private UserWatchTarget buildItemTarget(User user, WatchTargetAddRequest req) {
        if (req.itemId() == null) {
            throw WatchlistException.invalidTarget("ITEM 타입은 itemId가 필요합니다.");
        }
        Item item = itemRepository.findById(req.itemId())
                .orElseThrow(() -> WatchlistException.invalidTarget("아이템을 찾을 수 없습니다."));

        // 재료는 ritualMark 불가
        if (item.getType() == ItemType.MATERIAL && req.ritualMark() != null) {
            throw WatchlistException.invalidTarget("재료 아이템에는 주술을 등록할 수 없습니다.");
        }

        String watchKey = WatchKeyBuilder.itemKey(item.getId(), req.ritualMark());
        return UserWatchTarget.builder()
                .user(user)
                .targetType(WatchTargetType.ITEM)
                .watchKey(watchKey)
                .item(item)
                .ritualMark(req.ritualMark())
                .build();
    }

    private UserWatchTarget buildSetTarget(User user, WatchTargetAddRequest req) {
        if (req.setId() == null || req.composition() == null) {
            throw WatchlistException.invalidTarget("SET 타입은 setId와 composition이 필요합니다.");
        }
        EquipmentSet set = equipmentSetRepository.findById(req.setId())
                .orElseThrow(() -> WatchlistException.invalidTarget("세트를 찾을 수 없습니다."));

        SetComposition composition = req.composition();
        int ritualCount = req.ritualCount() != null ? req.ritualCount() : 0;
        String ritualMark = req.ritualMark();

        // BANSSANG(반지쌍 단독)은 반지에 주술이 없으므로 RC:0 강제
        // FULL_BANSSANG은 갑옷 주술이 제목에 반영되므로 강제하지 않는다
        if (composition == SetComposition.BANSSANG) {
            ritualCount = 0;
            ritualMark = null;
        }

        String watchKey = WatchKeyBuilder.setKey(set.getId(), composition, ritualCount, ritualMark);
        return UserWatchTarget.builder()
                .user(user)
                .targetType(WatchTargetType.SET)
                .watchKey(watchKey)
                .equipmentSet(set)
                .composition(composition)
                .ritualCount(ritualCount)
                .ritualMark(ritualMark)
                .build();
    }

    private String buildDisplayLabel(UserWatchTarget w) {
        if (w.getTargetType() == WatchTargetType.ITEM) {
            String name = w.getItem() != null ? w.getItem().getName() : "알 수 없는 아이템";
            return w.getRitualMark() != null ? name + " " + w.getRitualMark() : name;
        }
        // SET
        if (w.getEquipmentSet() == null || w.getComposition() == null) return w.getWatchKey();
        return SetTitleGenerator.generateByKind(
                w.getEquipmentSet().getName(),
                w.getComposition(),
                w.getRitualCount() != null ? w.getRitualCount() : 0,
                w.getRitualMark()
        );
    }
}
