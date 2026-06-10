package org.example.gersangtrade.listing.service;

import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.domain.listing.BundleEquipmentDetail;
import org.example.gersangtrade.domain.listing.BundleEquipmentRitual;
import org.example.gersangtrade.domain.listing.BundleLine;
import org.example.gersangtrade.domain.listing.ListingBundle;
import org.example.gersangtrade.domain.listing.enums.BundleType;
import org.example.gersangtrade.listing.dto.response.ListingDetailResponse.LineAssembly;
import org.example.gersangtrade.listing.dto.response.ListingSummaryResponse.BundleSummary;
import org.example.gersangtrade.listing.repository.BundleEquipmentDetailRepository;
import org.example.gersangtrade.listing.repository.BundleEquipmentRitualRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 번들 표시 제목 조립 서비스.
 * EQUIPMENT_SET은 DB에 저장된 title_override 대신 라인·주술 데이터로 제목을 재계산한다.
 */
@Service
@RequiredArgsConstructor
public class ListingBundleTitleService {

    private final BundleEquipmentDetailRepository bundleEquipmentDetailRepository;
    private final BundleEquipmentRitualRepository bundleEquipmentRitualRepository;

    /**
     * 번들 목록에 대한 요약 제목을 생성한다.
     */
    public List<BundleSummary> buildSummaries(
            List<ListingBundle> bundles,
            Map<Long, List<BundleLine>> linesByBundleId) {

        List<Long> lineIds = linesByBundleId.values().stream()
                .flatMap(List::stream)
                .map(BundleLine::getId)
                .toList();

        Map<Long, BundleEquipmentDetail> detailByLineId = loadDetailsByLineId(lineIds);
        Map<Long, List<BundleEquipmentRitual>> ritualsByLineId = loadRitualsByLineId(lineIds);

        return bundles.stream()
                .map(bundle -> buildSummary(
                        bundle,
                        linesByBundleId.getOrDefault(bundle.getId(), List.of()),
                        detailByLineId,
                        ritualsByLineId))
                .toList();
    }

    /**
     * 상세 조회용 번들 표시 제목을 생성한다.
     */
    public String resolveDetailTitle(ListingBundle bundle, List<LineAssembly> lineAssemblies) {
        List<BundleLine> lines = lineAssemblies.stream().map(LineAssembly::line).toList();
        Map<Long, BundleEquipmentDetail> detailByLineId = lineAssemblies.stream()
                .filter(assembly -> assembly.detail() != null)
                .collect(Collectors.toMap(
                        assembly -> assembly.line().getId(),
                        LineAssembly::detail));
        Map<Long, List<BundleEquipmentRitual>> ritualsByLineId = lineAssemblies.stream()
                .collect(Collectors.toMap(
                        assembly -> assembly.line().getId(),
                        LineAssembly::rituals));
        return resolveTitle(bundle, lines, detailByLineId, ritualsByLineId);
    }

    /**
     * 번들·라인 기준 표시 제목을 생성한다. (거래 내역 등 단건 조회용)
     */
    public String resolveTitle(ListingBundle bundle, List<BundleLine> lines) {
        List<Long> lineIds = lines.stream().map(BundleLine::getId).toList();
        return resolveTitle(
                bundle,
                lines,
                loadDetailsByLineId(lineIds),
                loadRitualsByLineId(lineIds));
    }

    private String resolveTitle(
            ListingBundle bundle,
            List<BundleLine> lines,
            Map<Long, BundleEquipmentDetail> detailByLineId,
            Map<Long, List<BundleEquipmentRitual>> ritualsByLineId) {

        if (bundle.getBundleType() == BundleType.EQUIPMENT_SET) {
            return SetTitleResolver.resolve(lines, detailByLineId, ritualsByLineId)
                    .orElseGet(() -> fallbackTitle(bundle, lines));
        }
        if (bundle.getBundleType() == BundleType.EQUIPMENT_SINGLE) {
            return SingleItemTitleResolver.resolve(lines, detailByLineId, ritualsByLineId)
                    .orElseGet(() -> singleItemFallback(lines));
        }
        return fallbackTitle(bundle, lines);
    }

    private BundleSummary buildSummary(
            ListingBundle bundle,
            List<BundleLine> lines,
            Map<Long, BundleEquipmentDetail> detailByLineId,
            Map<Long, List<BundleEquipmentRitual>> ritualsByLineId) {

        if (bundle.getBundleType() == BundleType.EQUIPMENT_SET) {
            return SetTitleResolver.resolve(lines, detailByLineId, ritualsByLineId)
                    .map(title -> new BundleSummary(bundle.getBundleType(), title))
                    .orElseGet(() -> BundleSummary.from(bundle, lines));
        }
        if (bundle.getBundleType() == BundleType.EQUIPMENT_SINGLE) {
            return SingleItemTitleResolver.resolve(lines, detailByLineId, ritualsByLineId)
                    .map(title -> new BundleSummary(bundle.getBundleType(), title))
                    .orElseGet(() -> new BundleSummary(bundle.getBundleType(), singleItemFallback(lines)));
        }
        return BundleSummary.from(bundle, lines);
    }

    /** 단품 번들 — titleOverride 무시, 아이템명만 폴백 */
    private String singleItemFallback(List<BundleLine> lines) {
        if (!lines.isEmpty()) {
            return lines.get(0).getItem().getName();
        }
        return BundleType.EQUIPMENT_SINGLE.name();
    }

    private String fallbackTitle(ListingBundle bundle, List<BundleLine> lines) {
        if (bundle.getTitleOverride() != null && !bundle.getTitleOverride().isBlank()) {
            return bundle.getTitleOverride();
        }
        if (!lines.isEmpty()) {
            String firstName = lines.get(0).getItem().getName();
            return lines.size() > 1 ? firstName + " 외 " + (lines.size() - 1) + "개" : firstName;
        }
        return bundle.getBundleType().name();
    }

    private Map<Long, BundleEquipmentDetail> loadDetailsByLineId(List<Long> lineIds) {
        if (lineIds.isEmpty()) {
            return Map.of();
        }
        return bundleEquipmentDetailRepository.findWithEquipmentSetByBundleLineIdIn(lineIds)
                .stream()
                .collect(Collectors.toMap(BundleEquipmentDetail::getBundleLineId, Function.identity()));
    }

    private Map<Long, List<BundleEquipmentRitual>> loadRitualsByLineId(List<Long> lineIds) {
        if (lineIds.isEmpty()) {
            return Map.of();
        }
        return bundleEquipmentRitualRepository.findWithRitualByBundleLineIdIn(lineIds)
                .stream()
                .collect(Collectors.groupingBy(r -> r.getBundleLine().getId()));
    }
}
