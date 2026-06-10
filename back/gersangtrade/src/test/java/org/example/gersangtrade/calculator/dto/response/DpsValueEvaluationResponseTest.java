package org.example.gersangtrade.calculator.dto.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

/**
 * DpsValueEvaluationResponse 단위 테스트.
 * ofTransient·ofPersisted 팩토리 메서드의 delta·increaseRate·efficiency·formatPrice 계산을 검증한다.
 */
class DpsValueEvaluationResponseTest {

    // ── 가격 포맷 ──────────────────────────────────────────────────────────────

    @ParameterizedTest(name = "{0}전 → \"{1}\"")
    @CsvSource({
            "100000000,   1억",
            "150000000,   1억 5천만",
            "50000000,    5천만",
            "5000000,     5000000전",
            "300000000,   3억",
            "250000000,   2억 5천만",
    })
    @DisplayName("formatPrice_단위별_포맷_검증")
    void formatPrice_단위별_포맷_검증(long price, String expected) {
        DpsResponse before = makeDps(1_000L, 1_000L, 1_000L);
        DpsResponse after  = makeDps(1_000L, 1_000L, 1_000L);

        DpsValueEvaluationResponse resp = DpsValueEvaluationResponse.ofTransient(
                before, after, price, PriceSource.USER_INPUT, null);

        assertThat(resp.formattedPrice()).isEqualTo(expected.trim());
    }

    @Test
    @DisplayName("formatPrice_null_입력_null_반환")
    void formatPrice_null_입력_null_반환() {
        DpsResponse dps = makeDps(1_000L, 1_000L, 1_000L);
        DpsValueEvaluationResponse resp = DpsValueEvaluationResponse.ofTransient(
                dps, dps, null, PriceSource.MISSING, null);

        assertThat(resp.formattedPrice()).isNull();
        assertThat(resp.price()).isNull();
    }

    // ── delta / increaseRate ──────────────────────────────────────────────────

    @Test
    @DisplayName("delta_정상계산")
    void delta_정상계산() {
        DpsResponse before = makeDps(10_000L, 8_000L, 3_000L);
        DpsResponse after  = makeDps(12_000L, 9_000L, 3_600L);

        DpsValueEvaluationResponse resp = DpsValueEvaluationResponse.ofTransient(
                before, after, 100_000_000L, PriceSource.USER_INPUT, null);

        assertThat(resp.delta().raw()).isEqualTo(2_000L);
        assertThat(resp.delta().adjust()).isEqualTo(1_000L);
        assertThat(resp.delta().finalDps()).isEqualTo(600L);
    }

    @Test
    @DisplayName("increaseRate_raw_20percent_정상계산")
    void increaseRate_raw_20percent_정상계산() {
        // raw: 10000 → 12000 = +20.0%
        DpsResponse before = makeDps(10_000L, 8_000L, 3_000L);
        DpsResponse after  = makeDps(12_000L, 8_000L, 3_000L);

        DpsValueEvaluationResponse resp = DpsValueEvaluationResponse.ofTransient(
                before, after, 100_000_000L, PriceSource.USER_INPUT, null);

        assertThat(resp.increaseRate().raw()).isCloseTo(20.0, offset(0.01));
    }

    @Test
    @DisplayName("increaseRate_before_0이면_0반환")
    void increaseRate_before_0이면_0반환() {
        DpsResponse before = makeDps(0L, 0L, 0L);
        DpsResponse after  = makeDps(5_000L, 5_000L, 5_000L);

        DpsValueEvaluationResponse resp = DpsValueEvaluationResponse.ofTransient(
                before, after, 100_000_000L, PriceSource.USER_INPUT, null);

        assertThat(resp.increaseRate().raw()).isEqualTo(0.0);
        assertThat(resp.increaseRate().adjust()).isEqualTo(0.0);
        assertThat(resp.increaseRate().finalDps()).isEqualTo(0.0);
    }

    // ── efficiency ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("efficiency_1억에_10percent상승_10점")
    void efficiency_1억에_10percent상승_10점() {
        // raw: 10000 → 11000 = +10%, price = 1억 → efficiency = 10.0 / 1.0 = 10.0
        DpsResponse before = makeDps(10_000L, 10_000L, 10_000L);
        DpsResponse after  = makeDps(11_000L, 11_000L, 11_000L);

        DpsValueEvaluationResponse resp = DpsValueEvaluationResponse.ofTransient(
                before, after, 100_000_000L, PriceSource.USER_INPUT, null);

        assertThat(resp.efficiencyPerEok().raw()).isCloseTo(10.0, offset(0.01));
        assertThat(resp.efficiencyPerEok().adjust()).isCloseTo(10.0, offset(0.01));
        assertThat(resp.efficiencyPerEok().finalDps()).isCloseTo(10.0, offset(0.01));
    }

    @Test
    @DisplayName("efficiency_price_null_null반환")
    void efficiency_price_null_null반환() {
        DpsResponse before = makeDps(10_000L, 10_000L, 10_000L);
        DpsResponse after  = makeDps(11_000L, 11_000L, 11_000L);

        DpsValueEvaluationResponse resp = DpsValueEvaluationResponse.ofTransient(
                before, after, null, PriceSource.MISSING, null);

        assertThat(resp.efficiencyPerEok().raw()).isNull();
        assertThat(resp.efficiencyPerEok().adjust()).isNull();
        assertThat(resp.efficiencyPerEok().finalDps()).isNull();
    }

    @Test
    @DisplayName("efficiency_price_0_null반환")
    void efficiency_price_0_null반환() {
        DpsResponse before = makeDps(10_000L, 10_000L, 10_000L);
        DpsResponse after  = makeDps(11_000L, 11_000L, 11_000L);

        DpsValueEvaluationResponse resp = DpsValueEvaluationResponse.ofTransient(
                before, after, 0L, PriceSource.USER_INPUT, null);

        assertThat(resp.efficiencyPerEok().raw()).isNull();
    }

    // ── persist 플래그 ────────────────────────────────────────────────────────

    @Test
    @DisplayName("ofTransient_persisted_false_evaluationId_null")
    void ofTransient_persisted_false_evaluationId_null() {
        DpsResponse dps = makeDps(1_000L, 1_000L, 1_000L);

        DpsValueEvaluationResponse resp = DpsValueEvaluationResponse.ofTransient(
                dps, dps, 100_000_000L, PriceSource.TRADE_STAT, 5);

        assertThat(resp.persisted()).isFalse();
        assertThat(resp.evaluationId()).isNull();
        assertThat(resp.scenarioDeckSnapshotId()).isNull();
        assertThat(resp.tradeCount()).isEqualTo(5);
        assertThat(resp.priceSource()).isEqualTo(PriceSource.TRADE_STAT);
    }

    // ── 헬퍼 ─────────────────────────────────────────────────────────────────

    private DpsResponse makeDps(long raw, long adjust, long total) {
        return new DpsResponse(1L, "몬스터", 0, 0, 43.0, 0, 0,
                raw, adjust, total, List.of());
    }
}
