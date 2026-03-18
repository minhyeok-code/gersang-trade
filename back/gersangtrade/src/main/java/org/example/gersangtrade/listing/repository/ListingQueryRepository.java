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

import java.util.List;

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

        // 번들 유형 필터 또는 아이템명 키워드 검색 시 bundle JOIN 필요
        if (cond.bundleType() != null || StringUtils.hasText(cond.keyword())) {
            query = query.leftJoin(listingBundle).on(listingBundle.listing.eq(tradeListing));
        }
        // 아이템명 키워드 검색 시 line JOIN 추가 필요
        if (StringUtils.hasText(cond.keyword())) {
            query = query.leftJoin(bundleLine).on(bundleLine.bundle.eq(listingBundle));
        }

        return query.where(
                isNotDeleted(),
                isNotHidden(),
                serverEq(cond.server()),
                statusEq(cond.status()),
                bundleTypeEq(cond.bundleType()),
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

        if (cond.bundleType() != null || StringUtils.hasText(cond.keyword())) {
            query = query.leftJoin(listingBundle).on(listingBundle.listing.eq(tradeListing));
        }
        if (StringUtils.hasText(cond.keyword())) {
            query = query.leftJoin(bundleLine).on(bundleLine.bundle.eq(listingBundle));
        }

        Long result = query.where(
                isNotDeleted(),
                isNotHidden(),
                serverEq(cond.server()),
                statusEq(cond.status()),
                bundleTypeEq(cond.bundleType()),
                itemNameContains(cond.keyword())
        ).fetchOne();
        return result != null ? result : 0L;
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

    /** 번들 라인의 아이템명 contains 검색 — null이면 조건 제외 */
    private BooleanExpression itemNameContains(String keyword) {
        return StringUtils.hasText(keyword)
                ? bundleLine.item.name.containsIgnoreCase(keyword)
                : null;
    }
}
