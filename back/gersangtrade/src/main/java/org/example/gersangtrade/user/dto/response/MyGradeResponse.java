package org.example.gersangtrade.user.dto.response;

import org.example.gersangtrade.domain.user.User;

/**
 * 내 등급·경험치 응답 DTO — 마이페이지 등급 패널용.
 *
 * @param grade           현재 등급 표시명 (행상 / 보상 / 객상 / 대상 / 거상)
 * @param gradeStep       현재 호봉 (거상은 null)
 * @param stepUnit        호봉 단위 (패·좌·방, 거상은 null)
 * @param maxStep         해당 등급 최대 호봉 (거상은 0)
 * @param expPerStep      호봉당 필요 EXP (거상은 0)
 * @param stepProgressExp 현재 호봉 내 누적 EXP (0 ~ expPerStep)
 * @param totalExp        누적 EXP
 * @param mannerScore     매너점수 (0~100)
 * @param tradeCount      거래 완료 횟수
 */
public record MyGradeResponse(
        String grade,
        Integer gradeStep,
        String stepUnit,
        int maxStep,
        int expPerStep,
        int stepProgressExp,
        long totalExp,
        int mannerScore,
        int tradeCount
) {
    public static MyGradeResponse from(User user) {
        var level = user.getGrade();
        long totalExp = user.getTotalExp() != null ? user.getTotalExp() : 0L;
        Integer gradeStep = user.getGradeStep();

        String stepUnit = null;
        int maxStep = 0;
        int expPerStep = 0;
        int stepProgressExp = 0;

        if (level != null) {
            stepUnit = level.getStepUnit();
            maxStep = level.getMaxStep();
            expPerStep = level.getExpPerStep();

            if (maxStep > 0 && gradeStep != null && gradeStep > 0 && expPerStep > 0) {
                long expInGrade = totalExp - level.getBaseExp();
                stepProgressExp = (int) (expInGrade - (long) (gradeStep - 1) * expPerStep);
                stepProgressExp = Math.max(0, Math.min(stepProgressExp, expPerStep));
            }
        }

        return new MyGradeResponse(
                level != null ? level.getDisplayName() : null,
                gradeStep,
                stepUnit,
                maxStep,
                expPerStep,
                stepProgressExp,
                totalExp,
                user.getMannerScore(),
                user.getTradeCount()
        );
    }
}
