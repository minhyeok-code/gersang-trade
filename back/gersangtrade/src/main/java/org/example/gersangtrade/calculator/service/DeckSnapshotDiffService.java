package org.example.gersangtrade.calculator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.calculator.dto.response.DeckSnapshotDiffLine;
import org.example.gersangtrade.deck.dto.response.DeckMemberSlotResponse;
import org.example.gersangtrade.hunt.dto.DeckSnapshotContent;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 덱 스냅샷 JSON 간 장비 차이를 요약한다.
 */
@Service
@RequiredArgsConstructor
public class DeckSnapshotDiffService {

    private final ObjectMapper objectMapper;

    public List<DeckSnapshotDiffLine> diff(String baselineJson, String currentJson) {
        DeckSnapshotContent baseline = parse(baselineJson);
        DeckSnapshotContent current = parse(currentJson);
        if (baseline == null || current == null) {
            return List.of();
        }

        Map<Long, DeckSnapshotContent.SnapshotMember> currentByMemberId = new HashMap<>();
        for (DeckSnapshotContent.SnapshotMember member : current.members()) {
            currentByMemberId.put(member.member().id(), member);
        }

        List<DeckSnapshotDiffLine> lines = new ArrayList<>();
        for (DeckSnapshotContent.SnapshotMember beforeMember : baseline.members()) {
            Long memberId = beforeMember.member().id();
            DeckSnapshotContent.SnapshotMember afterMember = currentByMemberId.get(memberId);
            if (afterMember == null) {
                continue;
            }
            Map<String, String> beforeSlots = slotItemNames(beforeMember);
            Map<String, String> afterSlots = slotItemNames(afterMember);
            for (String slot : unionKeys(beforeSlots, afterSlots)) {
                String beforeName = beforeSlots.get(slot);
                String afterName = afterSlots.get(slot);
                if (!Objects.equals(beforeName, afterName)) {
                    lines.add(new DeckSnapshotDiffLine(
                            memberId,
                            beforeMember.member().mercenaryName(),
                            slot,
                            beforeName,
                            afterName
                    ));
                }
            }
        }

        lines.sort(Comparator
                .comparing(DeckSnapshotDiffLine::mercenaryName, Comparator.nullsLast(String::compareTo))
                .thenComparing(DeckSnapshotDiffLine::slot));
        return lines;
    }

    private DeckSnapshotContent parse(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, DeckSnapshotContent.class);
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, String> slotItemNames(DeckSnapshotContent.SnapshotMember member) {
        Map<String, String> map = new HashMap<>();
        if (member.member().slots() == null) {
            return map;
        }
        for (DeckMemberSlotResponse slot : member.member().slots()) {
            if (slot.slot() != null) {
                map.put(slot.slot().name(), slot.itemName());
            }
        }
        return map;
    }

    private List<String> unionKeys(Map<String, String> a, Map<String, String> b) {
        List<String> keys = new ArrayList<>();
        a.keySet().forEach(k -> { if (!keys.contains(k)) keys.add(k); });
        b.keySet().forEach(k -> { if (!keys.contains(k)) keys.add(k); });
        return keys;
    }
}
