package org.example.gersangtrade.listing.dto.request;

import org.example.gersangtrade.domain.listing.enums.BundleType;
import org.example.gersangtrade.domain.listing.enums.ListingStatus;

/**
 * 거래 등록글 목록 조회 조건 DTO.
 * 모든 필드는 선택적이며, null인 조건은 QueryDSL에서 자동으로 제외된다.
 *
 * @param server     서버 필터 (null이면 전체)
 * @param status     등록글 상태 필터 (null이면 ACTIVE만 기본 조회는 서비스에서 처리)
 * @param bundleType 번들 유형 필터 (null이면 전체)
 * @param keyword    아이템명 키워드 검색 (null이면 전체)
 * @param page       페이지 번호 (0부터 시작, 기본 0)
 * @param size       페이지당 결과 수 (기본 20, 최대 50)
 */
public record ListingSearchCondition(
        String server,
        ListingStatus status,
        BundleType bundleType,
        String keyword,
        Integer page,
        Integer size
) {
    /** 기본값이 적용된 page 반환 */
    public int resolvedPage() {
        return (page != null && page >= 0) ? page : 0;
    }

    /** 기본값·상한이 적용된 size 반환 */
    public int resolvedSize() {
        if (size == null || size <= 0) return 20;
        return Math.min(size, 50);
    }
}
