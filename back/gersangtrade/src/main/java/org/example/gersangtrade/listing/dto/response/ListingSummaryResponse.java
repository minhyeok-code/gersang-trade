package org.example.gersangtrade.listing.dto.response;

import org.example.gersangtrade.domain.listing.ListingBundle;
import org.example.gersangtrade.domain.listing.TradeListing;
import org.example.gersangtrade.domain.listing.enums.BundleType;
import org.example.gersangtrade.domain.listing.enums.ListingStatus;

import java.time.LocalDateTime;
import java.util.List;

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
        String sellerName,
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
        public static BundleSummary from(ListingBundle bundle) {
            // titleOverride가 있으면 판매자 직접 입력 제목, 없으면 유형명으로 임시 표시
            String title = bundle.getTitleOverride() != null
                    ? bundle.getTitleOverride()
                    : bundle.getBundleType().name();
            return new BundleSummary(bundle.getBundleType(), title);
        }
    }

    public static ListingSummaryResponse from(TradeListing listing, List<ListingBundle> bundles) {
        return new ListingSummaryResponse(
                listing.getId(),
                listing.getSeller().getNickname(),
                listing.getServer(),
                listing.getStatus(),
                listing.getPrice(),
                bundles.stream().map(BundleSummary::from).toList(),
                listing.getCreatedAt()
        );
    }
}
