package org.example.gersangtrade.user.util;

import org.example.gersangtrade.domain.user.enums.GradeLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ExpGradeCalculator 단위 테스트.
 * 등급 진입 경계값, 호봉 최대값, 음수 EXP 처리 케이스를 검증한다.
 */
@DisplayName("ExpGradeCalculator")
class ExpGradeCalculatorTest {

    // ──────────────────────────────────────────────────────────────────────
    // calculateGrade
    // ──────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("calculateGrade — 등급 경계값")
    class CalculateGrade {

        @Test
        @DisplayName("0 EXP → 행상")
        void exp0_haengsang() {
            assertThat(ExpGradeCalculator.calculateGrade(0)).isEqualTo(GradeLevel.HAENGSANG);
        }

        @Test
        @DisplayName("149 EXP → 행상 (보상 진입 직전)")
        void exp149_haengsang() {
            assertThat(ExpGradeCalculator.calculateGrade(149)).isEqualTo(GradeLevel.HAENGSANG);
        }

        @Test
        @DisplayName("150 EXP → 보상 (진입 경계)")
        void exp150_bosang() {
            assertThat(ExpGradeCalculator.calculateGrade(150)).isEqualTo(GradeLevel.BOSANG);
        }

        @Test
        @DisplayName("899 EXP → 보상 (객상 진입 직전)")
        void exp899_bosang() {
            assertThat(ExpGradeCalculator.calculateGrade(899)).isEqualTo(GradeLevel.BOSANG);
        }

        @Test
        @DisplayName("900 EXP → 객상 (진입 경계)")
        void exp900_gaeksang() {
            assertThat(ExpGradeCalculator.calculateGrade(900)).isEqualTo(GradeLevel.GAEKSANG);
        }

        @Test
        @DisplayName("3699 EXP → 객상 (대상 진입 직전)")
        void exp3699_gaeksang() {
            assertThat(ExpGradeCalculator.calculateGrade(3699)).isEqualTo(GradeLevel.GAEKSANG);
        }

        @Test
        @DisplayName("3700 EXP → 대상 (진입 경계)")
        void exp3700_daesang() {
            assertThat(ExpGradeCalculator.calculateGrade(3700)).isEqualTo(GradeLevel.DAESANG);
        }

        @Test
        @DisplayName("19999 EXP → 대상 (거상 진입 직전)")
        void exp19999_daesang() {
            assertThat(ExpGradeCalculator.calculateGrade(19999)).isEqualTo(GradeLevel.DAESANG);
        }

        @Test
        @DisplayName("20000 EXP → 거상 (진입 경계)")
        void exp20000_geosang() {
            assertThat(ExpGradeCalculator.calculateGrade(20000)).isEqualTo(GradeLevel.GEOSANG);
        }

