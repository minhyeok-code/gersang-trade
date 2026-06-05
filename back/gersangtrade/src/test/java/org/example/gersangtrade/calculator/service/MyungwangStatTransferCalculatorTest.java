package org.example.gersangtrade.calculator.service;

import org.example.gersangtrade.catalog.repository.MercenaryCharacteristicLevelRepository;
import org.example.gersangtrade.catalog.repository.MercenaryCharacteristicRepository;
import org.example.gersangtrade.domain.catalog.Mercenary;
import org.example.gersangtrade.domain.catalog.enums.MercenaryCategory;
import org.example.gersangtrade.domain.catalog.enums.Nature;
import org.example.gersangtrade.domain.catalog.enums.StatType;
import org.example.gersangtrade.domain.deck.UserDeckMember;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 명왕 스탯 이전 계산기 단위 테스트.
 *
 * <p>배정 규칙:
 *   - 속성 제한 없음 (임의 명왕 → 임의 사천왕)
 *   - 동속성 명왕·사천왕 쌍 우선 배정
 *   - 동속성 없으면 사천왕의 이전 대상 스탯 값이 높은 명왕 우선
 *   - 미배정 명왕 → 주인공·전설장수 fallback
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MyungwangStatTransferCalculator")
class MyungwangStatTransferCalculatorTest {

    @Mock
    private MercenaryCharacteristicRepository characteristicRepository;

    @Mock
    private MercenaryCharacteristicLevelRepository levelRepository;

    @InjectMocks
    private MyungwangStatTransferCalculator calculator;

    @Nested
    @DisplayName("computeReceivedTransfers")
    class ComputeReceivedTransfers {

        @Test
        @DisplayName("명왕 1명 → 동속성 사천왕 이전")
        void 단일_동속성_이전() {
            UserDeckMember myungwang = member(1L, "항삼세명왕", MercenaryCategory.MYEONG_KING, Nature.FIRE);
            UserDeckMember king = member(2L, "채염사천왕", MercenaryCategory.FOUR_HEAVENLY_KINGS, Nature.FIRE);

            var result = calculator.computeReceivedTransfers(
                    List.of(myungwang, king), Map.of(), Map.of(),
                    Map.of(1L, Map.of(StatType.STRENGTH, 1000)));

            // 화명왕 기본 이전율 10% → 1000 × 10% = 100
            assertThat(result.receivedByMemberId().get(2L)).containsEntry(StatType.STRENGTH, 100);
            assertThat(result.receivedByMemberId()).doesNotContainKey(1L);
        }

        @Test
        @DisplayName("부동명왕(EARTH)은 이전 없음")
        void 부동명왕_이전_없음() {
            UserDeckMember myungwang = member(1L, "부동명왕", MercenaryCategory.MYEONG_KING, Nature.EARTH);
            UserDeckMember king = member(2L, "사천왕", MercenaryCategory.FOUR_HEAVENLY_KINGS, Nature.FIRE);

            var result = calculator.computeReceivedTransfers(
                    List.of(myungwang, king), Map.of(), Map.of(),
                    Map.of(1L, Map.of(StatType.STRENGTH, 5000)));

            assertThat(result.receivedByMemberId()).isEmpty();
        }

        @Test
        @DisplayName("동속성 명왕이 동속성 사천왕에게 우선 배정 — 다른 속성 사천왕은 남은 명왕 배정")
        void 동속성_우선_배정() {
            // 화명왕(FIRE) + 뇌명왕(THUNDER), 화사천왕(FIRE) + 뇌사천왕(THUNDER)
            UserDeckMember fireMw = member(1L, "화명왕", MercenaryCategory.MYEONG_KING, Nature.FIRE);
            UserDeckMember thunderMw = member(2L, "뇌명왕", MercenaryCategory.MYEONG_KING, Nature.THUNDER);
            UserDeckMember fireKing = member(3L, "화사천왕", MercenaryCategory.FOUR_HEAVENLY_KINGS, Nature.FIRE);
            UserDeckMember thunderKing = member(4L, "뇌사천왕", MercenaryCategory.FOUR_HEAVENLY_KINGS, Nature.THUNDER);

            var result = calculator.computeReceivedTransfers(
                    List.of(fireMw, thunderMw, fireKing, thunderKing), Map.of(), Map.of(),
                    Map.of(
                            1L, Map.of(StatType.STRENGTH, 1000),  // 화명왕 STR
                            2L, Map.of(StatType.DEXTERITY, 800)   // 뇌명왕 DEX
                    ));

            // 화명왕 → 화사천왕(STR 100), 뇌명왕 → 뇌사천왕(DEX 80)
            assertThat(result.receivedByMemberId().get(3L)).containsEntry(StatType.STRENGTH, 100);
            assertThat(result.receivedByMemberId().get(4L)).containsEntry(StatType.DEXTERITY, 80);
        }

