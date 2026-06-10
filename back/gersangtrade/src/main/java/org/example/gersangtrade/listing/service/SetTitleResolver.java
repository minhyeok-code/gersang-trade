package org.example.gersangtrade.listing.service;

import org.example.gersangtrade.domain.catalog.EquipmentItem;
import org.example.gersangtrade.domain.catalog.enums.EquipmentSlot;
import org.example.gersangtrade.domain.listing.BundleEquipmentDetail;
import org.example.gersangtrade.domain.listing.BundleEquipmentRitual;
import org.example.gersangtrade.domain.listing.BundleLine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 저장된 번들 라인·주술 데이터로 EQUIPMENT_SET 표시 제목을 재계산한다.
 * {@code listing_bundles.title_override}에 구 포맷이 남아 있어도 조회 시 최신 규칙을 적용한다.
 */
public final class SetTitleResolver {

    private SetTitleResolver() {}

    /**
     * 번들 라인·장비 상세·주술 정보로 세트 제목을 재계산한다.
     *
     * @param lines            sortOrder 순으로 정렬 전 라인 목록
     * @param detailByLineId   라인 ID → 장비 상세
     * @param ritualsByLineId  라인 ID → 주술 결과 목록
     * @return 세트명을 확인할 수 있으면 제목, 아니면 empty
     */
    public static Optional<String> resolve(
            List<BundleLine> lines,
            Map<Long, BundleEquipmentDetail> detailByLineId,
            Map<Long, List<BundleEquipmentRitual>> ritualsByLineId) {

        if (lines.isEmpty()) {
            return Optional.empty();
        }

        List<BundleLine> sorted = lines.stream()
                .sorted(Comparator.comparingInt(BundleLine::getSortOrder))
                .toList();

        String setName = null;
        List<SetTitleGenerator.PieceTitleInput> pieceInputs = new ArrayList<>();

        for (BundleLine line : sorted) {
            BundleEquipmentDetail detail = detailByLineId.get(line.getId());
            if (detail == null) {
                continue;
            }

            EquipmentItem equipmentItem = detail.getEquipmentItem();
            if (equipmentItem.getEquipmentSet() != null && setName == null) {
                setName = equipmentItem.getEquipmentSet().getName();
            }

            String displayMark = null;
            if (detail.isHasRitual()) {
                List<BundleEquipmentRitual> rituals = ritualsByLineId.getOrDefault(line.getId(), List.of());
                if (!rituals.isEmpty()) {
                    BundleEquipmentRitual first = rituals.get(0);
                    displayMark = SetTitleGenerator.buildTitleMark(first.getRitual(), first.getOutcome());
                }
            }
            pieceInputs.add(new SetTitleGenerator.PieceTitleInput(equipmentItem.getSlot(), displayMark));
        }

        if (setName == null) {
            return Optional.empty();
        }
        return Optional.of(SetTitleGenerator.generate(setName, pieceInputs));
    }
}
