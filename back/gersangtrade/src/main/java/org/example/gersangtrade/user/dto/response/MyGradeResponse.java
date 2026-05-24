package org.example.gersangtrade.user.dto.response;

import org.example.gersangtrade.domain.user.User;

/**
 * 내 등급·경험치 응답 DTO — 마이페이지 등급 패널용.
 *
 * @param grade      현재 등급 표시명 (행상 / 보상 / 객상 / 대상 / 거상)
 * @param gradeStep  현재 호봉 (거상은 null)
 * @param totalExp   누적 EXP
 * @param mannerScore 매너점수 (0~100)
 * @param tradeCount 거래 완료 횟수
 */
public record MyGradeResponse(
        String grade,
        Integer gradeStep,
        long totalExp,
        int mannerScore,
        int tradeCount
) {
    public static MyGradeResponse from(User user) {
        return new MyGradeResponse(
                user.getGrade() != null ? user.getGrade().getDisplayName() : null,
                user.getGradeStep(),
                user.getTotalExp(),
                user.getMannerScore(),
                user.getTradeCount()
        );
    }
}
