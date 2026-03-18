package org.example.gersangtrade.wanted.repository;

import org.example.gersangtrade.domain.wanted.WantedItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 구매 희망 아이템 기본 CRUD 레포지토리.
 */
public interface WantedItemRepository extends JpaRepository<WantedItem, Long> {

    /**
     * 특정 등록글에 속한 아이템 목록을 표시 순서 기준으로 조회.
     */
    List<WantedItem> findByWantedListingIdOrderBySortOrderAsc(Long wantedListingId);

    /**
     * 여러 등록글 ID에 속한 아이템 목록 일괄 조회 (N+1 방지).
     */
    List<WantedItem> findByWantedListingIdIn(List<Long> wantedListingIds);
}
