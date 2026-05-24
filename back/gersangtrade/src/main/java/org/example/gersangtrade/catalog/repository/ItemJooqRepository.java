package org.example.gersangtrade.catalog.repository;

import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.catalog.dto.ItemSearchResult;
import org.example.gersangtrade.domain.catalog.enums.EquipmentKind;
import org.example.gersangtrade.domain.catalog.enums.EquipmentSlot;
import org.example.gersangtrade.domain.catalog.enums.ItemType;
import org.example.gersangtrade.domain.deck.enums.EquipSlot;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * jOOQ 기반 아이템 검색 레포지토리.
 *
 * <h3>jOOQ를 선택한 이유</h3>
 * 자동완성에는 "입력어로 시작하는 아이템이 상단 노출"되는 rank 정렬이 필요하다.
 * {@code CASE WHEN name LIKE '키워드%' THEN 0 ELSE 1 END} 같은 인라인 rank 표현식은
 * JPQL/HQL이 지원하지 않고 QueryDSL도 OrderSpecifier에 직접 표현식을 넣기 불편하다.
 * jOOQ는 SQL을 그대로 쓰므로 이런 rank 표현식을 자연스럽게 표현할 수 있다.
 *
 * <h3>쿼리 구조</h3>
 * <pre>
 * SELECT i.id, i.name, i.type, ei.equipment_kind, ei.slot, es.name, mi.stack_unit_name,
 *        CASE WHEN i.name LIKE '키워드%' THEN 0 ELSE 1 END AS match_priority
 * FROM items i
 * LEFT JOIN equipment_items ei ON i.id = ei.item_id
 * LEFT JOIN equipment_sets  es ON ei.set_id = es.id
 * LEFT JOIN material_items  mi ON i.id = mi.item_id
 * WHERE i.name LIKE '%키워드%'
 *   [AND i.type = ?]
 *   [AND ei.equipment_kind = ?]
 * ORDER BY match_priority ASC, LENGTH(i.name) ASC, i.name ASC
 * LIMIT ?
 * </pre>
 */
@Repository
@RequiredArgsConstructor
public class ItemJooqRepository {

    private final DSLContext dsl;

    /**
     * 키워드로 아이템을 ranked 검색한다.
     * starts-with 매칭(match_priority=0)이 contains 매칭(match_priority=1)보다 먼저 노출된다.
     *
     * @param keyword     검색 키워드 (공백이면 빈 리스트 반환)
     * @param type        아이템 타입 필터 (null이면 전체)
     * @param kind        장비 종류 필터 (null이면 전체, MATERIAL 타입에는 무의미)
     * @param limit       최대 결과 수
     * @return ranked 정렬된 ItemSearchResult 목록
     */
    public List<ItemSearchResult> searchRanked(String keyword, ItemType type,
                                               EquipmentKind kind, int limit) {
        if (!StringUtils.hasText(keyword)) {
            return List.of();
        }

        // SQL 동적 조립
        String startsWithPattern = keyword + "%";
        String containsPattern = "%" + keyword + "%";

        StringBuilder sql = new StringBuilder("""
                SELECT i.id, i.name, i.type, i.image_url,
                       ei.equipment_kind, ei.slot, ei.equip_slot,
                       es.id AS set_id, es.name AS set_name,
                       mi.stack_unit_name,
                       CASE WHEN i.name LIKE ? THEN 0 ELSE 1 END AS match_priority
                FROM items i
                LEFT JOIN equipment_items ei ON i.id = ei.item_id
                LEFT JOIN equipment_sets  es ON ei.set_id = es.id
                LEFT JOIN material_items  mi ON i.id = mi.item_id
                WHERE i.name LIKE ?
                """);

        List<Object> bindings = new ArrayList<>();
        bindings.add(startsWithPattern);  // CASE WHEN 바인딩
        bindings.add(containsPattern);    // WHERE LIKE 바인딩

        // 아이템 타입 필터 (선택)
        if (type != null) {
            sql.append("  AND i.type = ?\n");
            bindings.add(type.name());
        }

        // 장비 종류 필터 — equipment_items join이 이미 있으므로 바로 추가 (선택)
        if (kind != null) {
            sql.append("  AND ei.equipment_kind = ?\n");
            bindings.add(kind.name());
        }

        sql.append("ORDER BY match_priority ASC, LENGTH(i.name) ASC, i.name ASC\n");
        sql.append("LIMIT ?");
        bindings.add(limit);

        return dsl.fetch(sql.toString(), bindings.toArray())
                .map(r -> new ItemSearchResult(
                        r.get("id", Long.class),
                        r.get("name", String.class),
                        parseEnum(ItemType.class, r.get("type", String.class)),
                        parseEnum(EquipmentKind.class, r.get("equipment_kind", String.class)),
                        parseEnum(EquipmentSlot.class, r.get("slot", String.class)),
                        r.get("set_id", Long.class),
                        r.get("set_name", String.class),
                        r.get("stack_unit_name", String.class),
                        r.get("image_url", String.class),
                        parseEnum(EquipSlot.class, r.get("equip_slot", String.class))
                ));
    }

    /** 문자열을 Enum으로 변환. null이면 null 반환. */
    private <E extends Enum<E>> E parseEnum(Class<E> enumClass, String value) {
        if (value == null) return null;
        try {
            return Enum.valueOf(enumClass, value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
