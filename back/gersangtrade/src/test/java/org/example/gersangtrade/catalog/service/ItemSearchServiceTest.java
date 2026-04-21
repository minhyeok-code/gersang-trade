package org.example.gersangtrade.catalog.service;

import org.example.gersangtrade.catalog.dto.ItemSearchResult;
import org.example.gersangtrade.catalog.dto.RitualResponse;
import org.example.gersangtrade.catalog.repository.ItemJooqRepository;
import org.example.gersangtrade.catalog.repository.RitualApplicabilityRepository;
import org.example.gersangtrade.domain.catalog.Ritual;
import org.example.gersangtrade.domain.catalog.RitualApplicability;
import org.example.gersangtrade.domain.catalog.enums.EquipmentKind;
import org.example.gersangtrade.domain.catalog.enums.ItemType;
import org.example.gersangtrade.domain.catalog.enums.RitualType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * ItemSearchService 단위 테스트.
 * Mockito로 ItemJooqRepository·RitualApplicabilityRepository를 Mock 처리하여
 * 서비스 위임 로직·limit 경계값·DTO 변환을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class ItemSearchServiceTest {

    @Mock
    private ItemJooqRepository itemJooqRepository;

    @Mock
    private RitualApplicabilityRepository ritualApplicabilityRepository;

    @InjectMocks
    private ItemSearchService itemSearchService;

    // ── search() 위임 및 limit 경계값 테스트 ──────────────────────────────────

    @Test
    @DisplayName("search_정상키워드_jooqRepository호출후결과반환")
    void search_정상키워드_jooqRepository호출후결과반환() {
        ItemSearchResult expected = new ItemSearchResult(
                1L, "강화석", ItemType.MATERIAL, null, null, null, "개");
        when(itemJooqRepository.searchRanked("강화", null, null, 20))
                .thenReturn(List.of(expected));

        List<ItemSearchResult> results = itemSearchService.search("강화", null, null, null);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).name()).isEqualTo("강화석");
        assertThat(results.get(0).type()).isEqualTo(ItemType.MATERIAL);
        verify(itemJooqRepository).searchRanked("강화", null, null, 20);
    }

    @Test
    @DisplayName("search_limit_null이면_기본값20으로호출")
    void search_limit_null이면_기본값20으로호출() {
        when(itemJooqRepository.searchRanked(any(), any(), any(), eq(20)))
                .thenReturn(Collections.emptyList());

        itemSearchService.search("검", null, null, null);

        verify(itemJooqRepository).searchRanked("검", null, null, 20);
    }

    @Test
    @DisplayName("search_limit_50초과이면_기본값20으로호출")
    void search_limit_50초과이면_기본값20으로호출() {
        when(itemJooqRepository.searchRanked(any(), any(), any(), eq(20)))
                .thenReturn(Collections.emptyList());

        itemSearchService.search("검", null, null, 51);

        // limit=51은 정책 초과이므로 기본값 20으로 보정되어야 함
        verify(itemJooqRepository).searchRanked("검", null, null, 20);
    }

    @Test
    @DisplayName("search_limit_0이하이면_기본값20으로호출")
    void search_limit_0이하이면_기본값20으로호출() {
        when(itemJooqRepository.searchRanked(any(), any(), any(), eq(20)))
                .thenReturn(Collections.emptyList());

        itemSearchService.search("검", null, null, 0);

        // limit=0은 유효하지 않으므로 기본값 20으로 보정되어야 함
        verify(itemJooqRepository).searchRanked("검", null, null, 20);
    }

    @Test
    @DisplayName("search_limit_음수이면_기본값20으로호출")
    void search_limit_음수이면_기본값20으로호출() {
        when(itemJooqRepository.searchRanked(any(), any(), any(), eq(20)))
                .thenReturn(Collections.emptyList());

        itemSearchService.search("검", null, null, -5);

        verify(itemJooqRepository).searchRanked("검", null, null, 20);
    }

    @Test
    @DisplayName("search_limit_유효범위내이면_그대로전달")
    void search_limit_유효범위내이면_그대로전달() {
        when(itemJooqRepository.searchRanked(any(), any(), any(), eq(15)))
                .thenReturn(Collections.emptyList());

        itemSearchService.search("검", null, null, 15);

        verify(itemJooqRepository).searchRanked("검", null, null, 15);
    }

    @Test
    @DisplayName("search_limit_경계값50이면_그대로전달")
    void search_limit_경계값50이면_그대로전달() {
        when(itemJooqRepository.searchRanked(any(), any(), any(), eq(50)))
                .thenReturn(Collections.emptyList());

        itemSearchService.search("검", null, null, 50);

        // 50은 허용 최대값이므로 그대로 전달되어야 함
        verify(itemJooqRepository).searchRanked("검", null, null, 50);
    }

    @Test
    @DisplayName("search_타입및종류필터포함_파라미터그대로전달")
    void search_타입및종류필터포함_파라미터그대로전달() {
        when(itemJooqRepository.searchRanked("투구", ItemType.EQUIPMENT, EquipmentKind.NORMAL, 20))
                .thenReturn(Collections.emptyList());

        itemSearchService.search("투구", ItemType.EQUIPMENT, EquipmentKind.NORMAL, null);

        verify(itemJooqRepository).searchRanked("투구", ItemType.EQUIPMENT, EquipmentKind.NORMAL, 20);
    }

    @Test
    @DisplayName("search_결과없음_빈목록반환")
    void search_결과없음_빈목록반환() {
        when(itemJooqRepository.searchRanked(any(), any(), any(), anyInt()))
                .thenReturn(Collections.emptyList());

        List<ItemSearchResult> results = itemSearchService.search("없는아이템", null, null, null);

        assertThat(results).isEmpty();
    }

    // ── findAvailableRituals() 테스트 ──────────────────────────────────────

    @Test
    @DisplayName("findAvailableRituals_주술있음_RitualResponse목록으로변환반환")
    void findAvailableRituals_주술있음_RitualResponse목록으로변환반환() {
        Ritual ritual = Ritual.builder()
                .displayName("XX주술")
                .ritualType(RitualType.ARMOR)
                .successMark("00")
                .greatSuccessMark("**")
                .build();

        RitualApplicability applicability = mock(RitualApplicability.class);
        when(applicability.getRitual()).thenReturn(ritual);

        when(ritualApplicabilityRepository.findByEquipmentItemIdWithRitual(10L))
                .thenReturn(List.of(applicability));

        List<RitualResponse> responses = itemSearchService.findAvailableRituals(10L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).displayName()).isEqualTo("XX주술");
        assertThat(responses.get(0).ritualType()).isEqualTo(RitualType.ARMOR);
        assertThat(responses.get(0).successMark()).isEqualTo("00");
        assertThat(responses.get(0).greatSuccessMark()).isEqualTo("**");
    }

    @Test
    @DisplayName("findAvailableRituals_여러주술있음_전체변환반환")
    void findAvailableRituals_여러주술있음_전체변환반환() {
        Ritual armorRitual = Ritual.builder()
                .displayName("XX주술")
                .ritualType(RitualType.ARMOR)
                .successMark("00")
                .greatSuccessMark("**")
                .build();
        Ritual weaponRitual = Ritual.builder()
                .displayName("OO주술")
                .ritualType(RitualType.WEAPON)
                .successMark("OO")
                .greatSuccessMark("@@")
                .build();

        RitualApplicability ap1 = mock(RitualApplicability.class);
        when(ap1.getRitual()).thenReturn(armorRitual);
        RitualApplicability ap2 = mock(RitualApplicability.class);
        when(ap2.getRitual()).thenReturn(weaponRitual);

        when(ritualApplicabilityRepository.findByEquipmentItemIdWithRitual(5L))
                .thenReturn(List.of(ap1, ap2));

        List<RitualResponse> responses = itemSearchService.findAvailableRituals(5L);

        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(RitualResponse::displayName)
                .containsExactly("XX주술", "OO주술");
    }

    @Test
    @DisplayName("findAvailableRituals_주술없음_빈목록반환")
    void findAvailableRituals_주술없음_빈목록반환() {
        when(ritualApplicabilityRepository.findByEquipmentItemIdWithRitual(99L))
                .thenReturn(Collections.emptyList());

        List<RitualResponse> responses = itemSearchService.findAvailableRituals(99L);

        assertThat(responses).isEmpty();
    }
}
