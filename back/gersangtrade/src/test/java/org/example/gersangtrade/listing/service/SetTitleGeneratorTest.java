package org.example.gersangtrade.listing.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SetTitleGenerator 단위 테스트.
 * 세트 표기 문자열 생성 규칙을 케이스별로 검증한다.
 */
class SetTitleGeneratorTest {

    // ── 전체 동일 주술 ────────────────────────────────────────────────────────

    @Test
    @DisplayName("5피스_전체_동일마크_풀마크세트명형식")
    void 전체동일주술_풀마크세트명형식() {
        List<String> marks = List.of("**", "**", "**", "**", "**");

        String title = SetTitleGenerator.generate("00세트", marks);

        assertThat(title).isEqualTo("풀 ** 00세트");
    }

    @Test
    @DisplayName("5피스_전체_성공마크_풀마크세트명형식")
    void 전체동일주술_성공마크() {
        List<String> marks = List.of("00", "00", "00", "00", "00");

        String title = SetTitleGenerator.generate("XX세트", marks);

        assertThat(title).isEqualTo("풀 00 XX세트");
    }

    @Test
    @DisplayName("1피스_전체_동일마크_풀마크세트명형식")
    void 단일피스_전체동일주술() {
        List<String> marks = List.of("**");

        String title = SetTitleGenerator.generate("00세트", marks);

        assertThat(title).isEqualTo("풀 ** 00세트");
    }

    // ── 부분 주술 ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("5피스중3피스_주술있음_풀세트명피스수마크형식")
    void 부분주술_3피스() {
        List<String> marks = Arrays.asList("**", "**", "**", null, null);

        String title = SetTitleGenerator.generate("00세트", marks);

        assertThat(title).isEqualTo("풀 00세트 3**");
    }

    @Test
    @DisplayName("5피스중1피스_주술있음_풀세트명피스수마크형식")
    void 부분주술_1피스() {
        List<String> marks = Arrays.asList("00", null, null, null, null);

        String title = SetTitleGenerator.generate("XX세트", marks);

        assertThat(title).isEqualTo("풀 XX세트 100");
    }

    @Test
    @DisplayName("5피스중4피스_주술있음_풀세트명피스수마크형식")
    void 부분주술_4피스() {
        List<String> marks = Arrays.asList("**", "**", "**", "**", null);

        String title = SetTitleGenerator.generate("00세트", marks);

        assertThat(title).isEqualTo("풀 00세트 4**");
    }

    // ── 주술 없음 ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("5피스_주술없음_풀세트명형식")
    void 주술없음_전체null() {
        List<String> marks = Arrays.asList(null, null, null, null, null);

        String title = SetTitleGenerator.generate("00세트", marks);

        assertThat(title).isEqualTo("풀 00세트");
    }

    @Test
    @DisplayName("빈목록_주술없음_풀세트명형식")
    void 주술없음_빈목록() {
        List<String> marks = List.of();

        String title = SetTitleGenerator.generate("00세트", marks);

        assertThat(title).isEqualTo("풀 00세트");
    }

    // ── 혼재 마크 (폴백) ──────────────────────────────────────────────────────

    @Test
    @DisplayName("마크혼재_다른종류_폴백세트명형식")
    void 혼재마크_폴백() {
        List<String> marks = Arrays.asList("**", "00", "**", null, null);

        String title = SetTitleGenerator.generate("00세트", marks);

        assertThat(title).isEqualTo("풀 00세트");
    }

    @Test
    @DisplayName("마크2종류_전체혼재_폴백세트명형식")
    void 전체혼재마크_폴백() {
        List<String> marks = List.of("**", "00", "**", "00", "**");

        String title = SetTitleGenerator.generate("XX세트", marks);

        assertThat(title).isEqualTo("풀 XX세트");
    }
}
