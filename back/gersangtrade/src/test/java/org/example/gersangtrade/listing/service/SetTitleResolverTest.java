package org.example.gersangtrade.listing.service;

import org.example.gersangtrade.domain.catalog.EquipmentItem;
import org.example.gersangtrade.domain.catalog.EquipmentSet;
import org.example.gersangtrade.domain.catalog.Ritual;
import org.example.gersangtrade.domain.catalog.enums.EquipmentSlot;
import org.example.gersangtrade.domain.catalog.enums.RitualType;
import org.example.gersangtrade.domain.listing.BundleEquipmentDetail;
import org.example.gersangtrade.domain.listing.BundleEquipmentRitual;
import org.example.gersangtrade.domain.listing.BundleLine;
import org.example.gersangtrade.domain.listing.enums.RitualOutcome;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * SetTitleResolver 단위 테스트 — DB에 구 포맷이 저장된 경우에도 재계산 규칙을 검증한다.
 */
class SetTitleResolverTest {

    @Test
    @DisplayName("대성공_3피스_반지포함_구DB제목과무관하게_신포맷_재계산")
    void 대성공_부분주술_반지포함() {
        EquipmentSet equipmentSet = mock(EquipmentSet.class);
        when(equipmentSet.getName()).thenReturn("각성광목천왕");

        Ritual ritual = Ritual.builder()
                .displayName("개양")
                .ritualType(RitualType.ARMOR)
                .successMark("<개양>")
                .greatSuccessMark("<북두칠성>")
                .build();

        List<BundleLine> lines = List.of(
                line(1L, 0), line(2L, 1), line(3L, 2),
                line(4L, 3), line(5L, 4), line(6L, 5));
        Map<Long, BundleEquipmentDetail> details = Map.of(
                1L, detail(1L, equipmentSet, EquipmentSlot.HELMET, false),
                2L, detail(2L, equipmentSet, EquipmentSlot.ARMOR, false),
                3L, detail(3L, equipmentSet, EquipmentSlot.GLOVES, true),
                4L, detail(4L, equipmentSet, EquipmentSlot.BELT, true),
                5L, detail(5L, equipmentSet, EquipmentSlot.SHOES, true),
                6L, detail(6L, equipmentSet, EquipmentSlot.RING, false));
        Map<Long, List<BundleEquipmentRitual>> rituals = Map.of(
                3L, List.of(ritualResult(lines.get(2), ritual, RitualOutcome.GREAT_SUCCESS)),
                4L, List.of(ritualResult(lines.get(3), ritual, RitualOutcome.GREAT_SUCCESS)),
                5L, List.of(ritualResult(lines.get(4), ritual, RitualOutcome.GREAT_SUCCESS)));

        Optional<String> title = SetTitleResolver.resolve(lines, details, rituals);

        assertThat(title).hasValue("3<북두칠성_개양> 풀 각성광목천왕반쌍");
    }

    private static BundleLine line(Long id, int sortOrder) {
        BundleLine line = mock(BundleLine.class);
        when(line.getId()).thenReturn(id);
        when(line.getSortOrder()).thenReturn(sortOrder);
        return line;
    }

    private static BundleEquipmentDetail detail(Long lineId, EquipmentSet set, EquipmentSlot slot, boolean hasRitual) {
        EquipmentItem equipmentItem = mock(EquipmentItem.class);
        when(equipmentItem.getEquipmentSet()).thenReturn(set);
        when(equipmentItem.getSlot()).thenReturn(slot);

        BundleEquipmentDetail detail = mock(BundleEquipmentDetail.class);
        when(detail.getBundleLineId()).thenReturn(lineId);
        when(detail.getEquipmentItem()).thenReturn(equipmentItem);
        when(detail.isHasRitual()).thenReturn(hasRitual);
        return detail;
    }

    private static BundleEquipmentRitual ritualResult(BundleLine line, Ritual ritual, RitualOutcome outcome) {
        BundleEquipmentRitual result = mock(BundleEquipmentRitual.class);
        when(result.getBundleLine()).thenReturn(line);
        when(result.getRitual()).thenReturn(ritual);
        when(result.getOutcome()).thenReturn(outcome);
        return result;
    }
}
