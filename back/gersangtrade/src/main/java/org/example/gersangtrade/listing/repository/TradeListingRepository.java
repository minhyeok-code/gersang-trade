package org.example.gersangtrade.listing.repository;

import org.example.gersangtrade.domain.listing.TradeListing;
import org.example.gersangtrade.domain.listing.enums.ListingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 거래 등록글 기본 CRUD 레포지토리.
 * 소프트 삭제(deletedAt IS NULL) 조건은 조회 메서드마다 명시적으로 적용한다.
 */
public interface TradeListingRepository extends JpaRepository<TradeListing, Long> {

    /**
     * 소프트 삭제되지 않고 관리자 숨김 처리되지 않은 등록글 단건 조회.
     * 상세 페이지 진입 시 사용한다.
     * hidden=true 인 경우 일반 사용자에게 노출되지 않아야 한다.
     */
    @Query("SELECT tl FROM TradeListing tl " +
           "JOIN FETCH tl.seller " +
           "WHERE tl.id = :id AND tl.deletedAt IS NULL AND tl.hidden = false")
    Optional<TradeListing> findActiveById(@Param("id") Long id);

    /**
     * 특정 판매자의 활성 등록글 목록 조회.
     * 마이페이지 내 등록글 관리에서 사용한다.
     */
    @Query("SELECT tl FROM TradeListing tl " +
           "WHERE tl.seller.id = :sellerId " +
           "AND tl.deletedAt IS NULL " +
           "ORDER BY tl.createdAt DESC")
    List<TradeListing> findActivesBySellerId(@Param("sellerId") Long sellerId);

    /**
     * 특정 상태의 등록글 목록 조회 (관리자용).
     * hidden=true 포함 전체를 조회한다.
     */
    List<TradeListing> findByStatusAndDeletedAtIsNull(ListingStatus status);

    /**
     * 소유자 확인 및 취소 처리 전용 단건 조회.
     * hidden 여부와 무관하게 소프트 삭제되지 않은 등록글을 조회한다.
     * 판매자 취소(cancelListing)에서 사용하며, 본인 소유 확인 후 처리한다.
     */
    @Query("SELECT tl FROM TradeListing tl " +
           "JOIN FETCH tl.seller " +
           "WHERE tl.id = :id AND tl.deletedAt IS NULL")
    Optional<TradeListing> findNotDeletedById(@Param("id") Long id);
}
