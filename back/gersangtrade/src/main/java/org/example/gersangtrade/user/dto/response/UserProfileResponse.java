package org.example.gersangtrade.user.dto.response;

import org.example.gersangtrade.domain.user.User;
import org.example.gersangtrade.domain.user.enums.GradeLevel;

import java.time.LocalDateTime;

/**
 * 사용자 프로필 응답 DTO.
 * 마이페이지 및 타 유저 프로필 조회에서 공통으로 사용한다.
 *
 * @param id           사용자 ID
 * @param nickname     닉네임
 * @param grade        현재 등급 (행상 / 보상 / 객상 / 대상 / 거상)
 * @param gradeStep    호봉 (거상은 null)
 * @param totalExp     누적 EXP
 * @param mannerScore  매너점수 (0~100)
 * @param tradeCount   거래 완료 횟수
 * @param createdAt    가입 일시
 */
public record UserProfileResponse(
        Long id,
        String nickname,
        GradeLevel grade,
        Integer gradeStep,
        Long totalExp,
        Integer mannerScore,
        Integer tradeCount,
        LocalDateTime createdAt
) {
    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getNickname(),
                user.getGrade(),
                user.getGradeStep(),
                user.getTotalExp(),
                user.getMannerScore(),
                user.getTradeCount(),
                user.getCreatedAt()
        );
    }
}
