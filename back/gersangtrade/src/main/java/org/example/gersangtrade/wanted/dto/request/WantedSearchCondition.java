package org.example.gersangtrade.wanted.dto.request;

import org.example.gersangtrade.domain.wanted.enums.WantedStatus;

/**
 * 구매 희망 등록글 목록 조회 조건 DTO.
 * 모든 필드는 선택적이며, null인 조건은 QueryDSL에서 자동으로 제외된다.
 *
 * @param server   서버 필터 (null이면 전체)
 * @param status   상태 필터 (null이면 서비스에서 OPEN 기본 적용)
 * @param keyword  아이템명 키워드 검색 (null이면 전체)
 * @param page     페이지 번호 (0부터 시작, 기본 0)
 * @param size     페이지당 결과 수 (기본 20, 최대 50)
 */
public record WantedSearchCondition(
        String server,
        WantedStatus status,
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
