package org.example.gersangtrade.wanted.dto.response;

import org.example.gersangtrade.domain.wanted.WantedEquipmentCondition;
import org.example.gersangtrade.domain.wanted.WantedItem;
import org.example.gersangtrade.domain.wanted.WantedListing;
import org.example.gersangtrade.domain.wanted.WantedRitualCondition;
import org.example.gersangtrade.domain.wanted.enums.PreferredOutcome;
import org.example.gersangtrade.domain.wanted.enums.WantedStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 구매 희망 등록글 상세 조회용 응답 DTO.
 * 아이템 → 장비 조건 → 주술 조건 계층을 전부 담는다.
 */
public record WantedListingDetailResponse(
        Long id,
        String buyerName,
        String server,
        WantedStatus status,
        Long offeredPrice,
        String note,
        List<ItemDetail> items,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    /**
     * 구매 희망 아이템 상세.
     *
     * @param wantedItemId     아이템 행 ID
     * @param itemId           아이템 ID
     * @param itemName         아이템명
     * @param quantity         구매 희망 수량
     * @param sortOrder        표시 순서
     * @param equipmentCondition 장비 조건 (재료이면 null)
     */
    public record ItemDetail(
            Long wantedItemId,
            Long itemId,
            String itemName,
            Integer quantity,
            Integer sortOrder,
            EquipmentConditionDetail equipmentCondition
    ) {
        public static ItemDetail from(WantedItem wantedItem,
                                      WantedEquipmentCondition condition,
                                      List<WantedRitualCondition> rituals) {
            EquipmentConditionDetail eqDetail = null;
            if (condition != null) {
                List<RitualConditionDetail> ritualDetails = rituals.stream()
                        .map(RitualConditionDetail::from)
                        .toList();
                eqDetail = new EquipmentConditionDetail(
                        condition.getMinEnhanceLevel(),
                        condition.isHasRitual(),
                        ritualDetails
                );
            }
            return new ItemDetail(
                    wantedItem.getId(),
                    wantedItem.getItem().getId(),
                    wantedItem.getItem().getName(),
                    wantedItem.getQuantity(),
                    wantedItem.getSortOrder(),
                    eqDetail
            );
        }
    }

    /**
     * 장비 구매 조건 상세.
     *
     * @param minEnhanceLevel  최소 허용 강화 수치 (null이면 무관)
     * @param hasRitual        주술 여부 조건
     * @param ritualConditions 주술 조건 목록
     */
    public record EquipmentConditionDetail(
            Integer minEnhanceLevel,
            boolean hasRitual,
            List<RitualConditionDetail> ritualConditions
    ) {
    }

    /**
     * 주술 조건 상세.
     *
     * @param ritualId          주술 ID
     * @param ritualDisplayName 주술 명칭
     * @param preferredOutcome  허용 결과 (ANY | SUCCESS | GREAT_SUCCESS)
     */
    public record RitualConditionDetail(
            Long ritualId,
            String ritualDisplayName,
            PreferredOutcome preferredOutcome
    ) {
        public static RitualConditionDetail from(WantedRitualCondition condition) {
            return new RitualConditionDetail(
                    condition.getRitual().getId(),
                    condition.getRitual().getDisplayName(),
                    condition.getPreferredOutcome()
            );
        }
    }

    public static WantedListingDetailResponse from(WantedListing listing,
                                                    List<ItemAssembly> itemAssemblies) {
        List<ItemDetail> itemDetails = itemAssemblies.stream()
                .map(ItemAssembly::toDetail)
                .toList();
        return new WantedListingDetailResponse(
                listing.getId(),
                listing.getBuyer().getNickname(),
                listing.getServer(),
                listing.getStatus(),
                listing.getOfferedPrice(),
                listing.getNote(),
                itemDetails,
                listing.getCreatedAt(),
                listing.getUpdatedAt()
        );
    }

    /**
     * 서비스 레이어에서 아이템과 조건 데이터를 조합해 전달하기 위한 내부 조립 구조체.
     */
    public record ItemAssembly(
            WantedItem wantedItem,
            WantedEquipmentCondition condition,
            List<WantedRitualCondition> rituals
    ) {
        public ItemDetail toDetail() {
            return ItemDetail.from(wantedItem, condition, rituals);
        }
    }
}
