package org.example.gersangtrade.listing.service;

import org.example.gersangtrade.domain.catalog.enums.EquipmentSlot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SetTitleGenerator 단위 테스트.
 * 세트 표기 문자열 생성 규칙을 케이스별로 검증한다.
 */
class SetTitleGeneratorTest {

    private static SetTitleGenerator.PieceTitleInput p(EquipmentSlot slot, String mark) {
        return new SetTitleGenerator.PieceTitleInput(slot, mark);
    }

    // ── 풀 5피스 ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("5피스_전체_동일마크")
    void 전체동일주술_5피스() {
        List<SetTitleGenerator.PieceTitleInput> pieces = List.of(
                p(EquipmentSlot.HELMET, "<개양>"),
                p(EquipmentSlot.ARMOR, "<개양>"),
                p(EquipmentSlot.GLOVES, "<개양>"),
                p(EquipmentSlot.BELT, "<개양>"),
                p(EquipmentSlot.SHOES, "<개양>"));

        assertThat(SetTitleGenerator.generate("각성광목천왕", pieces))
                .isEqualTo("5<개양> 풀 각성광목천왕");
    }

    @Test
    @DisplayName("5피스_변두리만_주술_3개")
    void 변두리만_주술_3개() {
        List<SetTitleGenerator.PieceTitleInput> pieces = List.of(
                p(EquipmentSlot.HELMET, null),
                p(EquipmentSlot.ARMOR, null),
                p(EquipmentSlot.GLOVES, "<북두칠성_개양>"),
                p(EquipmentSlot.BELT, "<북두칠성_개양>"),
                p(EquipmentSlot.SHOES, "<북두칠성_개양>"));

        assertThat(SetTitleGenerator.generate("각성광목천왕", pieces))
                .isEqualTo("3<북두칠성_개양> 풀 각성광목천왕");
    }

    @Test
    @DisplayName("5피스_주술없음")
    void 주술없음_풀() {
        List<SetTitleGenerator.PieceTitleInput> pieces = List.of(
                p(EquipmentSlot.HELMET, null),
                p(EquipmentSlot.ARMOR, null),
                p(EquipmentSlot.GLOVES, null),
                p(EquipmentSlot.BELT, null),
                p(EquipmentSlot.SHOES, null));

        assertThat(SetTitleGenerator.generate("각성광목천왕", pieces))
                .isEqualTo("풀 각성광목천왕");
    }

    // ── 풀반쌍 ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("풀반쌍_5피스_전체주술_반지무주술")
    void 풀반쌍_5피스_주술() {
        List<SetTitleGenerator.PieceTitleInput> pieces = List.of(
                p(EquipmentSlot.HELMET, "<북두칠성_개양>"),
                p(EquipmentSlot.ARMOR, "<북두칠성_개양>"),
                p(EquipmentSlot.GLOVES, "<북두칠성_개양>"),
                p(EquipmentSlot.BELT, "<북두칠성_개양>"),
                p(EquipmentSlot.SHOES, "<북두칠성_개양>"),
                p(EquipmentSlot.RING, null));

        assertThat(SetTitleGenerator.generate("각성광목천왕", pieces))
                .isEqualTo("5<북두칠성_개양> 풀 각성광목천왕반쌍");
    }

    @Test
    @DisplayName("풀반쌍_변두리만_주술")
    void 풀반쌍_변두리_주술() {
        List<SetTitleGenerator.PieceTitleInput> pieces = List.of(
                p(EquipmentSlot.HELMET, null),
                p(EquipmentSlot.ARMOR, null),
                p(EquipmentSlot.GLOVES, "<북두칠성_개양>"),
                p(EquipmentSlot.BELT, "<북두칠성_개양>"),
                p(EquipmentSlot.SHOES, "<북두칠성_개양>"),
                p(EquipmentSlot.RING, null));

        assertThat(SetTitleGenerator.generate("각성광목천왕", pieces))
                .isEqualTo("3<북두칠성_개양> 풀 각성광목천왕반쌍");
    }

    // ── 갑투 / 변 / 반쌍 ─────────────────────────────────────────────────────

    @Test
    @DisplayName("갑투_주술_2개")
    void 갑투_주술_2개() {
        List<SetTitleGenerator.PieceTitleInput> pieces = List.of(
                p(EquipmentSlot.HELMET, "<00>"),
                p(EquipmentSlot.ARMOR, "<00>"));

        assertThat(SetTitleGenerator.generate("각성광목천왕", pieces))
                .isEqualTo("2<00>각성광목천왕갑투");
    }

    @Test
    @DisplayName("갑투_무주술")
    void 갑투_무주술() {
        List<SetTitleGenerator.PieceTitleInput> pieces = List.of(
                p(EquipmentSlot.HELMET, null),
                p(EquipmentSlot.ARMOR, null));

        assertThat(SetTitleGenerator.generate("각성광목천왕", pieces))
                .isEqualTo("각성광목천왕갑투");
    }

    @Test
    @DisplayName("변_주술_3개")
    void 변_주술_3개() {
        List<SetTitleGenerator.PieceTitleInput> pieces = List.of(
                p(EquipmentSlot.GLOVES, "<00>"),
                p(EquipmentSlot.BELT, "<00>"),
                p(EquipmentSlot.SHOES, "<00>"));

        assertThat(SetTitleGenerator.generate("각성광목천왕", pieces))
                .isEqualTo("3<00>변각성광목천왕");
    }

    @Test
    @DisplayName("반쌍_무주술")
    void 반쌍_무주술() {
        List<SetTitleGenerator.PieceTitleInput> pieces = List.of(
                p(EquipmentSlot.RING, null));

        assertThat(SetTitleGenerator.generate("각성광목천왕", pieces))
                .isEqualTo("각성광목천왕반쌍");
    }

    // ── 혼재 마크 (폴백) ───────────────────────────────────────────────────────

    @Test
    @DisplayName("마크혼재_풀_폴백")
    void 혼재마크_풀_폴백() {
        List<SetTitleGenerator.PieceTitleInput> pieces = List.of(
                p(EquipmentSlot.HELMET, "<북두칠성_개양>"),
                p(EquipmentSlot.ARMOR, "<개양>"),
                p(EquipmentSlot.GLOVES, "<북두칠성_개양>"),
                p(EquipmentSlot.BELT, null),
                p(EquipmentSlot.SHOES, null));

        assertThat(SetTitleGenerator.generate("각성광목천왕", pieces))
                .isEqualTo("풀 각성광목천왕");
    }
}
