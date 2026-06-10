package org.example.gersangtrade.listing.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.domain.listing.TradeListing;
import org.example.gersangtrade.domain.listing.enums.BundleType;
import org.example.gersangtrade.domain.listing.enums.ListingStatus;
import org.example.gersangtrade.listing.dto.request.ListingSearchCondition;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.example.gersangtrade.domain.catalog.QEquipmentItem.equipmentItem;
import static org.example.gersangtrade.domain.listing.QBundleLine.bundleLine;
import static org.example.gersangtrade.domain.listing.QListingBundle.listingBundle;
import static org.example.gersangtrade.domain.listing.QTradeListing.tradeListing;

/**
 * 거래 등록글 동적 검색 QueryDSL 레포지토리.
 * 서버, 상태, 번들 유형, 아이템명 키워드를 조합한 필터 검색을 지원한다.
 * null인 조건은 자동으로 제외된다.
 */
@Repository
@RequiredArgsConstructor
public class ListingQueryRepository {

    private final JPAQueryFactory queryFactory;

    /**
     * 다중 조건 필터 기반 거래 등록글 목록 조회 (페이징 포함).
     * 소프트 삭제(deletedAt IS NULL), 관리자 숨김(hidden=false) 조건은 항상 적용한다.
     * 아이템명 키워드 조건이 있으면 번들 라인까지 JOIN해서 검색한다.
     *
     * @param cond 검색 조건 (page/size 포함)
     * @return 조건에 맞는 등록글 목록 (DB 수준에서 offset/limit 적용)
     */
    public List<TradeListing> search(ListingSearchCondition cond) {
        var query = queryFactory
                .selectDistinct(tradeListing)
                .from(tradeListing)
                .leftJoin(tradeListing.seller).fetchJoin();

        // 번들 유형·아이템ID·아이템명 키워드 검색 시 bundle JOIN 필요
        if (cond.bundleType() != null || cond.itemId() != null || StringUtils.hasText(cond.keyword())) {
            query = query.leftJoin(listingBundle).on(listingBundle.listing.eq(tradeListing));
        }
        // 아이템ID 필터 또는 아이템명 키워드 검색 시 line JOIN 추가 필요
        if (cond.itemId() != null || StringUtils.hasText(cond.keyword())) {
            query = query.leftJoin(bundleLine).on(bundleLine.bundle.eq(listingBundle));
        }

        return query.where(
                isNotDeleted(),
                isNotHidden(),
                serverEq(cond.server()),
                statusEq(cond.status()),
                bundleTypeEq(cond.bundleType()),
                itemIdEq(cond.itemId()),
                itemNameContains(cond.keyword())
        )
        .orderBy(tradeListing.createdAt.desc())
        .offset((long) cond.resolvedPage() * cond.resolvedSize())
        .limit(cond.resolvedSize())
        .fetch();
    }

    /**
     * 동일 조건의 총 건수 조회 (페이징 totalElements 계산용).
     * keyword 조건이 있으면 search와 동일하게 bundle/line JOIN을 추가한다.
     * fetch().size() 대신 count 서브쿼리로 처리하여 불필요한 데이터 로딩을 방지한다.
     */
    public long count(ListingSearchCondition cond) {
        // count 쿼리: id만 집계하여 DB에서 직접 건수 반환
        var query = queryFactory
                .select(tradeListing.id.countDistinct())
                .from(tradeListing);

        if (cond.bundleType() != null || cond.itemId() != null || StringUtils.hasText(cond.keyword())) {
            query = query.leftJoin(listingBundle).on(listingBundle.listing.eq(tradeListing));
        }
        if (cond.itemId() != null || StringUtils.hasText(cond.keyword())) {
            query = query.leftJoin(bundleLine).on(bundleLine.bundle.eq(listingBundle));
        }

        Long result = query.where(
                isNotDeleted(),
                isNotHidden(),
                serverEq(cond.server()),
                statusEq(cond.status()),
                bundleTypeEq(cond.bundleType()),
                itemIdEq(cond.itemId()),
                itemNameContains(cond.keyword())
        ).fetchOne();
        return result != null ? result : 0L;
    }

