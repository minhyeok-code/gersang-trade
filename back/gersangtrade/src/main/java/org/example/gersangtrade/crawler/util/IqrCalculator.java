package org.example.gersangtrade.crawler.util;

import java.util.List;
import java.util.Optional;

/**
 * IQR(사분위 범위) 방식 이상치 제거 유틸리티.
 *
 * <p>거래 단가 목록에서 이상치를 제거하고 평균가·최저가를 반환한다.
 *
 * <pre>
 * 1. 오름차순 정렬
 * 2. Q1 (25번째 백분위수), Q3 (75번째 백분위수) 계산
 * 3. IQR = Q3 - Q1
 * 4. 유효 범위: [Q1 - 1.5×IQR, Q3 + 1.5×IQR]
 * 5. 유효 범위 내 거래만 사용
 * 6. 최소 샘플 5건 미달 시 Optional.empty() 반환
 * </pre>
 */
public final class IqrCalculator {

    /** 최소 유효 샘플 수. 이 값 미만이면 집계 결과를 신뢰할 수 없어 저장하지 않는다 */
    public static final int MIN_SAMPLE = 5;

    private IqrCalculator() {}

    /**
     * IQR 방식 이상치 제거 후 집계 결과 반환.
     *
     * @param prices 거래 단가 목록 (비어있거나 null 불가)
     * @return 집계 결과 (avgPrice, minPrice, sampleCount). 최소 샘플 미달 시 Optional.empty()
     */
    public static Optional<Result> calculate(List<Long> prices) {
        if (prices == null || prices.size() < MIN_SAMPLE) {
            return Optional.empty();
        }

        List<Long> sorted = prices.stream().sorted().toList();
        int n = sorted.size();

        double q1 = percentile(sorted, 25);
        double q3 = percentile(sorted, 75);
        double iqr = q3 - q1;
        double lower = q1 - 1.5 * iqr;
        double upper = q3 + 1.5 * iqr;

        List<Long> valid = sorted.stream()
                .filter(p -> p >= lower && p <= upper)
                .toList();

        if (valid.size() < MIN_SAMPLE) {
            return Optional.empty();
        }

        long avg = Math.round(valid.stream().mapToLong(Long::longValue).average().orElse(0));
        long min = valid.get(0);

        return Optional.of(new Result(avg, min, valid.size()));
    }

    /**
     * 백분위수 계산 (선형 보간법).
     *
     * @param sorted 오름차순 정렬된 데이터
     * @param percentile 0~100 백분위수
     */
    private static double percentile(List<Long> sorted, int percentile) {
        int n = sorted.size();
        if (n == 1) return sorted.get(0);

        double rank = (percentile / 100.0) * (n - 1);
        int lower = (int) Math.floor(rank);
        int upper = (int) Math.ceil(rank);

        if (lower == upper) return sorted.get(lower);

        double fraction = rank - lower;
        return sorted.get(lower) + fraction * (sorted.get(upper) - sorted.get(lower));
    }

    /** IQR 집계 결과 */
    public record Result(
            /** 이상치 제거 후 평균가 */
            long avgPrice,
            /** 이상치 제거 후 최저가 */
            long minPrice,
            /** 집계에 사용된 유효 거래 건수 */
            int sampleCount
    ) {}
}
