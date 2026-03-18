package org.example.gersangtrade.listing.repository;

import org.example.gersangtrade.domain.listing.ListingBundle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 번들 기본 CRUD 레포지토리.
 * 단순 메서드 쿼리만 사용하는 레포지토리다.
 */
public interface ListingBundleRepository extends JpaRepository<ListingBundle, Long> {

    /**
     * 특정 등록글에 속한 번들 목록 조회.
     * 등록글 상세 조회 및 목록 조회 시 번들 구성을 가져올 때 사용한다.
     */
    List<ListingBundle> findByListingIdOrderByIdAsc(Long listingId);

    /**
     * 여러 등록글 ID에 속한 번들 목록 일괄 조회.
     * 목록 화면에서 N+1 방지를 위해 IN 쿼리로 미리 로드한다.
     */
    List<ListingBundle> findByListingIdIn(List<Long> listingIds);
}
