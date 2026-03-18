package org.example.gersangtrade.listing.dto.response;

import org.example.gersangtrade.domain.listing.BundleEquipmentDetail;
import org.example.gersangtrade.domain.listing.BundleEquipmentRitual;
import org.example.gersangtrade.domain.listing.BundleLine;
import org.example.gersangtrade.domain.listing.ListingBundle;
import org.example.gersangtrade.domain.listing.TradeListing;
import org.example.gersangtrade.domain.listing.enums.BundleType;
import org.example.gersangtrade.domain.listing.enums.ListingStatus;
import org.example.gersangtrade.domain.listing.enums.RitualOutcome;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 거래 등록글 상세 조회용 응답 DTO.
 * 번들 → 라인 → 장비 상세 → 주술 정보까지 전체 계층을 담는다.
 */
public record ListingDetailResponse(
        Long id,
        String sellerName,
        String server,
        ListingStatus status,
        Long price,
        String note,
        List<BundleDetail> bundles,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    /**
     * 번들 상세 정보.
     *
     * @param bundleType   번들 유형
     * @param displayTitle 표시 제목
     * @param lines        번들 구성 라인 목록
     */
    public record BundleDetail(
            Long bundleId,
            BundleType bundleType,
            String displayTitle,
            List<LineDetail> lines
    ) {
    }

    /**
     * 번들 라인 상세 정보.
     *
     * @param lineId        라인 ID
     * @param itemId        아이템 ID
     * @param itemName      아이템명
     * @param quantity      수량
     * @param sortOrder     표시 순서
     * @param equipmentInfo 장비 정보 (재료이면 null)
     */
    public record LineDetail(
            Long lineId,
            Long itemId,
            String itemName,
            Integer quantity,
            Integer sortOrder,
            EquipmentInfo equipmentInfo
    ) {
        public static LineDetail from(BundleLine line, BundleEquipmentDetail detail,
                                      List<BundleEquipmentRitual> rituals) {
            EquipmentInfo eqInfo = null;
            if (detail != null) {
                List<RitualInfo> ritualInfos = rituals.stream()
                        .map(RitualInfo::from)
                        .toList();
                eqInfo = new EquipmentInfo(
                        detail.getEquipmentKindSnapshot().name(),
                        detail.getEnhanceLevel(),
                        detail.isHasRitual(),
                        ritualInfos
                );
            }
            return new LineDetail(
                    line.getId(),
                    line.getItem().getId(),
                    line.getItem().getName(),
                    line.getQuantity(),
                    line.getSortOrder(),
                    eqInfo
            );
        }
    }

    /**
     * 장비 상세 정보.
     *
     * @param equipmentKind 장비 종류 스냅샷 (APPEARANCE | NORMAL)
     * @param enhanceLevel  강화 수치 (null 허용)
     * @param hasRitual     주술 적용 여부
     * @param rituals       적용된 주술 목록
     */
    public record EquipmentInfo(
            String equipmentKind,
            Integer enhanceLevel,
            boolean hasRitual,
            List<RitualInfo> rituals
    ) {
    }

    /**
     * 주술 결과 정보.
     *
     * @param ritualId          주술 ID
     * @param ritualDisplayName 주술 명칭
     * @param outcome           주술 결과 (SUCCESS | GREAT_SUCCESS)
     * @param appliedMark       적용된 마크 스냅샷 (예: "00", "**")
     */
    public record RitualInfo(
            Long ritualId,
            String ritualDisplayName,
            RitualOutcome outcome,
            String appliedMark
    ) {
        public static RitualInfo from(BundleEquipmentRitual ritual) {
            return new RitualInfo(
                    ritual.getRitual().getId(),
                    ritual.getRitual().getDisplayName(),
                    ritual.getOutcome(),
                    ritual.getAppliedMarkSnapshot()
            );
        }
    }

    public static ListingDetailResponse from(TradeListing listing,
                                              List<BundleAssembly> bundleAssemblies) {
        List<BundleDetail> bundleDetails = bundleAssemblies.stream()
                .map(BundleAssembly::toDetail)
                .toList();
        return new ListingDetailResponse(
                listing.getId(),
                listing.getSeller().getNickname(),
                listing.getServer(),
                listing.getStatus(),
                listing.getPrice(),
                listing.getNote(),
                bundleDetails,
                listing.getCreatedAt(),
                listing.getUpdatedAt()
        );
    }

    /**
     * 서비스 레이어에서 번들과 라인 데이터를 조합해 전달하기 위한 내부 조립 구조체.
     */
    public record BundleAssembly(
            ListingBundle bundle,
            List<LineAssembly> lines
    ) {
        public BundleDetail toDetail() {
            String title = bundle.getTitleOverride() != null
                    ? bundle.getTitleOverride()
                    : bundle.getBundleType().name();
            return new BundleDetail(
                    bundle.getId(),
                    bundle.getBundleType(),
                    title,
                    lines.stream().map(LineAssembly::toDetail).toList()
            );
        }
    }

    /**
     * 서비스 레이어에서 라인과 장비/주술 데이터를 조합해 전달하기 위한 내부 조립 구조체.
     */
    public record LineAssembly(
            BundleLine line,
            BundleEquipmentDetail detail,
            List<BundleEquipmentRitual> rituals
    ) {
        public LineDetail toDetail() {
            return LineDetail.from(line, detail, rituals);
        }
    }
}
