package org.example.gersangtrade.listing.service;

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
import org.example.gersangtrade.domain.listing.enums.BundleType;
import org.example.gersangtrade.domain.listing.enums.ListingStatus;
import org.example.gersangtrade.domain.user.enums.UserStatus;
import org.example.gersangtrade.domain.listing.BundleEquipmentDetail;
import org.example.gersangtrade.domain.listing.BundleEquipmentRitual;
import org.example.gersangtrade.domain.listing.BundleLine;
import org.example.gersangtrade.domain.listing.ListingBundle;
import org.example.gersangtrade.domain.listing.TradeListing;
import org.example.gersangtrade.domain.listing.enums.RitualOutcome;
import org.example.gersangtrade.domain.user.User;
import org.example.gersangtrade.domain.user.UserRepository;
import org.example.gersangtrade.listing.dto.request.BundleCreateRequest;
import org.example.gersangtrade.listing.dto.request.BundleLineCreateRequest;
import org.example.gersangtrade.listing.dto.request.EquipmentDetailRequest;
import org.example.gersangtrade.listing.dto.request.ListingCreateRequest;
import org.example.gersangtrade.listing.dto.request.ListingSearchCondition;
import org.example.gersangtrade.listing.dto.request.ListingUpdateRequest;
import org.example.gersangtrade.listing.dto.request.RitualResultRequest;
import org.example.gersangtrade.listing.dto.response.ListingDetailResponse;
import org.example.gersangtrade.listing.dto.response.ListingDetailResponse.BundleAssembly;
import org.example.gersangtrade.listing.dto.response.ListingDetailResponse.LineAssembly;
import org.example.gersangtrade.listing.dto.response.ListingSummaryResponse;
import org.example.gersangtrade.listing.repository.BundleEquipmentDetailRepository;
import org.example.gersangtrade.listing.repository.BundleEquipmentRitualRepository;
import org.example.gersangtrade.listing.repository.BundleLineRepository;
import org.example.gersangtrade.listing.repository.ListingBundleRepository;
import org.example.gersangtrade.listing.repository.ListingQueryRepository;
import org.example.gersangtrade.listing.repository.TradeListingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 거래 등록글 서비스.
 * 등록 → 목록 조회 → 상세 조회 흐름을 담당한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ListingService {

    private final TradeListingRepository tradeListingRepository;
    private final ListingBundleRepository listingBundleRepository;
    private final BundleLineRepository bundleLineRepository;
    private final BundleEquipmentDetailRepository bundleEquipmentDetailRepository;
    private final BundleEquipmentRitualRepository bundleEquipmentRitualRepository;
    private final ListingQueryRepository listingQueryRepository;

    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final EquipmentItemRepository equipmentItemRepository;
    private final RitualApplicabilityRepository ritualApplicabilityRepository;

    // ── 등록 ────────────────────────────────────────────────────────────────

    /**
     * 거래 등록글 신규 등록.
     * 번들 → 라인 → 장비 상세 → 주술 순서로 계층 저장한다.
     *
     * @param sellerId 판매자 사용자 ID (JWT에서 추출)
     * @param request  등록 요청 DTO
     * @return 등록 완료된 등록글 ID
     */
    @Transactional
    public Long createListing(Long sellerId, ListingCreateRequest request) {
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다. id=" + sellerId));

        // 차단된 사용자는 거래 등록 불가
        if (seller.getStatus() == UserStatus.BLOCKED) {
            throw new IllegalStateException("차단된 계정은 거래 등록글을 등록할 수 없습니다. userId=" + sellerId);
        }

        TradeListing listing = tradeListingRepository.save(
                TradeListing.builder()
                        .seller(seller)
                        .server(request.server())
                        .price(request.price())
                        .note(request.note())
                        .build()
        );

        for (BundleCreateRequest bundleReq : request.bundles()) {
            processBundle(listing, bundleReq);
        }

        return listing.getId();
    }

    /**
     * 피스별 세트명·주술 마크 정보. 세트 제목 자동 생성에 사용된다.
     *
     * @param setName 소속 세트명 (세트에 속하지 않으면 null)
     * @param mark    적용된 첫 번째 주술 마크 스냅샷 (주술 없으면 null)
     */
    private record PieceInfo(String setName, String mark) {}

    /**
     * 번들 단위 처리 — 번들 저장 후 라인을 순서대로 처리한다.
     * EQUIPMENT_SET 번들이고 titleOverride가 없으면 세트 표기 제목을 자동 생성한다.
     */
    private void processBundle(TradeListing listing, BundleCreateRequest bundleReq) {
        // EQUIPMENT_SINGLE은 라인이 정확히 1개여야 한다
        if (bundleReq.bundleType() == BundleType.EQUIPMENT_SINGLE
                && bundleReq.lines().size() != 1) {
            throw new IllegalArgumentException(
                    "EQUIPMENT_SINGLE 번들은 라인이 정확히 1개여야 합니다. 현재 라인 수: "
                    + bundleReq.lines().size());
        }

        // 번들 내 sortOrder 중복 검사
        long distinctSortOrders = bundleReq.lines().stream()
                .map(BundleLineCreateRequest::sortOrder)
                .distinct()
                .count();
        if (distinctSortOrders != bundleReq.lines().size()) {
            throw new IllegalArgumentException("번들 내 라인의 sortOrder 값이 중복되어 있습니다.");
        }

        ListingBundle bundle = listingBundleRepository.save(
                ListingBundle.builder()
                        .listing(listing)
                        .bundleType(bundleReq.bundleType())
                        .titleOverride(bundleReq.titleOverride())
                        .build()
        );

        List<PieceInfo> pieceInfos = new ArrayList<>();
        for (BundleLineCreateRequest lineReq : bundleReq.lines()) {
            processLine(bundle, lineReq).ifPresent(pieceInfos::add);
        }

        // EQUIPMENT_SET이고 titleOverride가 없으면 세트 표기 제목 자동 생성
        if (bundleReq.bundleType() == BundleType.EQUIPMENT_SET
                && (bundleReq.titleOverride() == null || bundleReq.titleOverride().isBlank())
                && !pieceInfos.isEmpty()) {
            String setName = pieceInfos.get(0).setName();
            if (setName != null) {
                List<String> marks = pieceInfos.stream().map(PieceInfo::mark).toList();
                bundle.updateTitle(SetTitleGenerator.generate(setName, marks));
            }
        }
    }

    /**
     * 번들 라인 단위 처리.
     * 아이템 조회 → 라인 저장 → 장비이면 상세·주술까지 처리한다.
     *
     * @return 장비 라인인 경우 피스 정보(세트명·마크), 재료 라인이면 빈 Optional
     */
    private Optional<PieceInfo> processLine(ListingBundle bundle, BundleLineCreateRequest lineReq) {
        Item item = itemRepository.findById(lineReq.itemId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "존재하지 않는 아이템입니다. itemId=" + lineReq.itemId()));

        BundleLine line = bundleLineRepository.save(
                BundleLine.builder()
                        .bundle(bundle)
                        .item(item)
                        .quantity(lineReq.quantity())
                        .sortOrder(lineReq.sortOrder())
                        .build()
        );

        if (lineReq.isEquipment()) {
            // 장비는 수량이 1이어야 한다
            if (lineReq.quantity() > 1) {
                throw new IllegalArgumentException(
                        "장비 라인의 수량은 1이어야 합니다. itemId=" + lineReq.itemId());
            }
            PieceInfo pieceInfo = validateAndSaveEquipmentDetail(item, line, lineReq.equipmentDetail());
            return Optional.of(pieceInfo);
        } else {
            // 재료 아이템은 MATERIAL 타입이어야 한다
            if (item.getType() != ItemType.MATERIAL) {
                throw new IllegalArgumentException(
                        "장비 상세 정보가 없는 라인의 아이템은 재료 타입이어야 합니다. itemId=" + item.getId());
            }
            return Optional.empty();
        }
    }

    /**
     * 장비 상세 정보 검증 및 저장.
     * EquipmentItem 존재 확인, 외변 강화 수치 정책, 주술 일관성, 주술 적용 가능 여부를 검증한다.
     *
     * @return 피스 정보 — 세트명(세트 미소속이면 null)과 대표 주술 마크(주술 없으면 null)
     */
    private PieceInfo validateAndSaveEquipmentDetail(Item item, BundleLine line,
                                                      EquipmentDetailRequest detailReq) {
        if (item.getType() != ItemType.EQUIPMENT) {
            throw new IllegalArgumentException(
                    "장비 상세 정보가 있는 라인의 아이템은 장비 타입이어야 합니다. itemId=" + item.getId());
        }

        EquipmentItem equipmentItem = equipmentItemRepository.findWithItemByItemId(item.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "장비 아이템 상세가 존재하지 않습니다. itemId=" + item.getId()));

        // 외변(APPEARANCE) 장비 정책: 강화 수치는 반드시 5
        if (equipmentItem.getEquipmentKind() == EquipmentKind.APPEARANCE) {
            if (detailReq.enhanceLevel() == null || detailReq.enhanceLevel() != 5) {
                throw new IllegalArgumentException(
                        "외변 장비의 강화 수치는 5이어야 합니다. itemId=" + item.getId());
            }
        }

        // 주술 여부와 주술 목록 일관성 검사
        if (!detailReq.isRitualConsistent()) {
            throw new IllegalArgumentException(
                    "주술 적용(hasRitual=true) 시 주술 목록은 1개 이상이어야 합니다. itemId=" + item.getId());
        }

        bundleEquipmentDetailRepository.save(
                BundleEquipmentDetail.builder()
                        .bundleLine(line)
                        .equipmentItem(equipmentItem)
                        .equipmentKindSnapshot(equipmentItem.getEquipmentKind())
                        .enhanceLevel(detailReq.enhanceLevel())
                        .hasRitual(detailReq.hasRitual())
                        .build()
        );

        // 세트명 추출 (세트에 속하지 않으면 null)
        String setName = equipmentItem.getEquipmentSet() != null
                ? equipmentItem.getEquipmentSet().getName()
                : null;

        // 주술 저장 후 대표 마크 추출 (세트 제목 생성용)
        String mark = null;
        if (detailReq.hasRitual()) {
            List<String> marks = saveRituals(line, equipmentItem, detailReq.rituals());
            mark = marks.isEmpty() ? null : marks.get(0);
        }

        return new PieceInfo(setName, mark);
    }

    /**
     * 주술 결과 저장.
     * 각 주술 ID가 해당 장비에 적용 가능한지 먼저 검증한 후 저장한다.
     * 적용 가능 주술 목록을 한 번에 로드하여 N+1을 방지한다.
     *
     * @return 저장된 주술의 appliedMarkSnapshot 목록 (세트 제목 생성용)
     */
    private List<String> saveRituals(BundleLine line, EquipmentItem equipmentItem,
                                      List<RitualResultRequest> ritualRequests) {
        // 요청 내 중복 ritualId 검사
        long distinctRitualCount = ritualRequests.stream()
                .map(RitualResultRequest::ritualId)
                .distinct()
                .count();
        if (distinctRitualCount != ritualRequests.size()) {
            throw new IllegalArgumentException("주술 목록에 중복된 ritualId가 있습니다.");
        }

        // 해당 장비에 적용 가능한 주술 맵 구성 (ritualId → Ritual)
        Map<Long, Ritual> applicableRitualMap = ritualApplicabilityRepository
                .findByEquipmentItemIdWithRitual(equipmentItem.getItemId())
                .stream()
                .map(RitualApplicability::getRitual)
                .collect(Collectors.toMap(Ritual::getId, Function.identity()));

        List<BundleEquipmentRitual> ritualsToSave = new ArrayList<>();
        for (RitualResultRequest ritualReq : ritualRequests) {
            Ritual ritual = applicableRitualMap.get(ritualReq.ritualId());
            if (ritual == null) {
                throw new IllegalArgumentException(
                        "해당 장비에 적용 불가능한 주술입니다. ritualId=" + ritualReq.ritualId()
                        + ", equipmentItemId=" + equipmentItem.getItemId());
            }

            // 결과(SUCCESS/GREAT_SUCCESS)에 따라 마크 스냅샷 결정
            String appliedMark = (ritualReq.outcome() == RitualOutcome.GREAT_SUCCESS)
                    ? ritual.getGreatSuccessMark()
                    : ritual.getSuccessMark();

            ritualsToSave.add(BundleEquipmentRitual.builder()
                    .bundleLine(line)
                    .ritual(ritual)
                    .outcome(ritualReq.outcome())
                    .appliedMarkSnapshot(appliedMark)
                    .build());
        }
        bundleEquipmentRitualRepository.saveAll(ritualsToSave);
        return ritualsToSave.stream()
                .map(BundleEquipmentRitual::getAppliedMarkSnapshot)
                .toList();
    }

    // ── 목록 조회 ────────────────────────────────────────────────────────────

    /**
     * 동적 필터 조건으로 거래 등록글 목록 조회.
     * N+1 방지를 위해 번들을 IN 쿼리로 일괄 로드한다.
     *
     * @param cond 검색 조건
     * @return 등록글 요약 목록
     */
    public List<ListingSummaryResponse> getListings(ListingSearchCondition cond) {
        List<TradeListing> listings = listingQueryRepository.search(cond);
        if (listings.isEmpty()) return List.of();

        List<Long> listingIds = listings.stream().map(TradeListing::getId).toList();

        // 번들을 listingId별로 그룹화
        List<ListingBundle> allBundles = listingBundleRepository.findByListingIdIn(listingIds);
        Map<Long, List<ListingBundle>> bundlesByListingId = allBundles.stream()
                .collect(Collectors.groupingBy(b -> b.getListing().getId()));

        // 번들 라인을 bundleId별로 그룹화 (아이템명 표시용)
        List<Long> bundleIds = allBundles.stream().map(ListingBundle::getId).toList();
        Map<Long, List<BundleLine>> linesByBundleId = bundleIds.isEmpty()
                ? Map.of()
                : bundleLineRepository.findByBundleIdIn(bundleIds).stream()
                        .collect(Collectors.groupingBy(l -> l.getBundle().getId()));

        return listings.stream()
                .map(listing -> ListingSummaryResponse.from(
                        listing,
                        bundlesByListingId.getOrDefault(listing.getId(), List.of()),
                        linesByBundleId
                ))
                .toList();
    }

    // ── 상세 조회 ────────────────────────────────────────────────────────────

    /**
     * 거래 등록글 상세 조회.
     * 번들 → 라인 → 장비 상세 → 주술 계층을 N+1 없이 조립한다.
     *
     * @param listingId 등록글 ID
     * @return 등록글 상세 응답
     */
    public ListingDetailResponse getDetail(Long listingId) {
        TradeListing listing = tradeListingRepository.findActiveById(listingId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "존재하지 않는 등록글입니다. id=" + listingId));

        List<ListingBundle> bundles = listingBundleRepository
                .findByListingIdOrderByIdAsc(listingId);

        List<Long> bundleIds = bundles.stream().map(ListingBundle::getId).toList();

        // 라인 일괄 조회 및 번들 ID별 그룹화
        List<BundleLine> allLines = bundleLineRepository.findByBundleIdIn(bundleIds);
        Map<Long, List<BundleLine>> linesByBundleId = allLines.stream()
                .collect(Collectors.groupingBy(l -> l.getBundle().getId()));

        List<Long> lineIds = allLines.stream().map(BundleLine::getId).toList();

        // 장비 상세 일괄 조회 및 라인 ID별 맵 구성
        Map<Long, BundleEquipmentDetail> detailByLineId = bundleEquipmentDetailRepository
                .findByBundleLineIdIn(lineIds)
                .stream()
                .collect(Collectors.toMap(d -> d.getBundleLine().getId(), Function.identity()));

        // 주술 일괄 조회 및 라인 ID별 그룹화
        Map<Long, List<BundleEquipmentRitual>> ritualsByLineId = bundleEquipmentRitualRepository
                .findWithRitualByBundleLineIdIn(lineIds)
                .stream()
                .collect(Collectors.groupingBy(r -> r.getBundleLine().getId()));

        // 번들 조립
        List<BundleAssembly> bundleAssemblies = bundles.stream()
                .map(bundle -> {
                    List<BundleLine> lines = linesByBundleId.getOrDefault(bundle.getId(), List.of());
                    List<LineAssembly> lineAssemblies = lines.stream()
                            .map(line -> new LineAssembly(
                                    line,
                                    detailByLineId.get(line.getId()),
                                    ritualsByLineId.getOrDefault(line.getId(), List.of())
                            ))
                            .toList();
                    return new BundleAssembly(bundle, lineAssemblies);
                })
                .toList();

        return ListingDetailResponse.from(listing, bundleAssemblies);
    }

    // ── 수정 ────────────────────────────────────────────────────────────────

    /**
     * 거래 등록글 수정 (가격·메모).
     * ACTIVE 상태인 본인 등록글만 수정 가능하다.
     *
     * @param sellerId  요청자 사용자 ID
     * @param listingId 수정 대상 등록글 ID
     * @param request   수정 요청 DTO (price, note)
     */
    @Transactional
    public void updateListing(Long sellerId, Long listingId, ListingUpdateRequest request) {
        TradeListing listing = tradeListingRepository.findNotDeletedById(listingId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "존재하지 않는 등록글입니다. id=" + listingId));

        if (!listing.getSeller().getId().equals(sellerId)) {
            throw new IllegalArgumentException("본인의 등록글만 수정할 수 있습니다.");
        }
        if (listing.getStatus() != ListingStatus.ACTIVE) {
            throw new IllegalStateException(
                    "ACTIVE 상태의 등록글만 수정 가능합니다. status=" + listing.getStatus());
        }

        listing.updatePriceAndNote(request.price(), request.note());
    }

    // ── 관리자 숨김 ─────────────────────────────────────────────────────────

    /**
     * 관리자 등록글 숨김 처리.
     * 소프트 삭제된 등록글이 아니라면 hidden=true 로 전환한다.
     *
     * @param listingId 숨김 대상 등록글 ID
     */
    @Transactional
    public void hideListing(Long listingId) {
        TradeListing listing = tradeListingRepository.findNotDeletedById(listingId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "존재하지 않는 등록글입니다. id=" + listingId));
        listing.hide();
    }

    /**
     * 관리자 등록글 숨김 해제.
     * 소프트 삭제된 등록글이 아니라면 hidden=false 로 전환한다.
     *
     * @param listingId 숨김 해제 대상 등록글 ID
     */
    @Transactional
    public void unhideListing(Long listingId) {
        TradeListing listing = tradeListingRepository.findNotDeletedById(listingId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "존재하지 않는 등록글입니다. id=" + listingId));
        listing.unhide();
    }

    // ── 취소 ────────────────────────────────────────────────────────────────

    /**
     * 거래 등록글 취소 (판매자 소프트 삭제 + CANCELLED 상태 전환).
     * 본인 등록글이고, 아직 종료되지 않은 경우에만 취소 가능하다.
     *
     * @param sellerId  요청자 사용자 ID (JWT에서 추출)
     * @param listingId 취소 대상 등록글 ID
     */
    @Transactional
    public void cancelListing(Long sellerId, Long listingId) {
        // 취소 처리는 hidden 여부와 무관하게 소프트 삭제되지 않은 본인 등록글에 대해 수행한다
        TradeListing listing = tradeListingRepository.findNotDeletedById(listingId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "존재하지 않는 등록글입니다. id=" + listingId));

        if (!listing.getSeller().getId().equals(sellerId)) {
            throw new IllegalArgumentException("본인의 등록글만 취소할 수 있습니다.");
        }

        // 이미 종료된 상태(SOLD, CANCELLED)는 취소 불가
        if (listing.getStatus() == ListingStatus.SOLD
                || listing.getStatus() == ListingStatus.CANCELLED) {
            throw new IllegalStateException(
                    "이미 완료되거나 취소된 거래 등록글입니다. status=" + listing.getStatus());
        }

        listing.cancel();
        listing.softDelete();
    }
}
