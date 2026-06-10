package org.example.gersangtrade.listing.service;

import org.example.gersangtrade.domain.listing.BundleEquipmentDetail;
import org.example.gersangtrade.domain.listing.BundleEquipmentRitual;
import org.example.gersangtrade.domain.listing.BundleLine;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * EQUIPMENT_SINGLE 번들의 표시 제목을 재계산한다.
 * 주술이 있으면 {@code <마크>아이템명} 형식으로 반환한다.
 */
public final class SingleItemTitleResolver {

    private SingleItemTitleResolver() {}

    public static Optional<String> resolve(
            List<BundleLine> lines,
            Map<Long, BundleEquipmentDetail> detailByLineId,
            Map<Long, List<BundleEquipmentRitual>> ritualsByLineId) {

        if (lines.size() != 1) {
            return Optional.empty();
        }

        BundleLine line = lines.get(0);
        BundleEquipmentDetail detail = detailByLineId.get(line.getId());
        if (detail == null || !detail.isHasRitual()) {
            return Optional.empty();
        }

        List<BundleEquipmentRitual> rituals = ritualsByLineId.getOrDefault(line.getId(), List.of());
        if (rituals.isEmpty()) {
            return Optional.empty();
        }

        BundleEquipmentRitual first = rituals.get(0);
        String mark = SetTitleGenerator.buildTitleMark(first.getRitual(), first.getOutcome());
        return Optional.of(mark + line.getItem().getName());
    }
}
