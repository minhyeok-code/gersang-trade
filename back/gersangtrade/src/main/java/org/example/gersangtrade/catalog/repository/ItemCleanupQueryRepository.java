package org.example.gersangtrade.catalog.repository;

import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.admin.dto.enums.ItemCleanupCriterion;
import org.example.gersangtrade.domain.catalog.enums.EquipmentSlot;
import org.example.gersangtrade.domain.catalog.enums.ItemType;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 크롤링 적재 아이템 정리 후보 조회 — jOOQ 네이티브 SQL.
 */
@Repository
@RequiredArgsConstructor
public class ItemCleanupQueryRepository {

    private static final int MAX_PER_CRITERION = 300;

    private final DSLContext dsl;

    public record RawCandidate(
            Long id,
            String name,
            ItemType type,
            EquipmentSlot slot,
            String setName
    ) {}

    public record CandidateWithReasons(
            RawCandidate candidate,
            List<ItemCleanupCriterion> matchedCriteria
    ) {}

    /**
     * 선택된 기준(OR)에 해당하는 후보를 조회한다.
     * 동일 아이템이 여러 기준에 걸리면 matchedCriteria에 모두 누적된다.
     */
    public List<CandidateWithReasons> findByCriteria(EnumSet<ItemCleanupCriterion> criteria) {
        Map<Long, RawCandidate> rows = new LinkedHashMap<>();
        Map<Long, List<ItemCleanupCriterion>> reasons = new LinkedHashMap<>();

        for (ItemCleanupCriterion criterion : criteria) {
            for (RawCandidate row : queryCriterion(criterion)) {
                rows.putIfAbsent(row.id(), row);
                reasons.computeIfAbsent(row.id(), k -> new ArrayList<>()).add(criterion);
            }
        }

        return rows.entrySet().stream()
                .map(e -> new CandidateWithReasons(e.getValue(), reasons.get(e.getKey())))
                .toList();
    }

    private List<RawCandidate> queryCriterion(ItemCleanupCriterion criterion) {
        return dsl.fetch(buildSql(criterion), MAX_PER_CRITERION)
                .map(this::toRawCandidate);
    }

    private String buildSql(ItemCleanupCriterion criterion) {
        return """
                SELECT i.id, i.name, i.type, ei.slot, es.name AS set_name
                """ + buildFromWhere(criterion) + """
                 LIMIT ?
                """;
    }

    private String buildFromWhere(ItemCleanupCriterion criterion) {
        return switch (criterion) {
            case NO_EQUIP_SLOT -> """
                    FROM items i
                    JOIN equipment_items ei ON ei.item_id = i.id
                    LEFT JOIN equipment_sets es ON ei.set_id = es.id
                    WHERE ei.equip_slot IS NULL
                      AND ei.slot <> 'RING'
                    """;
            case UNSUPPORTED_SLOT -> """
                    FROM items i
                    JOIN equipment_items ei ON ei.item_id = i.id
                    LEFT JOIN equipment_sets es ON ei.set_id = es.id
                    WHERE ei.slot IN ('ORB', 'WING', 'TITLE')
                    """;
            case NON_TRADEABLE_SET -> """
                    FROM items i
                    JOIN equipment_items ei ON ei.item_id = i.id
                    JOIN equipment_sets es ON ei.set_id = es.id
                    WHERE es.is_tradeable = FALSE
                    """;
            case NOT_SET_PIECE -> """
                    FROM items i
                    JOIN equipment_items ei ON ei.item_id = i.id
                    LEFT JOIN equipment_sets es ON ei.set_id = es.id
                    LEFT JOIN equipment_set_pieces esp ON esp.equipment_item_id = ei.item_id
                    WHERE esp.id IS NULL
                    """;
            case NO_STATS -> """
                    FROM items i
                    LEFT JOIN equipment_items ei ON ei.item_id = i.id
                    LEFT JOIN equipment_sets es ON ei.set_id = es.id
                    WHERE NOT EXISTS (
                        SELECT 1 FROM item_stats st WHERE st.item_id = i.id
                    )
                    """;
            case UNREFERENCED -> """
                    FROM items i
                    LEFT JOIN equipment_items ei ON ei.item_id = i.id
                    LEFT JOIN equipment_sets es ON ei.set_id = es.id
                    WHERE NOT EXISTS (SELECT 1 FROM bundle_lines bl WHERE bl.item_id = i.id)
                      AND NOT EXISTS (SELECT 1 FROM user_deck_member_slots ds WHERE ds.equipment_item_id = i.id)
                      AND NOT EXISTS (SELECT 1 FROM user_deck_member_equips de WHERE de.equipment_item_id = i.id)
                      AND NOT EXISTS (SELECT 1 FROM equipment_set_pieces esp WHERE esp.equipment_item_id = i.id)
                      AND NOT EXISTS (SELECT 1 FROM wanted_items wi WHERE wi.item_id = i.id)
                    """;
        };
    }

    private RawCandidate toRawCandidate(Record r) {
        return new RawCandidate(
                r.get("id", Long.class),
                r.get("name", String.class),
                parseEnum(ItemType.class, r.get("type", String.class)),
                parseEnum(EquipmentSlot.class, r.get("slot", String.class)),
                r.get("set_name", String.class)
        );
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumClass, String value) {
        if (value == null) return null;
        try {
            return Enum.valueOf(enumClass, value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
