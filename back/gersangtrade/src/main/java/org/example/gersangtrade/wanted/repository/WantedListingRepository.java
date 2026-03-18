package org.example.gersangtrade.wanted.repository;

import org.example.gersangtrade.domain.wanted.WantedListing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 구매 희망 등록글 기본 CRUD 레포지토리.
 */
public interface WantedListingRepository extends JpaRepository<WantedListing, Long> {

    /**
     * 소프트 삭제되지 않고 관리자 숨김 처리되지 않은 등록글 단건 조회.
     * hidden=true 인 경우 일반 사용자에게 노출되지 않아야 한다.
     */
    @Query("SELECT wl FROM WantedListing wl " +
           "JOIN FETCH wl.buyer " +
           "WHERE wl.id = :id AND wl.deletedAt IS NULL AND wl.hidden = false")
    Optional<WantedListing> findActiveById(@Param("id") Long id);

    /**
     * 특정 구매자의 활성 등록글 목록 조회.
     */
    @Query("SELECT wl FROM WantedListing wl " +
           "WHERE wl.buyer.id = :buyerId " +
           "AND wl.deletedAt IS NULL " +
           "ORDER BY wl.createdAt DESC")
    List<WantedListing> findActivesByBuyerId(@Param("buyerId") Long buyerId);

    /**
     * 소유자 확인 및 취소 처리 전용 단건 조회.
     * hidden 여부와 무관하게 소프트 삭제되지 않은 등록글을 조회한다.
     * 취소(cancelWantedListing)에서 사용하며, 본인 소유 확인 후 처리한다.
     */
    @Query("SELECT wl FROM WantedListing wl " +
           "JOIN FETCH wl.buyer " +
           "WHERE wl.id = :id AND wl.deletedAt IS NULL")
    Optional<WantedListing> findNotDeletedById(@Param("id") Long id);
}
