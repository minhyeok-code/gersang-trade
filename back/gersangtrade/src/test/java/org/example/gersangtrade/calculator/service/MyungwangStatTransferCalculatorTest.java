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
 * 각 명왕의 이전량은 동속성 사천왕 → 주인공 → 전설장수 중 1명에게만 적용된다.
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
    @DisplayName("findTransferTarget")
    class FindTransferTarget {

        @Test
        @DisplayName("동속성 사천왕이 있으면 사천왕 1명만 수신")
        void 동속성_사천왕_우선() {
            UserDeckMember myungwang = member(1L, "항삼세명왕", MercenaryCategory.MYEONG_KING, Nature.FIRE);
            UserDeckMember heavenlyKing = member(2L, "채염사천왕", MercenaryCategory.FOUR_HEAVENLY_KINGS, Nature.FIRE);
            UserDeckMember protagonist = member(3L, "주인공", MercenaryCategory.PROTAGONIST, Nature.FIRE);
            UserDeckMember legend = member(4L, "전설장수", MercenaryCategory.LEGENDARY_GENERAL, Nature.FIRE);

            UserDeckMember target = MyungwangStatTransferCalculator.findTransferTarget(
                    List.of(myungwang, heavenlyKing, protagonist, legend), Nature.FIRE);

            assertThat(target.getId()).isEqualTo(2L);
        }

        @Test
        @DisplayName("사천왕 없으면 동속성 주인공 수신")
        void 사천왕_없으면_주인공() {
            UserDeckMember myungwang = member(1L, "항삼세명왕", MercenaryCategory.MYEONG_KING, Nature.FIRE);
            UserDeckMember protagonist = member(3L, "주인공", MercenaryCategory.PROTAGONIST, Nature.FIRE);
            UserDeckMember legend = member(4L, "전설장수", MercenaryCategory.LEGENDARY_GENERAL, Nature.FIRE);

            UserDeckMember target = MyungwangStatTransferCalculator.findTransferTarget(
                    List.of(myungwang, protagonist, legend), Nature.FIRE);

            assertThat(target.getId()).isEqualTo(3L);
        }

        @Test
        @DisplayName("사천왕·주인공 없으면 동속성 전설장수 수신")
        void 전설장수_수신() {
            UserDeckMember myungwang = member(1L, "항삼세명왕", MercenaryCategory.MYEONG_KING, Nature.FIRE);
            UserDeckMember legend = member(4L, "전설장수", MercenaryCategory.LEGENDARY_GENERAL, Nature.FIRE);

            UserDeckMember target = MyungwangStatTransferCalculator.findTransferTarget(
                    List.of(myungwang, legend), Nature.FIRE);

            assertThat(target.getId()).isEqualTo(4L);
        }

        @Test
        @DisplayName("다른 속성 사천왕은 대상에서 제외")
        void 다른_속성_제외() {
            UserDeckMember myungwang = member(1L, "항삼세명왕", MercenaryCategory.MYEONG_KING, Nature.FIRE);
            UserDeckMember waterKing = member(2L, "수속 사천왕", MercenaryCategory.FOUR_HEAVENLY_KINGS, Nature.WATER);
            UserDeckMember fireLegend = member(4L, "화속 전설", MercenaryCategory.LEGENDARY_GENERAL, Nature.FIRE);

            UserDeckMember target = MyungwangStatTransferCalculator.findTransferTarget(
                    List.of(myungwang, waterKing, fireLegend), Nature.FIRE);

            assertThat(target.getId()).isEqualTo(4L);
        }

        @Test
        @DisplayName("대상 없으면 null")
        void 대상_없음() {
            UserDeckMember myungwang = member(1L, "항삼세명왕", MercenaryCategory.MYEONG_KING, Nature.FIRE);

            UserDeckMember target = MyungwangStatTransferCalculator.findTransferTarget(
                    List.of(myungwang), Nature.FIRE);

            assertThat(target).isNull();
        }
    }

    @Nested
    @DisplayName("computeReceivedTransfers")
    class ComputeReceivedTransfers {

        @Test
        @DisplayName("명왕 1명의 이전량은 수신 멤버 1명에만 합산")
        void 단일_수신자() {
            Mercenary myungwangMerc = mercenary("항삼세명왕", MercenaryCategory.MYEONG_KING, Nature.FIRE);
            Mercenary heavenlyMerc = mercenary("채염사천왕", MercenaryCategory.FOUR_HEAVENLY_KINGS, Nature.FIRE);

            UserDeckMember myungwang = UserDeckMember.builder().deck(null).mercenary(myungwangMerc).build();
            UserDeckMember heavenlyKing = UserDeckMember.builder().deck(null).mercenary(heavenlyMerc).build();
            setMemberId(myungwang, 1L);
            setMemberId(heavenlyKing, 2L);

            Map<Long, Map<StatType, Integer>> preTransfer = Map.of(
                    1L, Map.of(StatType.STRENGTH, 1000));

            MyungwangStatTransferCalculator.ComputedTransfers result =
                    calculator.computeReceivedTransfers(
                            List.of(myungwang, heavenlyKing),
                            Map.of(),
                            Map.of(),
                            preTransfer);

            // 화명왕 기본 이전율 10% → 1000 × 10% = 100
            assertThat(result.receivedByMemberId()).containsKey(2L);
            assertThat(result.receivedByMemberId().get(2L)).containsEntry(StatType.STRENGTH, 100);
            assertThat(result.receivedByMemberId()).doesNotContainKey(1L);
            assertThat(result.detailsByMemberId().get(2L)).hasSize(1);
            assertThat(result.detailsByMemberId().get(2L).get(0).sourceMercenaryName()).isEqualTo("항삼세명왕");
        }

        @Test
        @DisplayName("부동명왕(EARTH)은 이전 없음")
        void 부동명왕_이전_없음() {
            Mercenary earthMyungwang = mercenary("부동명왕", MercenaryCategory.MYEONG_KING, Nature.EARTH);
            Mercenary heavenlyMerc = mercenary("사천왕", MercenaryCategory.FOUR_HEAVENLY_KINGS, Nature.EARTH);

            UserDeckMember myungwang = UserDeckMember.builder().deck(null).mercenary(earthMyungwang).build();
            UserDeckMember heavenlyKing = UserDeckMember.builder().deck(null).mercenary(heavenlyMerc).build();
            setMemberId(myungwang, 1L);
            setMemberId(heavenlyKing, 2L);

            MyungwangStatTransferCalculator.ComputedTransfers result =
                    calculator.computeReceivedTransfers(
                            List.of(myungwang, heavenlyKing),
                            Map.of(),
                            Map.of(),
                            Map.of(1L, Map.of(StatType.STRENGTH, 5000)));

            assertThat(result.receivedByMemberId()).isEmpty();
            assertThat(result.detailsByMemberId()).isEmpty();
        }
    }

    private static UserDeckMember member(Long id, String name, MercenaryCategory category, Nature nature) {
        UserDeckMember member = UserDeckMember.builder()
                .deck(null)
                .mercenary(mercenary(name, category, nature))
                .build();
        setMemberId(member, id);
        return member;
    }

    private static Mercenary mercenary(String name, MercenaryCategory category, Nature nature) {
        return Mercenary.builder()
                .name(name)
                .category(category)
                .nature(nature)
                .build();
    }

    /** 테스트용 member id 주입 */
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
