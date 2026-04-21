package org.example.gersangtrade.domain.trade.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 거래 평가 선택지.
 * 3일 블라인드 평가 만료 후 EXP·매너점수에 일괄 반영된다.
 *
 * 상세 정책: docs/gersang-grade-policy.md 4-3절 참고.
 */
@Getter
@RequiredArgsConstructor
public enum TradeRating {

    /** 👍 좋음 — +15 EXP, 매너점수 +2 */
    GOOD(15, 2),

    /** 😐 보통 — EXP·매너점수 변화 없음 */
    NEUTRAL(0, 0),

    /** 👎 나쁨 — -20 EXP, 매너점수 -3 */
    BAD(-20, -3);

    /** 지급할 EXP (음수면 차감) */
    private final int expDelta;

    /** 적용할 매너점수 변화량 (음수면 차감) */
    private final int mannerScoreDelta;
}
