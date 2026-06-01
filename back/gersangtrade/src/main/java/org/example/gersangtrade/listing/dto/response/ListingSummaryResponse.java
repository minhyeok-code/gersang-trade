package org.example.gersangtrade.listing.dto.response;

import org.example.gersangtrade.domain.listing.BundleLine;
import org.example.gersangtrade.domain.listing.ListingBundle;
import org.example.gersangtrade.domain.listing.TradeListing;
import org.example.gersangtrade.domain.listing.enums.BundleType;
import org.example.gersangtrade.domain.listing.enums.ListingStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 거래 등록글 목록 조회용 요약 응답 DTO.
 * 검색 결과 리스트에서 카드 형태로 표시되는 정보를 담는다.
 *
 * @param id          등록글 ID
 * @param sellerName  판매자 닉네임
 * @param server      거래 서버
 * @param status      등록글 상태
 * @param price       판매 희망 가격
 * @param bundles     번들 요약 목록 (제목, 유형)
 * @param createdAt   등록 일시
 */
public record ListingSummaryResponse(
        Long id,
        Long sellerId,
        String sellerName,
        String sellerGameNickname,
        String sellerGameAccessTime,
        String server,
        ListingStatus status,
        Long price,
        List<BundleSummary> bundles,
        LocalDateTime createdAt
) {
    /**
     * 번들 요약 정보.
     *
     * @param bundleType    번들 유형
     * @param displayTitle  표시 제목 (titleOverride가 있으면 우선, 없으면 시스템 자동 제목)
     */
    public record BundleSummary(
            BundleType bundleType,
            String displayTitle
    ) {
        public static BundleSummary from(ListingBundle bundle, List<BundleLine> lines) {
            String title;
            if (bundle.getTitleOverride() != null) {
                title = bundle.getTitleOverride();
            } else if (!lines.isEmpty()) {
                // titleOverride 없으면 첫 번째 아이템명으로 표시
                String firstName = lines.get(0).getItem().getName();
                title = lines.size() > 1 ? firstName + " 외 " + (lines.size() - 1) + "개" : firstName;
            } else {
                title = bundle.getBundleType().name();
            }
            return new BundleSummary(bundle.getBundleType(), title);
        }
    }

    public static ListingSummaryResponse from(TradeListing listing, List<ListingBundle> bundles,
                                              Map<Long, List<BundleLine>> linesByBundleId) {
        return new ListingSummaryResponse(
                listing.getId(),
                listing.getSeller().getId(),
                listing.getSeller().getNickname(),
                listing.getSeller().getGameNickname(),
                listing.getSeller().getGameAccessTime(),
                listing.getServer(),
                listing.getStatus(),
                listing.getPrice(),
                bundles.stream()
                        .map(b -> BundleSummary.from(b, linesByBundleId.getOrDefault(b.getId(), List.of())))
                        .toList(),
                listing.getCreatedAt()
        );
    }
}
