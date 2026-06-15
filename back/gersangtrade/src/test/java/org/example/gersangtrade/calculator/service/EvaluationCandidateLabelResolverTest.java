package org.example.gersangtrade.calculator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.gersangtrade.calculator.overlay.MercenaryMode;
import org.example.gersangtrade.calculator.overlay.ScenarioItemType;
import org.example.gersangtrade.catalog.repository.ItemRepository;
import org.example.gersangtrade.catalog.repository.MercenaryRepository;
import org.example.gersangtrade.domain.calculator.DpsValueEvaluation;
import org.example.gersangtrade.domain.catalog.EquipmentSet;
import org.example.gersangtrade.domain.catalog.Item;
import org.example.gersangtrade.domain.catalog.Mercenary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EvaluationCandidateLabelResolverTest {

    @Mock private ItemRepository itemRepository;
    @Mock private MercenaryRepository mercenaryRepository;
    @Mock private EvaluationSetTitleResolver setTitleResolver;
    @Spy private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private EvaluationCandidateLabelResolver resolver;

    private Item item;
    private Mercenary mercenary;

    @BeforeEach
    void setUp() {
        item = mock(Item.class);
        when(item.getName()).thenReturn("부동명왕부");

        mercenary = mock(Mercenary.class);
        when(mercenary.getName()).thenReturn("황건");
    }

    @Test
    @DisplayName("request_json_ITEM_SINGLE_아이템명_표시")
    void request_json_ITEM_SINGLE_아이템명_표시() throws Exception {
        String requestJson = """
                {
                  "deckId": 1,
                  "monsterId": 2,
                  "resistanceType": "MAGIC",
                  "memberInputs": [],
                  "persist": true,
                  "scenario": {
                    "type": "ITEM_SINGLE",
                    "affectedMemberId": 10,
                    "lines": [{"itemId": 100, "quantity": 1, "sortOrder": 0}]
                  }
                }
                """;
        DpsValueEvaluation evaluation = evaluationWithRequest(
                ScenarioItemType.MERCENARY, 99L, MercenaryMode.REPLACE, requestJson);

        when(itemRepository.findById(100L)).thenReturn(Optional.of(item));

        assertThat(resolver.resolve(evaluation)).isEqualTo("부동명왕부");
        verify(mercenaryRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("request_json_ITEM_SET_세트명_표시")
    void request_json_ITEM_SET_세트명_표시() throws Exception {
        String requestJson = """
                {
                  "deckId": 1,
                  "monsterId": 2,
                  "resistanceType": "MAGIC",
                  "memberInputs": [],
                  "persist": true,
                  "scenario": {
                    "type": "ITEM_SET",
                    "setId": 50,
                    "affectedMemberId": 10
                  }
                }
                """;
        DpsValueEvaluation evaluation = evaluationWithRequest(
                ScenarioItemType.MERCENARY, 99L, null, requestJson);

        when(setTitleResolver.resolve(50L, null)).thenReturn(Optional.of("풀 00세트"));

        assertThat(resolver.resolve(evaluation)).isEqualTo("풀 00세트");
        verify(mercenaryRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("구기록_MERCENARY오분류_mercenaryMode_null_아이템명_우선")
    void 구기록_MERCENARY오분류_mercenaryMode_null_아이템명_우선() {
        DpsValueEvaluation evaluation = evaluationWithRequest(
                ScenarioItemType.MERCENARY, 100L, null, null);
        when(itemRepository.findById(100L)).thenReturn(Optional.of(item));

        assertThat(resolver.resolve(evaluation)).isEqualTo("부동명왕부");
        verify(mercenaryRepository, never()).findById(anyLong());
    }

    private DpsValueEvaluation evaluationWithRequest(
            ScenarioItemType type, Long candidateRef, MercenaryMode mode, String requestJson) {
        return DpsValueEvaluation.builder()
                .user(mock(org.example.gersangtrade.domain.user.User.class))
                .deckId(1L)
                .monster(mock(org.example.gersangtrade.domain.catalog.Monster.class))
                .candidateType(type)
                .candidateRef(candidateRef)
                .mercenaryMode(mode)
                .priceSource(org.example.gersangtrade.calculator.dto.response.PriceSource.USER_INPUT)
                .price(100L)
                .requestJson(requestJson)
                .rawDpsBefore(1L).rawDpsAfter(2L)
                .adjustDpsBefore(1L).adjustDpsAfter(2L)
                .finalDpsBefore(1L).finalDpsAfter(2L)
                .rawDpsDelta(1L).rawDpsIncreaseRate(1.0)
                .adjustDpsDelta(1L).adjustDpsIncreaseRate(1.0)
                .finalDpsDelta(1L).finalDpsIncreaseRate(1.0)
                .evaluationHash("hash")
                .build();
    }
}
