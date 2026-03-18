package org.example.gersangtrade.wanted.service;

import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.catalog.repository.EquipmentItemRepository;
import org.example.gersangtrade.catalog.repository.ItemRepository;
import org.example.gersangtrade.catalog.repository.RitualApplicabilityRepository;
import org.example.gersangtrade.domain.catalog.EquipmentItem;
import org.example.gersangtrade.domain.catalog.Item;
import org.example.gersangtrade.domain.catalog.Ritual;
import org.example.gersangtrade.domain.catalog.RitualApplicability;
import org.example.gersangtrade.domain.catalog.enums.EquipmentKind;
import org.example.gersangtrade.domain.catalog.enums.ItemType;
import org.example.gersangtrade.domain.user.User;
import org.example.gersangtrade.domain.user.UserRepository;
import org.example.gersangtrade.domain.user.enums.UserStatus;
import org.example.gersangtrade.domain.wanted.WantedEquipmentCondition;
import org.example.gersangtrade.domain.wanted.WantedItem;
import org.example.gersangtrade.domain.wanted.WantedListing;
import org.example.gersangtrade.domain.wanted.WantedRitualCondition;
import org.example.gersangtrade.domain.wanted.enums.WantedStatus;
import org.example.gersangtrade.wanted.dto.request.WantedEquipmentConditionRequest;
import org.example.gersangtrade.wanted.dto.request.WantedItemRequest;
import org.example.gersangtrade.wanted.dto.request.WantedListingCreateRequest;
import org.example.gersangtrade.wanted.dto.request.WantedRitualConditionRequest;
import org.example.gersangtrade.wanted.dto.request.WantedSearchCondition;
import org.example.gersangtrade.wanted.dto.response.WantedListingDetailResponse;
import org.example.gersangtrade.wanted.dto.response.WantedListingDetailResponse.ItemAssembly;
import org.example.gersangtrade.wanted.dto.response.WantedListingSummaryResponse;
import org.example.gersangtrade.wanted.repository.WantedEquipmentConditionRepository;
import org.example.gersangtrade.wanted.repository.WantedItemRepository;
import org.example.gersangtrade.wanted.repository.WantedListingQueryRepository;
import org.example.gersangtrade.wanted.repository.WantedListingRepository;
import org.example.gersangtrade.wanted.repository.WantedRitualConditionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 구매 희망 등록글 서비스.
 * 등록 → 목록 조회 → 상세 조회 → 취소 흐름을 담당한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WantedListingService {

    private final WantedListingRepository wantedListingRepository;
    private final WantedItemRepository wantedItemRepository;
    private final WantedEquipmentConditionRepository wantedEquipmentConditionRepository;
    private final WantedRitualConditionRepository wantedRitualConditionRepository;
    private final WantedListingQueryRepository wantedListingQueryRepository;

    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final EquipmentItemRepository equipmentItemRepository;
    private final RitualApplicabilityRepository ritualApplicabilityRepository;

    // ── 등록 ────────────────────────────────────────────────────────────────

    /**
     * 구매 희망 등록글 신규 등록.
     *
     * @param buyerId 구매자 사용자 ID (JWT에서 추출)
     * @param request 등록 요청 DTO
     * @return 등록 완료된 등록글 ID
     */
    @Transactional
    public Long createWantedListing(Long buyerId, WantedListingCreateRequest request) {
        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "존재하지 않는 사용자입니다. id=" + buyerId));

        // 차단된 사용자는 구매 희망 등록 불가
        if (buyer.getStatus() == UserStatus.BLOCKED) {
            throw new IllegalStateException("차단된 계정은 구매 희망 등록글을 등록할 수 없습니다. userId=" + buyerId);
        }

        // 아이템 목록 내 sortOrder 중복 검사
        long distinctSortOrders = request.items().stream()
                .map(WantedItemRequest::sortOrder)
                .distinct()
                .count();
        if (distinctSortOrders != request.items().size()) {
            throw new IllegalArgumentException("구매 희망 아이템 목록의 sortOrder 값이 중복되어 있습니다.");
        }

        WantedListing wantedListing = wantedListingRepository.save(
                WantedListing.builder()
                        .buyer(buyer)
                        .server(request.server())
                        .offeredPrice(request.offeredPrice())
                        .note(request.note())
                        .build()
        );

        for (WantedItemRequest itemReq : request.items()) {
            processItem(wantedListing, itemReq);
        }

        return wantedListing.getId();
    }

    /**
     * 구매 희망 아이템 단건 처리.
     * 아이템 조회 → WantedItem 저장 → 장비이면 조건·주술 조건까지 처리한다.
     */
    private void processItem(WantedListing wantedListing, WantedItemRequest itemReq) {
        Item item = itemRepository.findById(itemReq.itemId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "존재하지 않는 아이템입니다. itemId=" + itemReq.itemId()));

        WantedItem wantedItem = wantedItemRepository.save(
                WantedItem.builder()
                        .wantedListing(wantedListing)
                        .item(item)
                        .quantity(itemReq.quantity())
                        .sortOrder(itemReq.sortOrder())
                        .build()
        );

        if (itemReq.isEquipment()) {
            // 장비는 수량이 1이어야 한다
            if (itemReq.quantity() > 1) {
                throw new IllegalArgumentException(
                        "장비 아이템의 수량은 1이어야 합니다. itemId=" + itemReq.itemId());
            }
            validateAndSaveEquipmentCondition(item, wantedItem, itemReq.equipmentCondition());
        } else {
            if (item.getType() != ItemType.MATERIAL) {
                throw new IllegalArgumentException(
                        "장비 조건이 없는 아이템은 재료 타입이어야 합니다. itemId=" + item.getId());
            }
        }
    }

    /**
     * 장비 조건 검증 및 저장.
     * EquipmentItem 존재 확인, 주술 일관성, 주술 적용 가능 여부를 검증한다.
     */
    private void validateAndSaveEquipmentCondition(Item item, WantedItem wantedItem,
                                                    WantedEquipmentConditionRequest condReq) {
        if (item.getType() != ItemType.EQUIPMENT) {
            throw new IllegalArgumentException(
                    "장비 조건이 있는 아이템은 장비 타입이어야 합니다. itemId=" + item.getId());
        }

        EquipmentItem equipmentItem = equipmentItemRepository.findWithItemByItemId(item.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "장비 아이템 상세가 존재하지 않습니다. itemId=" + item.getId()));

        // 외변(APPEARANCE) 장비의 최소 강화 수치는 null 또는 5만 허용
        if (equipmentItem.getEquipmentKind() == EquipmentKind.APPEARANCE) {
            Integer minLevel = condReq.minEnhanceLevel();
            if (minLevel != null && minLevel != 5) {
                throw new IllegalArgumentException(
                        "외변 장비의 최소 강화 수치는 null 또는 5이어야 합니다. itemId=" + item.getId());
            }
        }

        // 주술 여부와 주술 조건 목록 일관성 검사
        if (!condReq.isRitualConsistent()) {
            throw new IllegalArgumentException(
                    "주술 조건(hasRitual=true) 시 주술 조건 목록은 1개 이상이어야 합니다. itemId=" + item.getId());
        }

        wantedEquipmentConditionRepository.save(
                WantedEquipmentCondition.builder()
                        .wantedItem(wantedItem)
                        .minEnhanceLevel(condReq.minEnhanceLevel())
                        .hasRitual(condReq.hasRitual())
                        .build()
        );

        if (condReq.hasRitual()) {
            saveRitualConditions(wantedItem, equipmentItem, condReq);
        }
    }

    /**
     * 주술 조건 저장.
     * 각 주술 ID가 해당 장비에 적용 가능한지 검증 후 저장한다.
     */
    private void saveRitualConditions(WantedItem wantedItem, EquipmentItem equipmentItem,
                                       WantedEquipmentConditionRequest condReq) {
        // 요청 내 중복 ritualId 검사
        long distinctRitualCount = condReq.ritualConditions().stream()
                .map(WantedRitualConditionRequest::ritualId)
                .distinct()
                .count();
        if (distinctRitualCount != condReq.ritualConditions().size()) {
            throw new IllegalArgumentException("주술 조건 목록에 중복된 ritualId가 있습니다.");
        }

        // 적용 가능한 주술 맵 구성 (ritualId → Ritual)
        Map<Long, Ritual> applicableRitualMap = ritualApplicabilityRepository
                .findByEquipmentItemIdWithRitual(equipmentItem.getItemId())
                .stream()
                .map(RitualApplicability::getRitual)
                .collect(Collectors.toMap(Ritual::getId, Function.identity()));

        List<WantedRitualCondition> conditionsToSave = new ArrayList<>();
        for (var ritualReq : condReq.ritualConditions()) {
            Ritual ritual = applicableRitualMap.get(ritualReq.ritualId());
            if (ritual == null) {
                throw new IllegalArgumentException(
                        "해당 장비에 적용 불가능한 주술입니다. ritualId=" + ritualReq.ritualId()
                        + ", equipmentItemId=" + equipmentItem.getItemId());
            }
            conditionsToSave.add(WantedRitualCondition.builder()
                    .wantedItem(wantedItem)
                    .ritual(ritual)
                    .preferredOutcome(ritualReq.preferredOutcome())
                    .build());
        }
        wantedRitualConditionRepository.saveAll(conditionsToSave);
    }

    // ── 목록 조회 ────────────────────────────────────────────────────────────

    /**
     * 동적 필터 조건으로 구매 희망 등록글 목록 조회.
     * N+1 방지를 위해 WantedItem을 IN 쿼리로 일괄 로드한다.
     */
    public List<WantedListingSummaryResponse> getWantedListings(WantedSearchCondition cond) {
        List<WantedListing> listings = wantedListingQueryRepository.search(cond);
        if (listings.isEmpty()) return List.of();

        List<Long> listingIds = listings.stream().map(WantedListing::getId).toList();

        Map<Long, List<WantedItem>> itemsByListingId = wantedItemRepository
                .findByWantedListingIdIn(listingIds)
                .stream()
                .collect(Collectors.groupingBy(wi -> wi.getWantedListing().getId()));

        return listings.stream()
                .map(listing -> WantedListingSummaryResponse.from(
                        listing,
                        itemsByListingId.getOrDefault(listing.getId(), List.of())
                ))
                .toList();
    }

    // ── 상세 조회 ────────────────────────────────────────────────────────────

    /**
     * 구매 희망 등록글 상세 조회.
     * 아이템 → 장비 조건 → 주술 조건 계층을 N+1 없이 조립한다.
     */
    public WantedListingDetailResponse getDetail(Long wantedListingId) {
        WantedListing listing = wantedListingRepository.findActiveById(wantedListingId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "존재하지 않는 구매 희망 등록글입니다. id=" + wantedListingId));

        List<WantedItem> items = wantedItemRepository
                .findByWantedListingIdOrderBySortOrderAsc(wantedListingId);

        List<Long> itemIds = items.stream().map(WantedItem::getId).toList();

        // 장비 조건 일괄 조회 (wantedItemId별 맵)
        Map<Long, WantedEquipmentCondition> conditionByItemId =
                wantedEquipmentConditionRepository.findByWantedItemIdIn(itemIds)
                        .stream()
                        .collect(Collectors.toMap(
                                c -> c.getWantedItem().getId(), Function.identity()));

        // 주술 조건 일괄 조회 (wantedItemId별 그룹)
        Map<Long, List<WantedRitualCondition>> ritualsByItemId =
                wantedRitualConditionRepository.findWithRitualByWantedItemIdIn(itemIds)
                        .stream()
                        .collect(Collectors.groupingBy(r -> r.getWantedItem().getId()));

        List<ItemAssembly> assemblies = items.stream()
                .map(item -> new ItemAssembly(
                        item,
                        conditionByItemId.get(item.getId()),
                        ritualsByItemId.getOrDefault(item.getId(), List.of())
                ))
                .toList();

        return WantedListingDetailResponse.from(listing, assemblies);
    }

    // ── 취소 ────────────────────────────────────────────────────────────────

    /**
     * 구매 희망 등록글 취소 (소프트 삭제 + CANCELLED 상태 전환).
     * 본인 등록글만 취소 가능하다.
     *
     * @param buyerId         요청자 사용자 ID
     * @param wantedListingId 취소 대상 등록글 ID
     */
    @Transactional
    public void cancelWantedListing(Long buyerId, Long wantedListingId) {
        // 취소 처리는 hidden 여부와 무관하게 소프트 삭제되지 않은 본인 등록글에 대해 수행한다
        WantedListing listing = wantedListingRepository.findNotDeletedById(wantedListingId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "존재하지 않는 구매 희망 등록글입니다. id=" + wantedListingId));

        if (!listing.getBuyer().getId().equals(buyerId)) {
            throw new IllegalArgumentException("본인의 등록글만 취소할 수 있습니다.");
        }

        // 이미 종료된 상태(PURCHASED, CANCELLED)는 취소 불가
        if (listing.getStatus() == WantedStatus.PURCHASED
                || listing.getStatus() == WantedStatus.CANCELLED) {
            throw new IllegalStateException(
                    "이미 완료되거나 취소된 구매 희망 등록글입니다. status=" + listing.getStatus());
        }

        listing.cancel();
        listing.softDelete();
    }
}
