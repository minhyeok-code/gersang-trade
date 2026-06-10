package org.example.gersangtrade.wanted.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.domain.wanted.WantedListing;
import org.example.gersangtrade.domain.wanted.enums.WantedStatus;
import org.example.gersangtrade.wanted.dto.request.WantedSearchCondition;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.example.gersangtrade.domain.wanted.QWantedItem.wantedItem;
import static org.example.gersangtrade.domain.wanted.QWantedListing.wantedListing;

/**
 * 구매 희망 등록글 동적 검색 QueryDSL 레포지토리.
 * 서버, 상태, 아이템명 키워드를 조합한 필터 검색을 지원한다.
 */
@Repository
@RequiredArgsConstructor
public class WantedListingQueryRepository {

    private final JPAQueryFactory queryFactory;

    /**
     * 다중 조건 필터 기반 구매 희망 등록글 목록 조회 (페이징 포함).
     * 소프트 삭제(deletedAt IS NULL), 관리자 숨김(hidden=false) 조건은 항상 적용한다.
     * 아이템명 키워드 또는 itemId 조건이 있으면 WantedItem까지 JOIN해서 검색한다.
     */
    public List<WantedListing> search(WantedSearchCondition cond) {
        var query = queryFactory
                .selectDistinct(wantedListing)
                .from(wantedListing)
                .leftJoin(wantedListing.buyer).fetchJoin();

        if (needsItemJoin(cond.keyword(), cond.itemId())) {
            query = query.leftJoin(wantedItem)
                    .on(wantedItem.wantedListing.eq(wantedListing));
        }

        return query.where(
                isNotDeleted(),
                isNotHidden(),
                serverEq(cond.server()),
                statusEq(cond.status()),
                itemNameContains(cond.keyword()),
                itemIdEq(cond.itemId())
        )
        .orderBy(wantedListing.createdAt.desc())
        .offset((long) cond.resolvedPage() * cond.resolvedSize())
        .limit(cond.resolvedSize())
        .fetch();
    }

    /**
     * 동일 조건의 총 건수 조회 (페이징 totalElements 계산용).
     * fetch().size() 대신 count 서브쿼리로 처리하여 불필요한 데이터 로딩을 방지한다.
     */
    public long count(WantedSearchCondition cond) {
        var query = queryFactory
                .select(wantedListing.id.countDistinct())
                .from(wantedListing);

        if (needsItemJoin(cond.keyword(), cond.itemId())) {
            query = query.leftJoin(wantedItem)
                    .on(wantedItem.wantedListing.eq(wantedListing));
        }

        Long result = query.where(
                isNotDeleted(),
                isNotHidden(),
                serverEq(cond.server()),
                statusEq(cond.status()),
                itemNameContains(cond.keyword()),
                itemIdEq(cond.itemId())
        ).fetchOne();
        return result != null ? result : 0L;
    }

    /**
     * 관심 SET 시세 조회용 — 세트 피스 itemId 목록 중 하나라도 포함한 구매 희망 등록글 후보 조회.
     * 2차 필터(SetWatchMatcher)에서 세부 주술 매칭을 위해 넉넉히 fetch한다.
     */
    public List<WantedListing> searchLatestBySetPieceIds(
            String server, WantedStatus status, List<Long> setPieceItemIds, int limit) {
        if (setPieceItemIds == null || setPieceItemIds.isEmpty()) {
            return List.of();
        }
        return queryFactory
                .selectDistinct(wantedListing)
                .from(wantedListing)
                .leftJoin(wantedListing.buyer).fetchJoin()
                .leftJoin(wantedItem).on(wantedItem.wantedListing.eq(wantedListing))
                .where(
                        isNotDeleted(),
                        isNotHidden(),
                        serverEq(server),
                        statusEq(status),
                        wantedItem.item.id.in(setPieceItemIds)
                )
                .orderBy(wantedListing.createdAt.desc())
                .limit(limit)
                .fetch();
    }

    /**
     * 관심 아이템 시세 조회용 배치 메서드.
     * 주어진 itemId 목록에 대해 서버·상태 조건으로 최신 글을 limitPerItem개씩 반환한다.
     * 관심 아이템 수가 최대 5개이므로 itemId별 개별 쿼리로 처리한다.
     */
    public Map<Long, List<WantedListing>> searchLatestPerItemIds(
            String server, WantedStatus status, List<Long> itemIds, int limitPerItem) {
        if (itemIds == null || itemIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, List<WantedListing>> result = new LinkedHashMap<>();
        for (Long itemId : itemIds) {
            List<WantedListing> listings = queryFactory
                    .selectDistinct(wantedListing)
                    .from(wantedListing)
                    .leftJoin(wantedListing.buyer).fetchJoin()
                    .leftJoin(wantedItem).on(wantedItem.wantedListing.eq(wantedListing))
                    .where(
                            isNotDeleted(),
                            isNotHidden(),
                            serverEq(server),
                            statusEq(status),
                            wantedItem.item.id.eq(itemId)
                    )
                    .orderBy(wantedListing.createdAt.desc())
                    .limit(limitPerItem)
                    .fetch();
            result.put(itemId, listings);
        }
        return result;
    }

    /** 소프트 삭제되지 않은 항목만 조회 */
    private BooleanExpression isNotDeleted() {
        return wantedListing.deletedAt.isNull();
    }

    /** 관리자 숨김 처리되지 않은 항목만 조회 */
    private BooleanExpression isNotHidden() {
        return wantedListing.hidden.eq(false);
    }

    /** 서버 필터 — null이면 조건 제외 */
    private BooleanExpression serverEq(String server) {
        return StringUtils.hasText(server) ? wantedListing.server.eq(server) : null;
    }

    /** 상태 필터 — null이면 조건 제외 */
    private BooleanExpression statusEq(WantedStatus status) {
        return status != null ? wantedListing.status.eq(status) : null;
    }

    /** WantedItem의 아이템명 contains 검색 — null이면 조건 제외 */
    private BooleanExpression itemNameContains(String keyword) {
        return StringUtils.hasText(keyword)
                ? wantedItem.item.name.containsIgnoreCase(keyword)
                : null;
    }

    /** WantedItem의 itemId 일치 검색 — null이면 조건 제외 */
    private BooleanExpression itemIdEq(Long itemId) {
        return itemId != null ? wantedItem.item.id.eq(itemId) : null;
    }

    /** wantedItem JOIN이 필요한지 여부 — 키워드 또는 itemId 조건이 있을 때 */
    private boolean needsItemJoin(String keyword, Long itemId) {
        return StringUtils.hasText(keyword) || itemId != null;
    }
}
