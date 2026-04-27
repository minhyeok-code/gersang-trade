package org.example.gersangtrade.common;

import java.math.BigDecimal;

/**
 * 거상 화폐(전) 금액 표기 포매터.
 *
 * <p>표기 기준:
 * <ul>
 *   <li>십만 단위 반올림 후 표기</li>
 *   <li>1,000만(0.1억) 이상 → 억 단위. 후행 0 제거. 예: "0.15억", "1억", "1.5억", "10억"</li>
 *   <li>1,000만 미만 → 만 단위. 예: "90만", "500만", "999만"</li>
 * </ul>
 */
public final class GoldFormatter {

    private static final long SIPMAN   = 100_000L;       // 십만 (반올림 단위)
    private static final long CHEONMAN = 10_000_000L;    // 천만 = 0.1억
    private static final long EOK      = 100_000_000L;   // 1억
    private static final long MAN      = 10_000L;        // 1만

    private GoldFormatter() {}

    /**
     * 전(錢) 금액을 한국식 표기 문자열로 변환한다.
     *
     * @param amount 전 단위 금액
     * @return 포맷된 문자열 (예: "0.15억", "1.5억", "500만")
     */
    public static String format(long amount) {
        // 십만 단위 반올림
        long rounded = Math.round(amount / (double) SIPMAN) * SIPMAN;

        if (rounded >= CHEONMAN) {
            // 후행 0 제거: 1억 → "1", 1.50억 → "1.5", 0.15억 → "0.15"
            BigDecimal eok = BigDecimal.valueOf(rounded)
                    .divide(BigDecimal.valueOf(EOK))
                    .stripTrailingZeros();
            return eok.toPlainString() + "억";
        }

        if (rounded >= MAN) {
            return (rounded / MAN) + "만";
        }

        return rounded + "전";
    }

    /** null-safe 버전. null이면 "-" 반환 */
    public static String format(Long amount) {
        if (amount == null) return "-";
        return format(amount.longValue());
    }
}
