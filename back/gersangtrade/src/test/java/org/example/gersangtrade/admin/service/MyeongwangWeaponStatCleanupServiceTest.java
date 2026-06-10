package org.example.gersangtrade.admin.service;

import org.example.gersangtrade.catalog.repository.ItemStatRepository;
import org.example.gersangtrade.domain.catalog.Item;
import org.example.gersangtrade.domain.catalog.ItemStat;
import org.example.gersangtrade.domain.catalog.enums.BuffTarget;
import org.example.gersangtrade.domain.catalog.enums.Element;
import org.example.gersangtrade.domain.catalog.enums.StatType;
import org.example.gersangtrade.domain.catalog.enums.StatUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MyeongwangWeaponStatCleanupServiceTest {

    @Mock
    private ItemStatRepository itemStatRepository;

    @InjectMocks
    private MyeongwangWeaponStatCleanupService cleanupService;

    @Test
    @DisplayName("명왕무기_SELF_속성값_삭제")
    void removeSelfElementValueStats() {
        Item item = Item.builder().name("명왕검").build();
        ItemStat wrong = ItemStat.builder()
                .item(item)
                .statType(StatType.ELEMENT_VALUE)
                .element(Element.FIRE)
                .value(5)
                .statUnit(StatUnit.FLAT)
                .scope(BuffTarget.SELF)
                .build();

        when(itemStatRepository.findMyeongwangWeaponSelfElementValueStats())
                .thenReturn(List.of(wrong));

        var result = cleanupService.removeSelfElementValueStats();

        assertThat(result.deletedCount()).isEqualTo(1);
        assertThat(result.itemNames()).containsExactly("명왕검");
        verify(itemStatRepository).delete(wrong);
    }
}
