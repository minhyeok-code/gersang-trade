package org.example.gersangtrade.user.dto.response;

import org.example.gersangtrade.domain.user.enums.GradeLevel;

/**
 * 등급 정책 응답 DTO — 프론트 툴팁 표시용.
 *
 * @param gradeNumber  등급 번호 (1이 최고)
 * @param name         등급 표시명 (행상·보상·객상·대상·거상)
 * @param stepUnit     호봉 단위 (거상은 null)
 * @param maxStep      최대 호봉 수 (거상은 0)
 * @param expPerStep   호봉당 필요 EXP (거상은 0)
 * @param baseExp      해당 등급 진입 누적 EXP
 */
public record GradePolicyResponse(
        int gradeNumber,
        String name,
        String stepUnit,
        int maxStep,
        int expPerStep,
        long baseExp
) {
    public static GradePolicyResponse from(GradeLevel grade) {
        return new GradePolicyResponse(
                grade.getGradeNumber(),
                grade.getDisplayName(),
                grade.getStepUnit(),
                grade.getMaxStep(),
                grade.getExpPerStep(),
                grade.getBaseExp()
        );
    }
}
