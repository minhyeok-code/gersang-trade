package org.example.gersangtrade.user.util;

import org.example.gersangtrade.domain.user.enums.GradeLevel;

/**
 * totalExp → 등급(GradeLevel) + 호봉(gradeStep) 계산 유틸리티.
 *
 * 등급 진입 기준 (누적 EXP):
 *   행상: 0 이상  (3패, 패당 50 EXP)
 *   보상: 150 이상 (5패, 패당 150 EXP)
 *   객상: 900 이상 (7좌, 좌당 400 EXP)
 *   대상: 3700 이상 (10방, 방당 1530 EXP)
 *   거상: 20000 이상 (호봉 없음)
 *
 * 호봉 계산: (totalExp - grade.baseExp) / grade.expPerStep + 1 (단, 최대 maxStep 초과 불가)
 *
 * 상세 정책: docs/gersang-grade-policy.md 참고.
 */
public final class ExpGradeCalculator {

    private ExpGradeCalculator() {}

    /**
     * totalExp에 해당하는 등급을 반환한다.
     * GradeLevel.values()는 높은 등급(baseExp 큰 순)으로 선언되어 있으므로
     * 앞에서부터 순회하여 첫 번째 조건 만족 등급을 반환한다.
     *
     * @param totalExp 누적 EXP (0 이상)
     * @return 해당하는 GradeLevel
     */
    public static GradeLevel calculateGrade(long totalExp) {
        for (GradeLevel grade : GradeLevel.values()) {
            if (totalExp >= grade.getBaseExp()) {
                return grade;
            }
        }
        // totalExp가 0 미만인 경우 (정상 케이스에서 발생하지 않음)
        return GradeLevel.HAENGSANG;
    }

    /**
     * totalExp에 해당하는 호봉(step)을 반환한다.
     * 거상(maxStep=0)은 항상 0을 반환한다.
     *
     * @param totalExp 누적 EXP (0 이상)
     * @return 호봉 (1~maxStep, 거상은 0)
     */
    public static int calculateStep(long totalExp) {
        GradeLevel grade = calculateGrade(totalExp);

        // 거상은 호봉 없음
        if (grade.getMaxStep() == 0) {
            return 0;
        }

        long expInGrade = totalExp - grade.getBaseExp();
        int step = (int) (expInGrade / grade.getExpPerStep()) + 1;

        // 최대 호봉 초과 방지 (다음 등급 진입 직전 상태)
        return Math.min(step, grade.getMaxStep());
    }

    /**
     * totalExp에 거래 보상 EXP를 더했을 때의 등급과 호봉을 함께 반환한다.
     * 거래 확정 후 User.applyExp() 호출 전 새 값 계산에 사용한다.
     *
     * @param totalExp  현재 누적 EXP
     * @param expDelta  이번 거래로 획득한 EXP
     * @return [새 등급, 새 호봉]
     */
    public static GradeAndStep calculate(long totalExp, long expDelta) {
        long newTotal = totalExp + expDelta;
        GradeLevel newGrade = calculateGrade(newTotal);
        int newStep = calculateStep(newTotal);
        return new GradeAndStep(newGrade, newStep, newTotal);
    }

    /**
     * 등급 + 호봉 + 새 누적 EXP를 묶은 결과 레코드.
     */
    public record GradeAndStep(GradeLevel grade, int step, long newTotalExp) {}
}