        @Test
        @DisplayName("동속성 없을 때 사천왕 스탯이 높은 명왕 우선 배정")
        void 비동속성_스탯_기준_배정() {
            // 화명왕(STR 이전), 뇌명왕(DEX 이전), 수사천왕 — 수사천왕의 DEX > STR
            UserDeckMember fireMw = member(1L, "화명왕", MercenaryCategory.MYEONG_KING, Nature.FIRE);
            UserDeckMember thunderMw = member(2L, "뇌명왕", MercenaryCategory.MYEONG_KING, Nature.THUNDER);
            UserDeckMember waterKing = member(3L, "수사천왕", MercenaryCategory.FOUR_HEAVENLY_KINGS, Nature.WATER);

            var result = calculator.computeReceivedTransfers(
                    List.of(fireMw, thunderMw, waterKing), Map.of(), Map.of(),
                    Map.of(
                            1L, Map.of(StatType.STRENGTH, 1000),
                            2L, Map.of(StatType.DEXTERITY, 900),
                            3L, Map.of(StatType.STRENGTH, 200, StatType.DEXTERITY, 500) // DEX 높음
                    ));

            // 수사천왕 DEX(500) > STR(200) → 뇌명왕(DEX 이전) 배정
            assertThat(result.receivedByMemberId().get(3L)).containsKey(StatType.DEXTERITY);
        }

        @Test
        @DisplayName("사천왕 미배정 명왕은 주인공에게 fallback")
        void 사천왕_없으면_주인공_fallback() {
            UserDeckMember myungwang = member(1L, "화명왕", MercenaryCategory.MYEONG_KING, Nature.FIRE);
            UserDeckMember protagonist = member(2L, "주인공", MercenaryCategory.PROTAGONIST, Nature.WATER);

            var result = calculator.computeReceivedTransfers(
                    List.of(myungwang, protagonist), Map.of(), Map.of(),
                    Map.of(1L, Map.of(StatType.STRENGTH, 1000)));

            assertThat(result.receivedByMemberId().get(2L)).containsEntry(StatType.STRENGTH, 100);
        }

        @Test
        @DisplayName("2명왕 1사천왕 — 동속성 명왕 우선, 나머지 명왕은 주인공에게")
        void 두명왕_한사천왕() {
            UserDeckMember fireMw = member(1L, "화명왕", MercenaryCategory.MYEONG_KING, Nature.FIRE);
            UserDeckMember thunderMw = member(2L, "뇌명왕", MercenaryCategory.MYEONG_KING, Nature.THUNDER);
            UserDeckMember fireKing = member(3L, "화사천왕", MercenaryCategory.FOUR_HEAVENLY_KINGS, Nature.FIRE);
            UserDeckMember protagonist = member(4L, "주인공", MercenaryCategory.PROTAGONIST, Nature.WATER);

            var result = calculator.computeReceivedTransfers(
                    List.of(fireMw, thunderMw, fireKing, protagonist), Map.of(), Map.of(),
                    Map.of(
                            1L, Map.of(StatType.STRENGTH, 1000),
                            2L, Map.of(StatType.DEXTERITY, 800)
                    ));

            // 화명왕 → 화사천왕, 뇌명왕 → 주인공
            assertThat(result.receivedByMemberId().get(3L)).containsEntry(StatType.STRENGTH, 100);
            assertThat(result.receivedByMemberId().get(4L)).containsEntry(StatType.DEXTERITY, 80);
        }
    }

    private static UserDeckMember member(Long id, String name, MercenaryCategory category, Nature nature) {
        UserDeckMember m = UserDeckMember.builder()
                .deck(null)
                .mercenary(mercenary(name, category, nature))
                .build();
        setMemberId(m, id);
        return m;
    }

    private static Mercenary mercenary(String name, MercenaryCategory category, Nature nature) {
        return Mercenary.builder().name(name).category(category).nature(nature).build();
    }

    private static void setMemberId(UserDeckMember member, Long id) {
        try {
            var field = UserDeckMember.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(member, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
