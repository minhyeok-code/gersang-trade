package org.example.gersangtrade.wanted.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.domain.wanted.WantedListing;
import org.example.gersangtrade.domain.wanted.enums.WantedStatus;
import org.example.gersangtrade.wanted.dto.request.WantedSearchCondition;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

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
     * 아이템명 키워드 조건이 있으면 WantedItem까지 JOIN해서 검색한다.
     */
    public List<WantedListing> search(WantedSearchCondition cond) {
        var query = queryFactory
                .selectDistinct(wantedListing)
                .from(wantedListing)
                .leftJoin(wantedListing.buyer).fetchJoin();

        // 아이템명 키워드 검색 시 wantedItem JOIN 필요
        if (StringUtils.hasText(cond.keyword())) {
            query = query.leftJoin(wantedItem)
                    .on(wantedItem.wantedListing.eq(wantedListing));
        }

        return query.where(
                isNotDeleted(),
                isNotHidden(),
                serverEq(cond.server()),
                statusEq(cond.status()),
                itemNameContains(cond.keyword())
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
        // count 쿼리: id만 집계하여 DB에서 직접 건수 반환
        var query = queryFactory
                .select(wantedListing.id.countDistinct())
                .from(wantedListing);

        if (StringUtils.hasText(cond.keyword())) {
            query = query.leftJoin(wantedItem)
                    .on(wantedItem.wantedListing.eq(wantedListing));
        }

        Long result = query.where(
                isNotDeleted(),
                isNotHidden(),
                serverEq(cond.server()),
                statusEq(cond.status()),
                itemNameContains(cond.keyword())
        ).fetchOne();
        return result != null ? result : 0L;
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
}
