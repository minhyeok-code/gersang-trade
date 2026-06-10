package org.example.gersangtrade.catalog.service;

import org.example.gersangtrade.catalog.dto.EquipmentSlotItemResponse;
import org.example.gersangtrade.catalog.repository.EquipmentItemRepository;
import org.example.gersangtrade.catalog.repository.ItemMercenaryRestrictionRepository;
import org.example.gersangtrade.catalog.repository.ItemStatRepository;
import org.example.gersangtrade.domain.catalog.EquipmentItem;
import org.example.gersangtrade.domain.catalog.Item;
import org.example.gersangtrade.domain.catalog.enums.EquipmentKind;
import org.example.gersangtrade.domain.catalog.enums.EquipmentSlot;
import org.example.gersangtrade.domain.deck.enums.EquipSlot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemSearchServiceExclusiveEquipmentTest {

    @Mock
    private EquipmentItemRepository equipmentItemRepository;

    @Mock
    private ItemStatRepository itemStatRepository;

    @Mock
    private ItemMercenaryRestrictionRepository itemMercenaryRestrictionRepository;

    @InjectMocks
    private ItemSearchService itemSearchService;

    @Test
    @DisplayName("getExclusiveEquipmentForMercenary_슬롯일치_전용무기만반환")
    void getExclusiveEquipmentForMercenary_슬롯일치_전용무기만반환() {
        EquipmentItem weapon = equipment(1L, "여포의 방천화극", EquipmentSlot.WEAPON, EquipmentKind.NORMAL, EquipSlot.WEAPON);
        EquipmentItem helmet = equipment(2L, "여포의 투구", EquipmentSlot.HELMET, EquipmentKind.NORMAL, EquipSlot.HELMET);

        when(equipmentItemRepository.findExclusiveEquipmentByMercenaryId(10L))
                .thenReturn(List.of(weapon, helmet));
        when(itemStatRepository.findByItemIdIn(anyList())).thenReturn(List.of());
        when(itemMercenaryRestrictionRepository.findByItemIdInWithMercenary(anyList()))
                .thenReturn(List.of());

        List<EquipmentSlotItemResponse> responses =
                itemSearchService.getExclusiveEquipmentForMercenary(10L, EquipSlot.WEAPON);

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().name()).isEqualTo("여포의 방천화극");
    }

    @Test
    @DisplayName("matchesDeckSlot_equipSlot불일치_slot기준_무기슬롯매칭")
    void matchesDeckSlot_equipSlot불일치_slot기준_무기슬롯매칭() {
        EquipmentItem misindexed = mock(EquipmentItem.class);
        when(misindexed.getSlot()).thenReturn(EquipmentSlot.WEAPON);
        when(misindexed.getEquipmentKind()).thenReturn(EquipmentKind.NORMAL);

        assertThat(ItemSearchService.matchesDeckSlot(misindexed, EquipSlot.WEAPON)).isTrue();
    }

    private static EquipmentItem equipment(long itemId, String name, EquipmentSlot slot,
                                           EquipmentKind kind, EquipSlot equipSlot) {
        Item item = mock(Item.class);
        lenient().when(item.getId()).thenReturn(itemId);
        lenient().when(item.getName()).thenReturn(name);

        EquipmentItem equipmentItem = mock(EquipmentItem.class);
        lenient().when(equipmentItem.getItemId()).thenReturn(itemId);
        lenient().when(equipmentItem.getItem()).thenReturn(item);
        lenient().when(equipmentItem.getSlot()).thenReturn(slot);
        lenient().when(equipmentItem.getEquipSlot()).thenReturn(equipSlot);
        lenient().when(equipmentItem.getEquipmentKind()).thenReturn(kind);
        lenient().when(equipmentItem.isRitualApplicable()).thenReturn(false);
        lenient().when(equipmentItem.isHasSlotOption()).thenReturn(false);
        lenient().when(equipmentItem.getMercenary()).thenReturn(null);
        lenient().when(equipmentItem.getEquipmentSet()).thenReturn(null);
        return equipmentItem;
    }
}
