package org.example.gersangtrade.listing.repository;

import org.example.gersangtrade.domain.listing.BundleLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 번들 라인 기본 CRUD 레포지토리.
 * 단순 메서드 쿼리만 사용하는 레포지토리다.
 */
public interface BundleLineRepository extends JpaRepository<BundleLine, Long> {

    /**
     * 특정 번들에 속한 라인 목록을 표시 순서(sortOrder) 기준으로 조회.
     * 번들 상세 조회 시 사용한다.
     */
    List<BundleLine> findByBundleIdOrderBySortOrderAsc(Long bundleId);

    /**
     * 여러 번들 ID에 속한 라인 목록 일괄 조회.
     * 등록글 상세 조회에서 N+1 방지를 위해 IN 쿼리로 미리 로드한다.
     */
    List<BundleLine> findByBundleIdIn(List<Long> bundleIds);
}