        @Test
        @DisplayName("매우 큰 EXP → 거상")
        void veryLargeExp_geosang() {
            assertThat(ExpGradeCalculator.calculateGrade(Long.MAX_VALUE)).isEqualTo(GradeLevel.GEOSANG);
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // calculateStep — 호봉 경계값
    // ──────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("calculateStep — 호봉 경계값")
    class CalculateStep {

        @Test
        @DisplayName("0 EXP → 행상 1호봉")
        void haengsang_step1() {
            assertThat(ExpGradeCalculator.calculateStep(0)).isEqualTo(1);
        }

        @Test
        @DisplayName("49 EXP → 행상 1호봉 (2호봉 진입 직전)")
        void haengsang_step1_max() {
            assertThat(ExpGradeCalculator.calculateStep(49)).isEqualTo(1);
        }

        @Test
        @DisplayName("50 EXP → 행상 2호봉")
        void haengsang_step2() {
            assertThat(ExpGradeCalculator.calculateStep(50)).isEqualTo(2);
        }

        @Test
        @DisplayName("100 EXP → 행상 3호봉 (최대)")
        void haengsang_step3_max() {
            // 행상 baseExp=0, expPerStep=50, maxStep=3
            // step = (100 - 0) / 50 + 1 = 3
            assertThat(ExpGradeCalculator.calculateStep(100)).isEqualTo(3);
        }

        @Test
        @DisplayName("149 EXP → 행상 3호봉 (maxStep 초과 클램핑)")
        void haengsang_step_clamped() {
            // step = (149 - 0) / 50 + 1 = 3 (클램핑)
            assertThat(ExpGradeCalculator.calculateStep(149)).isEqualTo(3);
        }

        @Test
        @DisplayName("150 EXP → 보상 1호봉 (등급 진입)")
        void bosang_step1() {
            assertThat(ExpGradeCalculator.calculateStep(150)).isEqualTo(1);
        }

        @Test
        @DisplayName("보상 최대 호봉 (5호봉) 경계 — 750 EXP")
        void bosang_step5() {
            // 보상 baseExp=150, expPerStep=150, maxStep=5
            // step = (750 - 150) / 150 + 1 = 5
            assertThat(ExpGradeCalculator.calculateStep(750)).isEqualTo(5);
        }

        @Test
        @DisplayName("객상 1호봉 — 900 EXP")
        void gaeksang_step1() {
            assertThat(ExpGradeCalculator.calculateStep(900)).isEqualTo(1);
        }

        @Test
        @DisplayName("객상 최대 호봉 (7좌) 경계 — 3300 EXP")
        void gaeksang_step7() {
            // 객상 baseExp=900, expPerStep=400, maxStep=7
            // step = (3300 - 900) / 400 + 1 = 7
            assertThat(ExpGradeCalculator.calculateStep(3300)).isEqualTo(7);
        }

        @Test
        @DisplayName("대상 최대 호봉 (10방) 경계 — 17470 EXP")
        void daesang_step10() {
            // 대상 baseExp=3700, expPerStep=1530, maxStep=10
            // step = (17470 - 3700) / 1530 + 1 = 10
            assertThat(ExpGradeCalculator.calculateStep(17470)).isEqualTo(10);
        }

        @Test
        @DisplayName("거상 → 0 반환 (호봉 없음)")
        void geosang_step0() {
            assertThat(ExpGradeCalculator.calculateStep(20000)).isEqualTo(0);
        }

        @Test
        @DisplayName("거상 매우 큰 EXP → 0 반환")
        void geosang_veryLarge_step0() {
            assertThat(ExpGradeCalculator.calculateStep(999_999_999L)).isEqualTo(0);
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // calculate — 복합 결과 레코드
    // ──────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("calculate — 등급+호봉+누적EXP 통합")
    class Calculate {

        @Test
        @DisplayName("행상 1호봉에서 +50 EXP → 행상 2호봉")
        void haengsang_levelUp_step() {
            ExpGradeCalculator.GradeAndStep result = ExpGradeCalculator.calculate(0, 50);
            assertThat(result.grade()).isEqualTo(GradeLevel.HAENGSANG);
            assertThat(result.step()).isEqualTo(2);
            assertThat(result.newTotalExp()).isEqualTo(50);
        }

        @Test
        @DisplayName("행상 3호봉(149 EXP)에서 +1 EXP → 보상 1호봉 등급 상승")
        void haengsang_to_bosang() {
            ExpGradeCalculator.GradeAndStep result = ExpGradeCalculator.calculate(149, 1);
            assertThat(result.grade()).isEqualTo(GradeLevel.BOSANG);
            assertThat(result.step()).isEqualTo(1);
            assertThat(result.newTotalExp()).isEqualTo(150);
        }

        @Test
        @DisplayName("대상 마지막 경계(19999 EXP)에서 +1 EXP → 거상 (호봉 0)")
        void daesang_to_geosang() {
            ExpGradeCalculator.GradeAndStep result = ExpGradeCalculator.calculate(19999, 1);
            assertThat(result.grade()).isEqualTo(GradeLevel.GEOSANG);
            assertThat(result.step()).isEqualTo(0);
            assertThat(result.newTotalExp()).isEqualTo(20000);
        }

        @ParameterizedTest(name = "totalExp={0}, delta={1} → grade={2}, step={3}")
        @CsvSource({
            "0,   0,    HAENGSANG, 1",
            "0,   100,  HAENGSANG, 3",
            "150, 750,  GAEKSANG,  1",   // 150+750=900 → 객상 진입
            "900, 2800, DAESANG,   1",   // 900+2800=3700 → 대상 진입
            "3700,16300,GEOSANG,   0"    // 3700+16300=20000 → 거상
        })
        @DisplayName("파라미터화 — 다양한 EXP 조합")
        void parameterized(long totalExp, long delta, GradeLevel expectedGrade, int expectedStep) {
            ExpGradeCalculator.GradeAndStep result = ExpGradeCalculator.calculate(totalExp, delta);
            assertThat(result.grade()).isEqualTo(expectedGrade);
            assertThat(result.step()).isEqualTo(expectedStep);
        }
    }
}
