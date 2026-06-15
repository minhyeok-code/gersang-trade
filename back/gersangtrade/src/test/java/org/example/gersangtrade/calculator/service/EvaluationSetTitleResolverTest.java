package org.example.gersangtrade.calculator.service;

import org.example.gersangtrade.calculator.overlay.ScenarioLine;
import org.example.gersangtrade.catalog.repository.EquipmentItemRepository;
import org.example.gersangtrade.catalog.repository.EquipmentSetPieceRepository;
import org.example.gersangtrade.catalog.repository.EquipmentSetRepository;
import org.example.gersangtrade.catalog.repository.RitualRepository;
import org.example.gersangtrade.domain.catalog.EquipmentItem;
import org.example.gersangtrade.domain.catalog.EquipmentSet;
import org.example.gersangtrade.domain.catalog.EquipmentSetPiece;
import org.example.gersangtrade.domain.catalog.enums.EquipmentSlot;
import org.example.gersangtrade.listing.dto.request.EquipmentDetailRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EvaluationSetTitleResolverTest {

    @Mock private EquipmentSetRepository equipmentSetRepository;
    @Mock private EquipmentSetPieceRepository equipmentSetPieceRepository;
    @Mock private EquipmentItemRepository equipmentItemRepository;
    @Mock private RitualRepository ritualRepository;

    @InjectMocks
    private EvaluationSetTitleResolver resolver;

    @Test
    @DisplayName("lines_없음_풀세트_표기")
    void lines_없음_풀세트_표기() {
        EquipmentSet set = mock(EquipmentSet.class);
        when(set.getName()).thenReturn("대위덕명왕");
        when(equipmentSetRepository.findById(50L)).thenReturn(Optional.of(set));
        List<EquipmentSetPiece> fullPieces = List.of(
                pieceWithSlot(EquipmentSlot.HELMET),
                pieceWithSlot(EquipmentSlot.ARMOR),
                pieceWithSlot(EquipmentSlot.GLOVES),
                pieceWithSlot(EquipmentSlot.BELT),
                pieceWithSlot(EquipmentSlot.SHOES)
        );
        when(equipmentSetPieceRepository.findWithItemByEquipmentSetId(50L)).thenReturn(fullPieces);

        Optional<String> title = resolver.resolve(50L, null);

        assertThat(title).hasValue("풀 대위덕명왕");
    }

    @Test
    @DisplayName("lines_갑투만_갑투표기")
    void lines_갑투만_갑투표기() {
        EquipmentSet set = mock(EquipmentSet.class);
        when(set.getName()).thenReturn("대위덕명왕");
        when(equipmentSetRepository.findById(50L)).thenReturn(Optional.of(set));

        EquipmentItem helmet = equipmentWithSlot(100L, EquipmentSlot.HELMET);
        EquipmentItem armor = equipmentWithSlot(101L, EquipmentSlot.ARMOR);
        when(equipmentItemRepository.findWithItemAndSetByItemIdIn(List.of(100L, 101L)))
                .thenReturn(List.of(helmet, armor));

        List<ScenarioLine> lines = List.of(
                new ScenarioLine(100L, 1, 0, new EquipmentDetailRequest(0, false, List.of())),
                new ScenarioLine(101L, 1, 1, new EquipmentDetailRequest(0, false, List.of()))
        );

        Optional<String> title = resolver.resolve(50L, lines);

        assertThat(title).hasValue("대위덕명왕갑투");
    }

    private EquipmentSetPiece pieceWithSlot(EquipmentSlot slot) {
        EquipmentSetPiece piece = mock(EquipmentSetPiece.class);
        when(piece.getSlot()).thenReturn(slot);
        return piece;
    }

    private EquipmentItem equipmentWithSlot(Long itemId, EquipmentSlot slot) {
        EquipmentItem equipment = mock(EquipmentItem.class);
        when(equipment.getItemId()).thenReturn(itemId);
        when(equipment.getSlot()).thenReturn(slot);
        return equipment;
    }
}
