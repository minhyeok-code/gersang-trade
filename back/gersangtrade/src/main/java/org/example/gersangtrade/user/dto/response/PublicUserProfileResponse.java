package org.example.gersangtrade.user.dto.response;

import org.example.gersangtrade.domain.user.User;
import org.example.gersangtrade.domain.user.enums.GradeLevel;

/**
 * 공개 사용자 프로필 응답 DTO.
 * 이메일·OAuth 정보 등 민감 필드를 제외한 공개 정보만 포함한다.
 *
 * @param id          사용자 ID
 * @param nickname    닉네임
 * @param grade       현재 등급
 * @param gradeStep   호봉 (거상은 null)
 * @param tradeCount  거래 완료 횟수
 * @param mannerScore 매너점수 (0~100)
 */
public record PublicUserProfileResponse(
        Long id,
        String nickname,
        GradeLevel grade,
        Integer gradeStep,
        Integer tradeCount,
        Integer mannerScore
) {
    public static PublicUserProfileResponse from(User user) {
        return new PublicUserProfileResponse(
                user.getId(),
                user.getNickname(),
                user.getGrade(),
                user.getGradeStep(),
                user.getTradeCount(),
                user.getMannerScore()
        );
    }
}