    /**
     * 관심 SET 시세 조회용 — 특정 setId의 EQUIPMENT_SET 번들 포함 판매 등록글 후보 조회.
     * 2차 필터(SetWatchMatcher)에서 composition·ritual 매칭을 위해 넉넉히 fetch한다.
     */
    public List<TradeListing> searchLatestBySetId(
            String server, ListingStatus status, Long setId, int limit) {
        return queryFactory
                .selectDistinct(tradeListing)
                .from(tradeListing)
                .leftJoin(tradeListing.seller).fetchJoin()
                .leftJoin(listingBundle).on(listingBundle.listing.eq(tradeListing))
                .leftJoin(bundleLine).on(bundleLine.bundle.eq(listingBundle))
                .leftJoin(equipmentItem).on(equipmentItem.itemId.eq(bundleLine.item.id))
                .where(
                        isNotDeleted(),
                        isNotHidden(),
                        serverEq(server),
                        statusEq(status),
                        listingBundle.bundleType.eq(BundleType.EQUIPMENT_SET),
                        // 번들 FK 또는 라인 장비의 set_id — 기존 데이터( FK 미저장) 호환
                        listingBundle.equipmentSet.id.eq(setId)
                                .or(equipmentItem.equipmentSet.id.eq(setId))
                )
                .orderBy(tradeListing.createdAt.desc())
                .limit(limit)
                .fetch();
    }

    /**
     * 관심 아이템 시세 조회용 배치 메서드 — 판매 등록글 대상.
     * 주어진 itemId 목록에 대해 서버·상태 조건으로 최신 판매글을 limitPerItem개씩 반환한다.
     * 관심 아이템 수가 최대 5개이므로 itemId별 개별 쿼리로 처리한다.
     */
    public Map<Long, List<TradeListing>> searchLatestPerItemIds(
            String server, ListingStatus status, List<Long> itemIds, int limitPerItem) {
        if (itemIds == null || itemIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, List<TradeListing>> result = new LinkedHashMap<>();
        for (Long itemId : itemIds) {
            List<TradeListing> listings = queryFactory
                    .selectDistinct(tradeListing)
                    .from(tradeListing)
                    .leftJoin(tradeListing.seller).fetchJoin()
                    .leftJoin(listingBundle).on(listingBundle.listing.eq(tradeListing))
                    .leftJoin(bundleLine).on(bundleLine.bundle.eq(listingBundle))
                    .where(
                            isNotDeleted(),
                            isNotHidden(),
                            serverEq(server),
                            statusEq(status),
                            // 세트 번들 제외 — 단품·재료 시세에 세트 가격 섞임 방지
                            listingBundle.bundleType.ne(BundleType.EQUIPMENT_SET),
                            bundleLine.item.id.eq(itemId)
                    )
                    .orderBy(tradeListing.createdAt.desc())
                    .limit(limitPerItem)
                    .fetch();
            result.put(itemId, listings);
        }
        return result;
    }

    /** 소프트 삭제되지 않은 항목만 조회 */
    private BooleanExpression isNotDeleted() {
        return tradeListing.deletedAt.isNull();
    }

    /** 관리자 숨김 처리되지 않은 항목만 조회 */
    private BooleanExpression isNotHidden() {
        return tradeListing.hidden.eq(false);
    }

    /** 서버 필터 — null이면 조건 제외 */
    private BooleanExpression serverEq(String server) {
        return StringUtils.hasText(server) ? tradeListing.server.eq(server) : null;
    }

    /** 상태 필터 — null이면 조건 제외 */
    private BooleanExpression statusEq(ListingStatus status) {
        return status != null ? tradeListing.status.eq(status) : null;
    }

    /** 번들 유형 필터 — null이면 조건 제외 */
    private BooleanExpression bundleTypeEq(BundleType bundleType) {
        return bundleType != null ? listingBundle.bundleType.eq(bundleType) : null;
    }

    /** 번들 라인의 아이템 ID 필터 — null이면 조건 제외 */
    private BooleanExpression itemIdEq(Long itemId) {
        return itemId != null ? bundleLine.item.id.eq(itemId) : null;
    }

    /** 번들 라인의 아이템명 contains 검색 — null이면 조건 제외 */
    private BooleanExpression itemNameContains(String keyword) {
        return StringUtils.hasText(keyword)
                ? bundleLine.item.name.containsIgnoreCase(keyword)
                : null;
    }
}
