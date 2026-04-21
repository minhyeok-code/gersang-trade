package org.example.gersangtrade.domain.user.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 사용자 등급 — 전통 상인 직함 기반 5단계.
 * EXP 계산: values()가 높은 등급(낮은 expBase) 순으로 선언되어 있어
 * ExpGradeCalculator에서 역순 탐색 없이 앞에서부터 비교한다.
 *
 * 상세 정책: docs/gersang-grade-policy.md 참고.
 */
@Getter
@RequiredArgsConstructor
public enum GradeLevel {

    /** 거상(巨商) — 1등급, 누적 20,000 EXP 진입. 호봉 없음 */
    GEOSANG(1, "거상", null, 0, 0, 20_000L),

    /** 대상(大商) — 2등급, 10방 × 1,530 EXP */
    DAESANG(2, "대상", "방", 10, 1_530, 3_700L),

    /** 객상(客商) — 3등급, 7좌 × 400 EXP */
    GAEKSANG(3, "객상", "좌", 7, 400, 900L),

    /** 보상(褓商) — 4등급, 5패 × 150 EXP */
    BOSANG(4, "보상", "패", 5, 150, 150L),

    /** 행상(行商) — 5등급(신규 가입), 3패 × 50 EXP */
    HAENGSANG(5, "행상", "패", 3, 50, 0L);

    /** 등급 번호 (1이 최고) */
    private final int gradeNumber;

    /** 등급 한국어 표시명 */
    private final String displayName;

    /** 호봉 단위 (거상은 null) */
    private final String stepUnit;

    /** 최대 호봉 수 (거상은 0) */
    private final int maxStep;

    /** 호봉당 필요 EXP (거상은 0) */
    private final int expPerStep;

    /** 해당 등급 진입에 필요한 누적 EXP */
    private final long baseExp;
}
