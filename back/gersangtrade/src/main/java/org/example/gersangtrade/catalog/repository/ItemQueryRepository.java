package org.example.gersangtrade.catalog.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.catalog.dto.ItemSearchResult;
import org.example.gersangtrade.domain.catalog.enums.EquipmentKind;
import org.example.gersangtrade.domain.catalog.enums.ItemType;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

import static org.example.gersangtrade.domain.catalog.QEquipmentItem.equipmentItem;
import static org.example.gersangtrade.domain.catalog.QEquipmentSet.equipmentSet;
import static org.example.gersangtrade.domain.catalog.QItem.item;
import static org.example.gersangtrade.domain.catalog.QMaterialItem.materialItem;

/**
 * QueryDSL 기반 아이템 검색 레포지토리.
 *
 * <h3>QueryDSL을 선택한 이유</h3>
 * 타입 안전한 동적 조건 조립이 필요할 때 사용한다.
 * 검색 결과를 DTO로 프로젝션하면서 여러 테이블의 LEFT JOIN을 타입 안전하게 처리한다.
 * jOOQ(ranked 검색)와 달리 순서 우선순위 없이 단순 조건 필터링이 필요한 경우에 적합하다.
 *
 * <h3>쿼리 구조</h3>
 * items LEFT JOIN equipment_items LEFT JOIN equipment_sets LEFT JOIN material_items
 * WHERE name LIKE '%keyword%' [AND type = ?] [AND equipment_kind = ?]
 * ORDER BY items.name ASC
 */
@Repository
@RequiredArgsConstructor
public class ItemQueryRepository {

    private final JPAQueryFactory queryFactory;

    /**
     * 키워드 + 선택 필터로 아이템을 검색한다 (알파벳순 정렬, rank 없음).
     * rank 정렬이 필요하면 {@link ItemJooqRepository#searchRanked} 사용.
     *
     * @param keyword 검색 키워드 (null/공백이면 전체)
     * @param type    아이템 타입 필터 (null이면 전체)
     * @param kind    장비 종류 필터 (null이면 전체)
     * @param limit   최대 결과 수
     */
    public List<ItemSearchResult> search(String keyword, ItemType type,
                                         EquipmentKind kind, int limit) {
        return queryFactory
                .select(Projections.constructor(ItemSearchResult.class,
                        item.id,
                        item.name,
                        item.type,
                        equipmentItem.equipmentKind,
                        equipmentItem.slot,
                        equipmentSet.name,
                        materialItem.stackUnitName
                ))
                .from(item)
                .leftJoin(equipmentItem).on(equipmentItem.item.eq(item))
                .leftJoin(equipmentItem.equipmentSet, equipmentSet)
                .leftJoin(materialItem).on(materialItem.item.eq(item))
                .where(
                        nameContains(keyword),
                        typeEq(type),
                        kindEq(kind)
                )
                .orderBy(item.name.asc())
                .limit(limit)
                .fetch();
    }

    // ── 동적 조건 헬퍼 (null이면 QueryDSL이 자동으로 WHERE절에서 제외) ─────────

    /** 이름에 키워드가 포함되는 조건. keyword가 비어있으면 null 반환 → 조건 제외. */
    private BooleanExpression nameContains(String keyword) {
        return StringUtils.hasText(keyword) ? item.name.containsIgnoreCase(keyword) : null;
    }

    /** 아이템 타입 일치 조건 */
    private BooleanExpression typeEq(ItemType type) {
        return type != null ? item.type.eq(type) : null;
    }

    /** 장비 종류 일치 조건 */
    private BooleanExpression kindEq(EquipmentKind kind) {
        return kind != null ? equipmentItem.equipmentKind.eq(kind) : null;
    }
}
