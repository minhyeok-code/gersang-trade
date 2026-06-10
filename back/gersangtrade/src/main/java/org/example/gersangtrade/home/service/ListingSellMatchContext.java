package org.example.gersangtrade.home.service;

import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.domain.listing.BundleEquipmentDetail;
import org.example.gersangtrade.domain.listing.BundleEquipmentRitual;
import org.example.gersangtrade.domain.listing.BundleLine;
import org.example.gersangtrade.domain.listing.ListingBundle;
import org.example.gersangtrade.domain.listing.enums.BundleType;
import org.example.gersangtrade.listing.repository.BundleEquipmentDetailRepository;
import org.example.gersangtrade.listing.repository.BundleEquipmentRitualRepository;
import org.example.gersangtrade.listing.repository.BundleLineRepository;
import org.example.gersangtrade.listing.repository.ListingBundleRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 판매 등록글 시세 2차 필터용 — 번들 라인·장비 상세·주술 배치 로드 컨텍스트.
 */
@Component
@RequiredArgsConstructor
public class ListingSellMatchContext {

    private final ListingBundleRepository listingBundleRepository;
    private final BundleLineRepository bundleLineRepository;
    private final BundleEquipmentDetailRepository bundleEquipmentDetailRepository;
    private final BundleEquipmentRitualRepository bundleEquipmentRitualRepository;

    public Loaded load(List<Long> listingIds) {
        if (listingIds == null || listingIds.isEmpty()) {
            return Loaded.empty();
        }

        List<ListingBundle> nonSetBundles = listingBundleRepository.findByListingIdIn(listingIds).stream()
                .filter(bundle -> bundle.getBundleType() != BundleType.EQUIPMENT_SET)
                .toList();

        if (nonSetBundles.isEmpty()) {
            return Loaded.empty();
        }

        Map<Long, Long> listingIdByBundleId = nonSetBundles.stream()
                .collect(Collectors.toMap(ListingBundle::getId, b -> b.getListing().getId()));

        List<Long> bundleIds = nonSetBundles.stream().map(ListingBundle::getId).toList();
        List<BundleLine> lines = bundleLineRepository.findByBundleIdIn(bundleIds);

        Map<Long, List<BundleLine>> linesByListingId = new LinkedHashMap<>();
        for (BundleLine line : lines) {
            Long listingId = listingIdByBundleId.get(line.getBundle().getId());
            if (listingId == null) {
                continue;
            }
            linesByListingId.computeIfAbsent(listingId, ignored -> new ArrayList<>()).add(line);
        }

        List<Long> lineIds = lines.stream().map(BundleLine::getId).toList();
        Map<Long, BundleEquipmentDetail> detailByLineId = lineIds.isEmpty()
                ? Map.of()
                : bundleEquipmentDetailRepository.findByBundleLineIdIn(lineIds).stream()
                .collect(Collectors.toMap(BundleEquipmentDetail::getBundleLineId, Function.identity()));

        Map<Long, List<BundleEquipmentRitual>> ritualsByLineId = lineIds.isEmpty()
                ? Map.of()
                : bundleEquipmentRitualRepository.findWithRitualByBundleLineIdIn(lineIds).stream()
                .collect(Collectors.groupingBy(r -> r.getBundleLine().getId()));

        return new Loaded(linesByListingId, detailByLineId, ritualsByLineId);
    }

    public record Loaded(
            Map<Long, List<BundleLine>> linesByListingId,
            Map<Long, BundleEquipmentDetail> detailByLineId,
            Map<Long, List<BundleEquipmentRitual>> ritualsByLineId
    ) {
        static Loaded empty() {
            return new Loaded(Map.of(), Map.of(), Map.of());
        }

        List<BundleLine> linesFor(Long listingId) {
            return linesByListingId.getOrDefault(listingId, List.of());
        }
    }
}
